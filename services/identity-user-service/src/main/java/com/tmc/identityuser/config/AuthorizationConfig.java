package com.tmc.identityuser.config;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.tmc.identityuser.repository.IdentityStore;
import com.tmc.identityuser.service.IdentityService;

@Configuration
public class AuthorizationConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    AuthorizationServerSettings authorizationServerSettings(
            @Value("${spring.security.oauth2.authorizationserver.issuer:http://localhost:8081}") String issuer) {
        return AuthorizationServerSettings.builder().issuer(issuer).build();
    }

    @Bean
    OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate, RegisteredClientRepository clients) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, clients);
    }

    @Bean
    OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbcTemplate, RegisteredClientRepository clients) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, clients);
    }

    @Bean
    JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource, AuthorizationServerSettings settings) {
        var decoder = (NimbusJwtDecoder) OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(settings.getIssuer()));
        return decoder;
    }

    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer(IdentityStore identityStore,
            SigningKeyConfig.SigningKeys signingKeys) {
        return context -> {
            context.getJwsHeader().keyId(signingKeys.activeKeyId());
            context.getClaims().audience(List.of("tmc-api"));
            if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())) {
                identityStore.device(context.getRegisteredClient().getClientId()).ifPresent(device -> {
                    if (!device.active()) {
                        throw new OAuth2AuthenticationException("invalid_client");
                    }
                    context.getClaims().claim("station_id", device.stationId().toString());
                });
            } else {
                var employee = identityStore.employee(UUID.fromString(context.getPrincipal().getName()));
                if (!employee.active()) {
                    throw new OAuth2AuthenticationException("access_denied");
                }
                context.getClaims().subject(employee.employeeId().toString())
                        .claim("employee_id", employee.employeeId().toString())
                        .claim("roles", employee.roles().stream().map(Enum::name).sorted().toList())
                        .claim("factory_ids", employee.factoryIds().stream().map(UUID::toString).sorted().toList());
            }
        };
    }

    @Bean
    CommandLineRunner bootstrap(IdentityService service, RegisteredClientRepository clients,
            PasswordEncoder passwordEncoder, PlatformTransactionManager transactionManager, IdentityStore identityStore,
            @Value("${tmc.identity.bootstrap-username:}") String username,
            @Value("${tmc.identity.bootstrap-password:}") String password,
            @Value("${tmc.identity.technical-client-id:tmc-local-client}") String technicalClientId,
            @Value("${tmc.identity.technical-client-secret:}") String technicalClientSecret,
            @Value("${tmc.identity.web-redirect-uri:http://localhost:3000/callback}") String redirectUri) {
        return arguments -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            identityStore.lockAdministration();
            service.bootstrap(username, password);
            // Serialize initial provisioning across instances; never rotate existing secrets implicitly.
            if (!technicalClientSecret.isBlank() && clients.findById("tmc-service-bootstrap") == null) {
                clients.save(RegisteredClient.withId("tmc-service-bootstrap").clientId(technicalClientId)
                        .clientSecret(passwordEncoder.encode(technicalClientSecret))
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                        .scope("identity.verify").scope("qr.verify").scope("stations.verify")
                        .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(Duration.ofMinutes(2)).build()).build());
            }
            if (clients.findById("tmc-web") == null) {
                clients.save(RegisteredClient.withId("tmc-web").clientId("tmc-web")
                        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                        .redirectUri(redirectUri).scope("qr.issue").scope("access.review")
                        .clientSettings(ClientSettings.builder().requireProofKey(true).build())
                        .tokenSettings(TokenSettings.builder().reuseRefreshTokens(false)
                                .accessTokenTimeToLive(Duration.ofMinutes(5)).build()).build());
            }
        });
    }
}
