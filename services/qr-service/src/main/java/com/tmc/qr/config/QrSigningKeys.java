package com.tmc.qr.config;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashSet;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class QrSigningKeys {

    private final JWKSet keySet;
    private final RSAKey activeKey;

    public QrSigningKeys(@Value("${tmc.qr.jwk-set-file}") Resource resource) throws IOException, ParseException {
        try (var input = resource.getInputStream()) {
            keySet = JWKSet.load(input);
        }
        RSAKey selectedKey = null;
        var ids = new HashSet<String>();
        for (var key : keySet.getKeys()) {
            if (!(key instanceof RSAKey rsaKey) || rsaKey.size() < 2048 || key.getKeyID() == null
                    || !ids.add(key.getKeyID()) || key.getKeyID().length() > 100
                    || (key.getKeyUse() != null && !KeyUse.SIGNATURE.equals(key.getKeyUse()))
                    || (key.getAlgorithm() != null && !JWSAlgorithm.RS256.equals(key.getAlgorithm()))) {
                throw new IllegalArgumentException("QR keys require RSA signing keys with unique IDs");
            }
            if (rsaKey.isPrivate()) {
                if (selectedKey != null) {
                    throw new IllegalArgumentException("QR requires exactly one active private key");
                }
                selectedKey = rsaKey;
            }
        }
        if (selectedKey == null) {
            throw new IllegalArgumentException("QR private signing key is missing");
        }
        activeKey = selectedKey;
    }

    public RSAKey activeKey() {
        return activeKey;
    }

    public RSAKey publicKey(String keyId) {
        var key = keySet.getKeyByKeyId(keyId);
        return key instanceof RSAKey rsaKey ? rsaKey.toPublicJWK() : null;
    }

    public String publicJwks() {
        return keySet.toPublicJWKSet().toString();
    }
}
