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

package dev.velofine.core.shader;

import com.mojang.blaze3d.shaders.ShaderType;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The single dispatch point for every real GLSL source lookup ({@code
 * com.mojang.blaze3d.opengl.GlDevice.compileShader}'s call to {@code ShaderSource.get(Identifier,
 * ShaderType)}), backing {@code core}'s own {@code GlDeviceMixin}.
 *
 * <p>Why this exists rather than each engine {@code @Redirect}ing that call site directly: Mixin's
 * {@code @Redirect} claims exclusive ownership of a single call site - two independent mixins both
 * redirecting the identical instruction conflict at apply time. Phase 2's {@code GlDeviceMixin}
 * originally lived in {@code legacysupport} and had sole ownership of this redirect for the Intel
 * {@code mix()} patch; Phase 7 needs the same seam for shader-pack GLSL substitution, and
 * critically needs it to fire on hardware where {@code LegacySupportEngine} installs <em>no</em>
 * mixins at all (its {@code onAgentAttached} returns early whenever no {@link
 * dev.velofine.core.hardware.Fix} is active - true on essentially all non-reference hardware, i.e.
 * exactly the hardware shader packs actually target). So the one real {@code @Redirect} now lives
 * in {@code core} (always installed, no engine gate - see {@code CoreEngine}'s own javadoc for the
 * precedent), and both {@code legacysupport} and {@code shaders} register a {@link
 * ShaderSourceInterceptor} into this registry instead of touching the call site themselves.
 *
 * <p>Interceptors are tried in ascending {@code priority} order; the first to return a non-empty
 * {@link Optional} wins and the rest are skipped. Shaders' gbuffers substitution registers at a
 * lower number (higher priority) than LegacySupport's {@code mix()} patch, since a shader-pack
 * program fully replaces vanilla's GLSL for that draw call - patching the vanilla source LegacySupport
 * would otherwise target is meaningless once a pack owns that identifier.
 *
 * <h2>Two stages (v1.8-Beta)</h2>
 *
 * <p>{@code GlDevice.compileShader} obtains GLSL in two steps, confirmed by javap against the real
 * 26.2 jar: {@code ShaderSource.get(...)} at bytecode offset 9, then
 * {@code GlslPreprocessor.injectDefines(source, defines)} at offset 47. So there are two meaningfully
 * different texts, and this registry exposes both:
 *
 * <ul>
 *   <li><b>Stage 1</b> ({@link #register}/{@link #resolve}) - the raw asset text, before any
 *       {@code #define} injection. Right for whole-source substitution (shader packs) and for
 *       transforms that don't depend on preprocessor state ({@code ShaderPatcher}'s {@code mix()}
 *       patch).</li>
 *   <li><b>Stage 2</b> ({@link #registerPostDefines}/{@link #resolvePostDefines}) - the text
 *       <em>after</em> defines are injected, i.e. exactly what the GL compiler is handed. Required
 *       for anything that must read a {@code #define}d value. v1.7-Beta's end-portal loop-unroll
 *       shipped at stage 1 and consequently never fired even once on real hardware: its bound
 *       {@code PORTAL_LAYERS} lives on {@code RenderPipelines.END_PORTAL}'s {@code ShaderDefines},
 *       not in the {@code .fsh}, so it simply wasn't in the text yet.</li>
 * </ul>
 *
 * <p>Both stages share one class because stage 2's redirect has no {@code Identifier}/
 * {@code ShaderType} in scope at that instruction - only the source string. The identity is carried
 * across by a {@link ThreadLocal} that stage 1 records and stage 2 reads-and-clears. That is sound
 * because the two calls are straight-line within a single {@code compileShader} invocation (with an
 * early return between them only when {@code get} returned {@code null}, in which case
 * {@code injectDefines} never runs), and because stage 1's redirect is installed unconditionally by
 * {@code CoreEngine} rather than being gated behind any engine.
 */
public final class ShaderSourceInterceptors {

    public static final int PRIORITY_SHADER_PACK = 0;
    public static final int PRIORITY_LEGACY_SUPPORT = 100;

    private static final List<Entry> interceptors = new CopyOnWriteArrayList<>();
    private static final List<Entry> postDefineInterceptors = new CopyOnWriteArrayList<>();

    private static final ThreadLocal<ShaderIdentity> currentShader = new ThreadLocal<>();

    private ShaderSourceInterceptors() {
    }

    public static void register(int priority, ShaderSourceInterceptor interceptor) {
        interceptors.add(new Entry(priority, interceptor));
        interceptors.sort((a, b) -> Integer.compare(a.priority, b.priority));
    }

    /** Registers a stage-2 interceptor - see the class javadoc for when that is the right stage. */
    public static void registerPostDefines(int priority, ShaderSourceInterceptor interceptor) {
        postDefineInterceptors.add(new Entry(priority, interceptor));
        postDefineInterceptors.sort((a, b) -> Integer.compare(a.priority, b.priority));
    }

    public static String resolve(Identifier id, ShaderType type, String vanillaSource) {
        // Recorded even when no stage-1 interceptor is registered: stage 2 needs the identity, and
        // the two stages are installed independently of each other.
        currentShader.set(new ShaderIdentity(id, type));

        for (Entry entry : interceptors) {
            Optional<String> override = entry.interceptor.intercept(id, type, vanillaSource);
            if (override.isPresent()) {
                return override.get();
            }
        }
        return vanillaSource;
    }

    /**
     * Stage 2. Takes only the post-{@code injectDefines} source; the shader's identity comes from
     * what {@link #resolve} recorded earlier in the same {@code compileShader} call.
     */
    public static String resolvePostDefines(String injectedSource) {
        ShaderIdentity identity = currentShader.get();
        // Cleared before dispatching, so a throwing interceptor can't strand a stale entry on a
        // long-lived render thread (remove() rather than set(null) also drops the Identifier ref).
        currentShader.remove();

        if (postDefineInterceptors.isEmpty() || identity == null) {
            // The common path for every user: stage 2's redirect is always installed (it lives in
            // core), while interceptors register only on matched legacy hardware.
            return injectedSource;
        }

        String resolved = injectedSource;
        for (Entry entry : postDefineInterceptors) {
            Optional<String> override = entry.interceptor.intercept(identity.id(), identity.type(), injectedSource);
            if (override.isPresent()) {
                resolved = override.get();
                break;
            }
        }

        // Opt-in diagnostic export (-Dvelofine.shader.dumpPatched=<dir>), so a tester can compile-check
        // the exact text the driver will receive via diagnostic.exe's CANDIDATE mode. Dumped here
        // because this is the final form - defines injected and every patch applied.
        if (ShaderSourceDump.isEnabled()) {
            ShaderSourceDump.dump(identity.id(), identity.type(), "postdefines", resolved);
        }
        return resolved;
    }

    private record Entry(int priority, ShaderSourceInterceptor interceptor) {
    }

    private record ShaderIdentity(Identifier id, ShaderType type) {
    }
}
