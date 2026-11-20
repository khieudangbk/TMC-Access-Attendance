package com.tmc.qr.service.impl;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.tmc.qr.client.EmployeeAccess;
import com.tmc.qr.config.QrSigningKeys;
import com.tmc.qr.dto.response.IssuedQrDTO;
import com.tmc.qr.dto.response.QrVerificationDTO;
import com.tmc.qr.repository.QrStore;
import com.tmc.qr.service.QrService;

@Service("qrService")
public class QrServiceImpl implements QrService {

    private static final String AUDIENCE = "tmc-gate-qr";
    private static final String PURPOSE = "factory-access";
    private final QrStore qrStore;
    private final QrSigningKeys signingKeys;
    private final EmployeeAccess employeeAccess;
    private final Clock clock;
    private final Duration ttl;
    private final String issuer;

    public QrServiceImpl(QrStore qrStore, QrSigningKeys signingKeys, EmployeeAccess employeeAccess, Clock clock,
            @Value("${tmc.qr.ttl:60s}") Duration ttl, @Value("${tmc.qr.issuer:tmc-qr}") String issuer) {
        if (ttl.compareTo(Duration.ofSeconds(30)) < 0 || ttl.compareTo(Duration.ofSeconds(60)) > 0) {
            throw new IllegalArgumentException("QR TTL must be between 30 and 60 seconds");
        }
        this.qrStore = qrStore;
        this.signingKeys = signingKeys;
        this.employeeAccess = employeeAccess;
        this.clock = clock;
        this.ttl = ttl;
        this.issuer = issuer;
    }

    @Transactional
    @Override
    public IssuedQrDTO issue(UUID employeeId, UUID factoryId) {
        if (!employeeAccess.allowed(employeeId, factoryId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "SITE_ACCESS_DENIED");
        }
        qrStore.lockEmployee(employeeId);
        Instant issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plus(ttl);
        UUID qrId = UUID.randomUUID();
        var key = signingKeys.activeKey();
        var claims = new JWTClaimsSet.Builder().issuer(issuer).subject(employeeId.toString())
                .jwtID(qrId.toString()).audience(AUDIENCE).issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt)).claim("factory_id", factoryId.toString())
                .claim("purpose", PURPOSE).claim("version", 1).build();
        var token = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID())
                .type(new JOSEObjectType("tmc-qr+jwt")).build(), claims);
        try {
            token.sign(new RSASSASigner(key));
        } catch (JOSEException exception) {
            throw new IllegalStateException("QR signing failed", exception);
        }
        qrStore.revoke(employeeId, factoryId, issuedAt);
        qrStore.save(qrId, employeeId, factoryId, issuedAt, expiresAt, key.getKeyID());
        return new IssuedQrDTO(qrId, token.serialize(), expiresAt);
    }

    @Transactional
    @Override
    public void revoke(UUID employeeId, UUID factoryId) {
        qrStore.lockEmployee(employeeId);
        qrStore.revoke(employeeId, factoryId, clock.instant());
        qrStore.audit(employeeId, null, factoryId, "QR_REVOKED");
    }

    @Transactional(readOnly = true)
    @Override
    public QrVerificationDTO verify(String encodedToken, UUID expectedFactoryId) {
        try {
            var token = SignedJWT.parse(encodedToken);
            if (!JWSAlgorithm.RS256.equals(token.getHeader().getAlgorithm())
                    || !new JOSEObjectType("tmc-qr+jwt").equals(token.getHeader().getType())) {
                return QrVerificationDTO.denied("INVALID_QR_PURPOSE");
            }
            var key = signingKeys.publicKey(token.getHeader().getKeyID());
            if (key == null || !token.verify(new RSASSAVerifier(key))) {
                return QrVerificationDTO.denied("INVALID_QR_SIGNATURE");
            }
            var claims = token.getJWTClaimsSet();
            if (!issuer.equals(claims.getIssuer()) || !List.of(AUDIENCE).equals(claims.getAudience())
                    || !PURPOSE.equals(claims.getStringClaim("purpose"))
                    || !Integer.valueOf(1).equals(claims.getIntegerClaim("version"))) {
                return QrVerificationDTO.denied("INVALID_QR_PURPOSE");
            }
            Instant now = clock.instant();
            Instant issuedAt = claims.getIssueTime().toInstant();
            Instant expiresAt = claims.getExpirationTime().toInstant();
            if (!expiresAt.isAfter(now) || issuedAt.isAfter(now.plusSeconds(5)) || !expiresAt.isAfter(issuedAt)
                    || Duration.between(issuedAt, expiresAt).compareTo(Duration.ofSeconds(60)) > 0) {
                return QrVerificationDTO.denied("QR_EXPIRED");
            }
            UUID employeeId = UUID.fromString(claims.getSubject());
            UUID qrId = UUID.fromString(claims.getJWTID());
            UUID factoryId = UUID.fromString(claims.getStringClaim("factory_id"));
            if (!factoryId.equals(expectedFactoryId)) {
                return QrVerificationDTO.denied("QR_FACTORY_MISMATCH");
            }
            if (!qrStore.matches(qrId, employeeId, factoryId, issuedAt, expiresAt, key.getKeyID(), now)) {
                return QrVerificationDTO.denied("QR_REVOKED_OR_UNKNOWN");
            }
            return new QrVerificationDTO(true, "VALID", qrId, employeeId, factoryId, expiresAt);
        } catch (java.text.ParseException | JOSEException | IllegalArgumentException | NullPointerException exception) {
            return QrVerificationDTO.denied("INVALID_QR");
        }
    }
}
