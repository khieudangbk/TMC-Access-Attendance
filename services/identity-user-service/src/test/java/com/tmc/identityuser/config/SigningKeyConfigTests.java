package com.tmc.identityuser.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SigningKeyConfigTests {

    @TempDir
    Path keyDirectory;

    @Test
    void publicOnlyKeySetCannotIssueTokens() throws Exception {
        RSAKey key = new RSAKeyGenerator(2048).keyID("public-only").generate();
        Path file = keyDirectory.resolve("public.json");
        Files.writeString(file, new JWKSet(key.toPublicJWK()).toString());
        assertThrows(IllegalArgumentException.class,
                () -> new SigningKeyConfig().signingKeys(new FileSystemResource(file)));
    }

    @Test
    void multiplePrivateKeysAreRejectedInsteadOfChoosingArbitrarily() throws Exception {
        RSAKey first = new RSAKeyGenerator(2048).keyID("first").generate();
        RSAKey second = new RSAKeyGenerator(2048).keyID("second").generate();
        Path file = keyDirectory.resolve("multiple.json");
        Files.writeString(file, new JWKSet(List.of(first, second)).toString(false));
        assertThrows(IllegalArgumentException.class,
                () -> new SigningKeyConfig().signingKeys(new FileSystemResource(file)));
    }

    @Test
    void configuredPrivateKeyIsRetainedAcrossLoadsWithoutLeakingItsValue() throws Exception {
        RSAKey key = new RSAKeyGenerator(2048).keyID("persistent").generate();
        Path file = keyDirectory.resolve("persistent.json");
        Files.writeString(file, new JWKSet(key).toString(false));
        var first = new SigningKeyConfig().signingKeys(new FileSystemResource(file));
        var second = new SigningKeyConfig().signingKeys(new FileSystemResource(file));
        assertEquals(first.activeKeyId(), second.activeKeyId());
        assertEquals(first.keySet().getKeyByKeyId("persistent"), second.keySet().getKeyByKeyId("persistent"));
        assertFalse(first.toString().contains(key.getPrivateExponent().toString()));
    }
}
