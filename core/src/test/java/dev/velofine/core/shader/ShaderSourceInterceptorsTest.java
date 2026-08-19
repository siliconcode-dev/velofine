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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the two-stage dispatch added in v1.8-Beta - in particular that stage 2 receives the shader
 * identity recorded by stage 1, which is the mechanism that lets a post-{@code #define} interceptor
 * know <em>which</em> shader it is looking at (the redirected {@code injectDefines} call has no
 * {@code Identifier} in scope).
 *
 * <p>{@code Identifier} is passed as {@code null} throughout: these are {@code mcstubs} types that
 * exist only at compile time, and the registry never dereferences them - it only carries them across
 * the two stages. The assertions that matter here are about <em>dispatch and context</em>, and using
 * real instances would require the actual Minecraft class on the test runtime classpath.
 */
final class ShaderSourceInterceptorsTest {

    @Test
    void postDefineStageSeesTheIdentityRecordedByStageOne() {
        List<ShaderType> seenByStageTwo = new CopyOnWriteArrayList<>();
        ShaderSourceInterceptors.registerPostDefines(ShaderSourceInterceptors.PRIORITY_LEGACY_SUPPORT,
                (id, type, source) -> {
                    seenByStageTwo.add(type);
                    return Optional.of(source + " /* stage2 */");
                });

        ShaderSourceInterceptors.resolve(null, ShaderType.FRAGMENT, "raw");
        String result = ShaderSourceInterceptors.resolvePostDefines("raw+defines");

        assertEquals("raw+defines /* stage2 */", result);
        assertEquals(List.of(ShaderType.FRAGMENT), seenByStageTwo,
                "stage 2 must receive the ShaderType stage 1 recorded for this same compile");
    }

    @Test
    void postDefineContextIsClearedAfterASingleRead() {
        ShaderSourceInterceptors.resolve(null, ShaderType.VERTEX, "raw");
        ShaderSourceInterceptors.resolvePostDefines("first");

        // A second call with no intervening resolve() has no identity, so nothing may be dispatched -
        // this is what stops one compile's identity leaking into an unrelated later one.
        assertEquals("second", ShaderSourceInterceptors.resolvePostDefines("second"));
    }

    @Test
    void postDefineStageIsAPassthroughWhenNothingIsRegisteredForIt() {
        ShaderSourceInterceptors.resolve(null, ShaderType.FRAGMENT, "raw");
        assertEquals("untouched", ShaderSourceInterceptors.resolvePostDefines("untouched"));
    }

    /**
     * Shader compilation happens on the render thread, but the registry is global static state -
     * confirm one thread's in-flight identity can never be observed by another.
     */
    @Test
    void identityContextIsPerThread() throws Exception {
        List<String> observed = new CopyOnWriteArrayList<>();
        ShaderSourceInterceptors.registerPostDefines(ShaderSourceInterceptors.PRIORITY_SHADER_PACK,
                (id, type, source) -> {
                    observed.add(type + ":" + source);
                    return Optional.empty();
                });

        CountDownLatch bothRecorded = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);

        Runnable worker = () -> {
            ShaderSourceInterceptors.resolve(null, ShaderType.VERTEX, "raw");
            bothRecorded.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            ShaderSourceInterceptors.resolvePostDefines("vertex-source");
        };

        Thread a = new Thread(worker);
        Thread b = new Thread(worker);
        a.start();
        b.start();
        assertTrue(bothRecorded.await(5, TimeUnit.SECONDS), "both threads should record an identity");
        release.countDown();
        a.join(5_000);
        b.join(5_000);

        // Both threads interleaved their stage-1 record; each must still have resolved its own.
        assertEquals(2, observed.size());
        assertTrue(observed.stream().allMatch(s -> s.equals("VERTEX:vertex-source")), observed.toString());
    }
}
