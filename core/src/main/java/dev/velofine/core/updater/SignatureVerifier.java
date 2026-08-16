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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Verifies the release pipeline's Ed25519 signature over {@code SHA256SUMS.txt} - the layer that
 * distinguishes "the bytes match what some server sent" ({@link ChecksumVerifier}) from "a release
 * genuinely produced by Velofine's own signing key produced these bytes". Matches current practice
 * for unsigned-app updaters (Tauri's updater, Sparkle/minisign): a public key baked into the app,
 * a private key that only ever touches the release CI runner.
 *
 * <p>{@code java.security.Signature.getInstance("Ed25519")} has been JDK-builtin since Java 15 -
 * no new dependency for either half of this (signing happens in a standalone script outside the
 * module graph entirely, see {@code tools/release-signer/Signer.java}, so private-key-handling
 * code is never bundled inside anything Velofine ships).
 *
 * <p>The embedded key below is the real public half of Velofine's release-signing keypair,
 * generated once for this project; the matching private key lives only as the
 * {@code VELOFINE_SIGNING_KEY} GitHub Actions secret used by {@code .github/workflows/release.yml}.
 */
final class SignatureVerifier {

    private static final String PUBLIC_KEY_BASE64 =
            "MCowBQYDK2VwAyEA2CjKZl8XqX1syn1cqfVoOT+xRDIDh8Pc1c/6HIwGQuo=";

    boolean verify(Path checksumsFile, Path signatureFile) throws IOException, GeneralSecurityException {
        byte[] message = Files.readAllBytes(checksumsFile);
        byte[] signatureBytes = Base64.getDecoder().decode(Files.readString(signatureFile).trim());

        PublicKey publicKey = loadPublicKey();
        Signature signature = Signature.getInstance("Ed25519");
        signature.initVerify(publicKey);
        signature.update(message);
        return signature.verify(signatureBytes);
    }

    private static PublicKey loadPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] encoded = Base64.getDecoder().decode(PUBLIC_KEY_BASE64);
        KeyFactory factory = KeyFactory.getInstance("Ed25519");
        return factory.generatePublic(new X509EncodedKeySpec(encoded));
    }
}
