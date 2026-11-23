package com.tmc.identityuser.config;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashSet;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class SigningKeyConfig {

    @Bean
    SigningKeys signingKeys(@Value("${tmc.identity.jwk-set-file}") Resource resource) throws IOException, ParseException {
        JWKSet keySet;
        try (var input = resource.getInputStream()) {
            keySet = JWKSet.load(input);
        }
        var keyIds = new HashSet<String>();
        RSAKey signingKey = null;
        for (JWK key : keySet.getKeys()) {
            if (!(key instanceof RSAKey rsaKey) || key.getKeyID() == null || !keyIds.add(key.getKeyID())
                    || rsaKey.size() < 2048
                    || (key.getKeyUse() != null && !KeyUse.SIGNATURE.equals(key.getKeyUse()))
                    || (key.getAlgorithm() != null && !JWSAlgorithm.RS256.equals(key.getAlgorithm()))) {
                throw new IllegalArgumentException("Signing keys must be RSA >=2048 bits with unique key IDs");
            }
            if (key.isPrivate()) {
                if (signingKey != null) {
                    throw new IllegalArgumentException("Exactly one active private signing key is required");
                }
                signingKey = rsaKey;
            }
        }
        if (signingKey == null) {
            throw new IllegalArgumentException("An active private signing key is required");
        }
        return new SigningKeys(keySet, signingKey.getKeyID());
    }

    @Bean
    JWKSource<SecurityContext> jwkSource(SigningKeys signingKeys) {
        return (selector, context) -> selector.select(signingKeys.keySet());
    }

    public record SigningKeys(JWKSet keySet, String activeKeyId) {
        @Override
        public String toString() {
            return "SigningKeys[activeKeyId=" + activeKeyId + "]";
        }
    }
}
