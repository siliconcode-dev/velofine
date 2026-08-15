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

package dev.velofine.launcher;

import com.sun.tools.attach.VirtualMachine;

import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Entry point referenced as {@code mainClass} by Velofine's generated launcher profile.
 *
 * <p>Two invocation modes, dispatched on {@code args[0]}:
 * <ul>
 *   <li>{@code --install-profile <minecraftDir> --vanilla-version <id>} / {@code --uninstall-profile <minecraftDir>}
 *       — run by the installer's post-install step (see the {@code installer} module), not by Mojang's launcher.</li>
 *   <li>anything else — the normal path: self-attach {@link VelofineAgent}, then hand off to vanilla's real
 *       main class, whose fully-qualified name is passed via the {@code velofine.vanillaMainClass} system
 *       property set in the generated profile's {@code javaArgs}.</li>
 * </ul>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && "--install-profile".equals(args[0])) {
            ProfileInstaller.install(Arrays.copyOfRange(args, 1, args.length));
            return;
        }
        if (args.length > 0 && "--uninstall-profile".equals(args[0])) {
            ProfileInstaller.uninstall(Arrays.copyOfRange(args, 1, args.length));
            return;
        }

        captureGameDir(args);
        selfAttachAgent();
        handOffToVanilla(args);
    }

    /**
     * Minecraft's own launch args always include {@code --gameDir <path>} (the instance directory
     * the launcher just set as this JVM's working directory) — publish it as a system property
     * before self-attaching so the agent (same JVM, self-attach) can read it without needing a
     * separate profile-JSON flag (the game directory isn't known at profile-generation time, only
     * at each actual launch).
     */
    private static void captureGameDir(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if ("--gameDir".equals(args[i])) {
                System.setProperty("velofine.gameDir", args[i + 1]);
                return;
            }
        }
    }

    private static void selfAttachAgent() {
        try {
            String pid = String.valueOf(ProcessHandle.current().pid());
            String jarPath = currentJarPath();
            VirtualMachine vm = VirtualMachine.attach(pid);
            try {
                vm.loadAgent(jarPath);
            } finally {
                vm.detach();
            }
            System.out.println("[Velofine] agent attached (self-attach, pid=" + pid + ")");
        } catch (Exception e) {
            System.err.println("[Velofine] FAILED to self-attach agent: " + e);
            e.printStackTrace();
            throw new IllegalStateException("Velofine agent could not attach; refusing to launch unmodified vanilla "
                    + "silently. Check that -Djdk.attach.allowAttachSelf=true is present in this profile's javaArgs.", e);
        }
    }

    private static String currentJarPath() {
        try {
            Path path = Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return path.toAbsolutePath().toString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Could not resolve Velofine's own jar path for self-attach", e);
        }
    }

    private static void handOffToVanilla(String[] args) throws Exception {
        String vanillaMainClass = System.getProperty("velofine.vanillaMainClass");
        if (vanillaMainClass == null || vanillaMainClass.isBlank()) {
            throw new IllegalStateException(
                    "System property velofine.vanillaMainClass is not set; the generated profile's javaArgs "
                            + "must include -Dvelofine.vanillaMainClass=<vanilla's real main class>");
        }

        System.out.println("[Velofine] handing off to vanilla main class: " + vanillaMainClass);
        Class<?> vanillaMain = Class.forName(vanillaMainClass);
        Method main = vanillaMain.getMethod("main", String[].class);
        main.invoke(null, (Object) args);
    }
}
