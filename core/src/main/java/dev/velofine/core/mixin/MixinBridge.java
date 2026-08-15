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

package dev.velofine.core.mixin;

import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.MixinService;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

/**
 * Wires Mixin's own {@link IMixinTransformer} into our {@code Instrumentation}-based agent.
 * Shared across every engine (LegacySupport, Optimus, ...): {@code ServiceLoader}'s
 * {@code META-INF/services} registrations and {@code MixinBootstrap.init()}/{@code
 * Mixins.addConfiguration()} are global per-JVM, so there is exactly one transformer pipeline
 * supporting every engine's {@code mixins.<engine>.json} config, not one per engine.
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
     * after at least one {@code Mixins.addConfiguration(...)} call (so there's something to
     * transform). Safe to call once even when multiple engines each add their own config - the
     * transformer applies every registered config's mixins in one pass.
     */
    public static void install(Instrumentation instrumentation) {
        if (transformer != null) {
            return;
        }
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
                    System.err.println("[Velofine][core] Mixin transform failed for " + dottedName + ": " + t);
                    t.printStackTrace();
                    return null;
                }
            }
        }, true);
    }

    /**
     * Used by {@link VelofineMixinService#getClassNode} when asked to run transformers, and by
     * each engine's own diagnostic harness for direct bytes-in/bytes-out testing. Public (not
     * package-private) specifically so a harness class can live outside {@code
     * dev.velofine.core.mixin} - Mixin forbids classes inside an active mixin config's own package
     * from being loaded directly ("is in a defined mixin package ... and cannot be referenced
     * directly"), which would otherwise break a harness's own main-class loading.
     */
    public static byte[] transform(String name, String transformedName, byte[] bytes) {
        IMixinTransformer current = transformer;
        return current != null ? current.transformClassBytes(name, transformedName, bytes) : bytes;
    }
}
