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

package dev.velofine.legacysupport.mixin;

import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.MixinService;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

/**
 * Wires Mixin's own {@link IMixinTransformer} into our {@code Instrumentation}-based agent.
 *
 * <p>{@code MixinBootstrap.init()} constructs an {@code IMixinTransformerFactory} and offers it to
 * whatever {@code IMixinService} is active (ours: {@link VelofineMixinService}) via {@code
 * IMixinService#offer}, which {@code MixinServiceAbstract}'s default implementation stores in an
 * internals registry keyed by interface type. We retrieve it from there, create the actual
 * transformer, and register a {@link ClassFileTransformer} that delegates to {@code
 * IMixinTransformer#transformClassBytes} - documented by Mixin itself as "Callback from the
 * hotswap agent and LaunchWrapper Proxy", i.e. built for exactly this kind of bare-agent use.
 */
public final class MixinBridge {

    private static volatile IMixinTransformer transformer;

    private MixinBridge() {
    }

    /**
     * Call after {@code MixinBootstrap.init()} (so the transformer factory has been offered) and
     * after {@code Mixins.addConfiguration(...)} (so there's something to transform).
     */
    public static void install(Instrumentation instrumentation) {
        VelofineMixinService service = (VelofineMixinService) MixinService.getService();
        IMixinTransformerFactory factory = service.internal(IMixinTransformerFactory.class);
        if (factory == null) {
            throw new IllegalStateException(
                    "Mixin did not offer an IMixinTransformerFactory to VelofineMixinService - bootstrap sequence changed?");
        }

        transformer = factory.createTransformer();

        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String internalClassName, Class<?> classBeingRedefined,
                    ProtectionDomain protectionDomain, byte[] classfileBuffer) {
                if (internalClassName == null) {
                    return null;
                }
                String dottedName = internalClassName.replace('/', '.');
                try {
                    byte[] result = transformer.transformClassBytes(dottedName, dottedName, classfileBuffer);
                    return result == classfileBuffer ? null : result;
                } catch (Throwable t) {
                    System.err.println("[Velofine][LegacySupport] Mixin transform failed for " + dottedName + ": " + t);
                    t.printStackTrace();
                    return null;
                }
            }
        }, true);
    }

    /**
     * Used by {@link VelofineMixinService#getClassNode} when asked to run transformers, and by
     * {@code VerifyMixinsHarness} for direct bytes-in/bytes-out diagnostic testing. Public (not
     * package-private) specifically so the harness can live outside {@code
     * dev.velofine.legacysupport.mixin} - Mixin forbids classes inside an active mixin config's
     * own package from being loaded directly ("is in a defined mixin package ... and cannot be
     * referenced directly"), which would otherwise break the harness's own main-class loading.
     */
    public static byte[] transform(String name, String transformedName, byte[] bytes) {
        IMixinTransformer current = transformer;
        return current != null ? current.transformClassBytes(name, transformedName, bytes) : bytes;
    }
}
