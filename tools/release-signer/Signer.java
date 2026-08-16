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

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Signs a release's {@code SHA256SUMS.txt} with Velofine's Ed25519 release-signing private key,
 * verified at runtime by {@code core.updater.SignatureVerifier} against the matching public key
 * embedded there.
 *
 * <p>Deliberately a standalone single-file program (run via {@code java Signer.java ...}, Java's
 * file-source-launch, no compile step) living entirely outside the Gradle module graph - not a
 * {@code core}/{@code installer} source set. Private-key-handling code must never end up inside
 * anything Velofine ships to end users; keeping it here, invoked only by
 * {@code .github/workflows/release.yml} with the key piped in from a GitHub Actions secret, makes
 * that structurally true rather than a convention someone could accidentally violate later.
 *
 * <p>Usage: {@code java Signer.java <fileToSign> <outputSignaturePath>}, with the base64-encoded
 * PKCS#8 private key in the {@code VELOFINE_SIGNING_KEY} environment variable. Never write that
 * key to disk or echo it - the CI workflow passes it as a masked secret, and this program only
 * ever holds it in memory.
 */
public final class Signer {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: java Signer.java <fileToSign> <outputSignaturePath>");
            System.exit(2);
        }

        String keyBase64 = System.getenv("VELOFINE_SIGNING_KEY");
        if (keyBase64 == null || keyBase64.isBlank()) {
            System.err.println("VELOFINE_SIGNING_KEY environment variable is not set.");
            System.exit(2);
        }

        Path fileToSign = Path.of(args[0]);
        Path outputSignaturePath = Path.of(args[1]);

        PrivateKey privateKey = loadPrivateKey(keyBase64);
        byte[] message = Files.readAllBytes(fileToSign);

        Signature signature = Signature.getInstance("Ed25519");
        signature.initSign(privateKey);
        signature.update(message);
        byte[] signatureBytes = signature.sign();

        Files.writeString(outputSignaturePath, Base64.getEncoder().encodeToString(signatureBytes));
        System.out.println("Signed " + fileToSign + " -> " + outputSignaturePath);
    }

    private static PrivateKey loadPrivateKey(String base64) throws Exception {
        byte[] encoded = Base64.getDecoder().decode(base64);
        KeyFactory factory = KeyFactory.getInstance("Ed25519");
        return factory.generatePrivate(new PKCS8EncodedKeySpec(encoded));
    }
}
