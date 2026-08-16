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

package dev.velofine.diagnostics.pipeline;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Determines how to relaunch this same tool as a child process, for {@code ChildProcessSupervisor}
 * to append {@code --pipeline-worker <args-file>} to.
 *
 * <p>Prefers {@code ProcessHandle.current().info().command()} - reliable inside the packaged
 * jpackage app-image, since the native launcher exe <i>is</i> the OS process, and jpackage's
 * launcher forwards argv straight through to {@code Main.main(args)} with no {@code -jar}/{@code -cp}
 * needed. Falls back to reconstructing {@code java -jar <this running jar>} for dev/manual runs
 * (e.g. {@code java -jar diagnostics-tool-0.1.0.jar}), using the same codeSource-lookup trick
 * {@code report.ReportPaths.runningLocation()} already establishes elsewhere in this module.
 *
 * <p>Only the fallback branch is realistically unit-testable (it's pure path logic); the
 * packaged-exe branch depends on how the OS actually launched this process and is
 * manual-verify-only, consistent with this module's existing GL-code testing precedent.
 */
public final class SelfLauncher {

    private SelfLauncher() {
    }

    public static List<String> buildRelaunchCommand() {
        Optional<String> command = ProcessHandle.current().info().command();
        if (command.isPresent() && !isRawJavaBinary(command.get())) {
            return List.of(command.get());
        }
        String javaBin = command.orElseGet(SelfLauncher::defaultJavaBinaryPath);
        return List.of(javaBin, "-jar", runningJarPath());
    }

    static boolean isRawJavaBinary(String command) {
        String name = Path.of(command).getFileName().toString().toLowerCase(Locale.ROOT);
        return name.equals("java") || name.equals("java.exe") || name.equals("javaw") || name.equals("javaw.exe");
    }

    private static String defaultJavaBinaryPath() {
        String javaHome = System.getProperty("java.home");
        String exeName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win") ? "java.exe" : "java";
        return Path.of(javaHome, "bin", exeName).toString();
    }

    private static String runningJarPath() {
        try {
            return Path.of(SelfLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (URISyntaxException | NullPointerException e) {
            throw new IllegalStateException("Could not determine the running jar's own location to relaunch it", e);
        }
    }
}
