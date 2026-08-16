/*
 * This file is part of Velofine.
 *
 * Velofine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Velofine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Velofine. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2026 siliconcode-dev
 */

package dev.velofine.core.updater;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises real Ed25519 verify logic against a throwaway keypair generated fresh in this test -
 * the same technique used to generate and hand-verify Velofine's actual release-signing keypair
 * during Phase 8. Never touches the real embedded production key or its matching private half.
 */
final class SignatureVerifierTest {

    private final SignatureVerifier verifier = new SignatureVerifier();
    private String publicKeyBase64;
    private PrivateKey privateKey;

    @BeforeEach
    void generateThrowawayKeypair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
        KeyPair pair = generator.generateKeyPair();
        privateKey = pair.getPrivate();
        publicKeyBase64 = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
    }

    @Test
    void aGenuineSignatureVerifies(@TempDir Path dir) throws Exception {
        Path checksums = dir.resolve("SHA256SUMS.txt");
        Files.writeString(checksums, "deadbeef  Velofine-Setup-1.0.0-Beta.exe\n", StandardCharsets.UTF_8);
        Path signatureFile = dir.resolve("SHA256SUMS.txt.sig");
        signWith(privateKey, checksums, signatureFile);

        assertTrue(verifier.verify(checksums, signatureFile, publicKeyBase64));
    }

    @Test
    void aSignatureFromTheWrongKeyFailsVerification(@TempDir Path dir) throws Exception {
        Path checksums = dir.resolve("SHA256SUMS.txt");
        Files.writeString(checksums, "deadbeef  Velofine-Setup-1.0.0-Beta.exe\n", StandardCharsets.UTF_8);
        Path signatureFile = dir.resolve("SHA256SUMS.txt.sig");

        KeyPair otherPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        signWith(otherPair.getPrivate(), checksums, signatureFile);

        assertFalse(verifier.verify(checksums, signatureFile, publicKeyBase64));
    }

    @Test
    void tamperedChecksumsFileFailsVerificationEvenWithAGenuineSignature(@TempDir Path dir) throws Exception {
        Path checksums = dir.resolve("SHA256SUMS.txt");
        Files.writeString(checksums, "deadbeef  Velofine-Setup-1.0.0-Beta.exe\n", StandardCharsets.UTF_8);
        Path signatureFile = dir.resolve("SHA256SUMS.txt.sig");
        signWith(privateKey, checksums, signatureFile);

        // A release-signed checksums file being swapped out after the fact is exactly the attack
        // this layer exists to catch - the whole reason ChecksumVerifier alone is not enough.
        Files.writeString(checksums, "cafebabe  Velofine-Setup-1.0.0-Beta.exe\n", StandardCharsets.UTF_8);

        assertFalse(verifier.verify(checksums, signatureFile, publicKeyBase64));
    }

    @Test
    void theRealEmbeddedProductionKeyIsWellFormed() throws Exception {
        // Does not (and cannot) verify a real signature without the private half, which only
        // exists as a GitHub Actions secret - just confirms the embedded constant this class ships
        // with actually decodes as a valid Ed25519 public key, so a typo there fails loudly here
        // instead of silently at release-verification time.
        Path dir = Files.createTempDirectory("velofine-sigverify-prodkey-test");
        try {
            Path checksums = Files.writeString(dir.resolve("SHA256SUMS.txt"), "x", StandardCharsets.UTF_8);
            Path signatureFile = Files.writeString(dir.resolve("SHA256SUMS.txt.sig"),
                    Base64.getEncoder().encodeToString(new byte[64]), StandardCharsets.UTF_8);

            // Real embedded key, deliberately wrong (all-zero) signature - must return false, not throw.
            assertFalse(verifier.verify(checksums, signatureFile));
        } finally {
            Files.deleteIfExists(dir.resolve("SHA256SUMS.txt"));
            Files.deleteIfExists(dir.resolve("SHA256SUMS.txt.sig"));
            Files.deleteIfExists(dir);
        }
    }

    private static void signWith(PrivateKey key, Path message, Path outputSignatureFile) throws Exception {
        Signature signature = Signature.getInstance("Ed25519");
        signature.initSign(key);
        signature.update(Files.readAllBytes(message));
        Files.writeString(outputSignatureFile, Base64.getEncoder().encodeToString(signature.sign()), StandardCharsets.UTF_8);
    }
}
