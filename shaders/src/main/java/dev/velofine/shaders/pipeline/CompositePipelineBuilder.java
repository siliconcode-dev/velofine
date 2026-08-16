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

package dev.velofine.shaders.pipeline;

import com.mojang.blaze3d.shaders.ShaderType;
import dev.velofine.core.log.VelofineLog;
import dev.velofine.shaders.format.ShaderPack;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostChainConfig;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds a real {@link PostChainConfig} (confirmed via javap: a JSON-loadable, {@code Codec}-backed,
 * {@code FrameGraphBuilder}-driven multi-pass system - see CLAUDE.md's Phase 7 research notes on
 * why this is preferred over hand-rolling framebuffer chaining) from a shader pack's {@code
 * deferred}/{@code composite}/{@code final} program files, matching OptiFine's real stage order:
 * {@code deferred}, {@code deferred1}-{@code deferred15}, {@code composite},
 * {@code composite1}-{@code composite15}, {@code final}.
 *
 * <p><b>Real constraints, deliberately scoped rather than silently wrong</b>:
 * <ul>
 *   <li>{@code PostChainConfig.Pass.outputTarget()} is a single {@link Identifier}, not a list, so
 *   a pass whose {@code DRAWBUFFERS}/{@code RENDERTARGETS} directive names more than one colortex
 *   buffer only gets its <em>first</em> declared output honored - a warning is logged rather than
 *   attempting synthetic multi-pass splitting (which would re-run the shader once per declared
 *   output, wasteful and only valid for side-effect-free shaders). Real popular packs commonly use
 *   multi-target composite passes; this is a genuine v1 limitation to revisit.</li>
 *   <li>Only {@code colortexN}-named samplers are recognized as buffer inputs - OptiFine's legacy
 *   aliases ({@code gcolor}/{@code gdepth}/{@code gnormal}/{@code gaux1-4}, see {@code
 *   PackRenderTargetDirectives.LEGACY_RENDER_TARGETS} in the real Iris source) are not resolved.</li>
 *   <li>Every pass needs an explicit {@code .vsh} <em>and</em> {@code .fsh} - OptiFine's implicit
 *   default full-screen-quad vertex shader (used when a composite pass ships only a {@code .fsh})
 *   has no Velofine-side equivalent to substitute, since that would mean reproducing Mojang's own
 *   builtin GLSL, which {@code mcstubs}' own rules forbid vendoring.</li>
 *   <li>{@code shadowtex}/{@code shadowcolor} inputs (needed once the shadow pass, 7.6, exists) and
 *   custom images/textures are not recognized yet.</li>
 * </ul>
 */
public final class CompositePipelineBuilder {

    private static final Pattern COLORTEX_SAMPLER =
            Pattern.compile("\\buniform\\s+sampler2D\\s+colortex(\\d+)\\s*;");

    private CompositePipelineBuilder() {
    }

    /** @return the built config, or {@code null} if the pack declares no composite/deferred/final programs at all. */
    public static PostChainConfig build(ShaderPack pack) {
        CompositeProgramRegistry.clear();

        List<PostChainConfig.Pass> passes = new ArrayList<>();
        Set<Integer> referencedColortex = new LinkedHashSet<>();
        boolean sawExplicitFinal = false;

        for (String programName : stageOrder()) {
            String fragmentSource = ShaderProgramLoader.loadDirect(pack, programName, ShaderType.FRAGMENT);
            String vertexSource = ShaderProgramLoader.loadDirect(pack, programName, ShaderType.VERTEX);
            if (fragmentSource == null || vertexSource == null) {
                continue;
            }

            boolean isFinalPass = "final".equals(programName);
            sawExplicitFinal |= isFinalPass;
            passes.add(buildPass(programName, vertexSource, fragmentSource, referencedColortex, isFinalPass));
        }

        if (passes.isEmpty()) {
            return null;
        }

        // OptiFine's "final" program is special: its output IS the displayed image, not another
        // intermediate buffer. If the pack has no "final" program at all, the last composite/
        // deferred pass found still needs to end up on screen somehow - OptiFine's own real
        // behavior here is to implicitly treat the last composite/deferred program as final when
        // no explicit final.fsh exists, so this does the same rather than silently rendering
        // nothing to the display.
        if (!sawExplicitFinal) {
            PostChainConfig.Pass last = passes.get(passes.size() - 1);
            passes.set(passes.size() - 1, new PostChainConfig.Pass(
                    last.vertexShaderId(), last.fragmentShaderId(), last.inputs(), PostChain.MAIN_TARGET_ID, last.uniforms()));
        }

        Map<Identifier, PostChainConfig.InternalTarget> internalTargets = new LinkedHashMap<>();
        for (int index : referencedColortex) {
            internalTargets.put(colortexId(index), new PostChainConfig.InternalTarget(Optional.empty(), Optional.empty(), false, 0));
        }

        return new PostChainConfig(internalTargets, passes);
    }

    private static PostChainConfig.Pass buildPass(String programName, String vertexSource,
            String fragmentSource, Set<Integer> referencedColortex, boolean isFinalPass) {
        List<Integer> declaredOutputs = DrawBuffersDirective.parse(fragmentSource);
        if (declaredOutputs.size() > 1) {
            VelofineLog.warn("Shaders", programName + " declares " + declaredOutputs.size()
                    + " output buffers (DRAWBUFFERS/RENDERTARGETS) - only the first (colortex"
                    + declaredOutputs.get(0) + ") is honored, see CompositePipelineBuilder's class javadoc.");
        }
        int outputIndex = declaredOutputs.get(0);
        if (!isFinalPass) {
            referencedColortex.add(outputIndex);
        }

        List<PostChainConfig.Input> inputs = new ArrayList<>();
        Matcher matcher = COLORTEX_SAMPLER.matcher(fragmentSource);
        while (matcher.find()) {
            int inputIndex = Integer.parseInt(matcher.group(1));
            referencedColortex.add(inputIndex);
            inputs.add(new PostChainConfig.TargetInput("colortex" + inputIndex, colortexId(inputIndex), false, false));
        }

        Identifier vertexId = CompositeProgramRegistry.register(programName, ".vsh", vertexSource);
        Identifier fragmentId = CompositeProgramRegistry.register(programName, ".fsh", fragmentSource);
        Identifier outputTarget = isFinalPass ? PostChain.MAIN_TARGET_ID : colortexId(outputIndex);

        return new PostChainConfig.Pass(vertexId, fragmentId, inputs, outputTarget, Map.of());
    }

    private static Identifier colortexId(int index) {
        return Identifier.fromNamespaceAndPath("velofine", "colortex" + index);
    }

    private static List<String> stageOrder() {
        List<String> order = new ArrayList<>();
        order.add("deferred");
        for (int i = 1; i <= 15; i++) {
            order.add("deferred" + i);
        }
        order.add("composite");
        for (int i = 1; i <= 15; i++) {
            order.add("composite" + i);
        }
        order.add("final");
        return order;
    }
}
