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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.platform.container.ContainerHandleURI;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.LoggerAdapterConsole;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.ITransformerProvider;
import org.spongepowered.asm.service.MixinServiceAbstract;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

/**
 * Velofine's {@code IMixinService} for our bare-agent environment: no ModLauncher, no Fabric
 * Knot, no custom classloader. Single classloader (the system/application classloader Mojang's
 * launcher started us with), single mixin container (our own shaded jar), single transformer
 * (Mixin's own — we register no other {@code ClassFileTransformer}s to delegate to).
 *
 * <p>Discovered via {@code ServiceLoader} through {@code META-INF/services/
 * org.spongepowered.asm.service.IMixinService}. The actual bytecode-transform wiring lives in
 * {@link MixinBridge}, not here — this class only satisfies Mixin's service contract.
 */
public final class VelofineMixinService extends MixinServiceAbstract implements IClassProvider, IClassBytecodeProvider {

    @Override
    public String getName() {
        return "Velofine";
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Phase getInitialPhase() {
        // MixinServiceAbstract's default (PREINIT) is for staged mod-loader lifecycles (FML
        // pre-init, etc.) we don't have. Confirmed via VerifyMixinsHarness: Mixins.addConfiguration
        // resolves its fallback MixinEnvironment via getDefaultEnvironment() (always Phase.DEFAULT),
        // while the transformer looks up the *current* environment via getCurrentEnvironment()
        // (whatever getInitialPhase() returned) - PREINIT vs DEFAULT are different MixinEnvironment
        // objects, so MixinConfig#select's reference-equality check silently excluded our config
        // from ever being applied. Returning DEFAULT here makes both resolve to the same object.
        return Phase.DEFAULT;
    }

    @Override
    protected ILogger createLogger(String name) {
        return new LoggerAdapterConsole(name);
    }

    @Override
    public IClassProvider getClassProvider() {
        return this;
    }

    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        return this;
    }

    @Override
    public ITransformerProvider getTransformerProvider() {
        // No other transformers exist in this environment to delegate to/interoperate with.
        return null;
    }

    @Override
    public IClassTracker getClassTracker() {
        return null;
    }

    @Override
    public IMixinAuditTrail getAuditTrail() {
        return null;
    }

    @Override
    public Collection<String> getPlatformAgents() {
        return Collections.emptyList();
    }

    @Override
    public IContainerHandle getPrimaryContainer() {
        try {
            return new ContainerHandleURI(VelofineMixinService.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Could not resolve Velofine's own jar as the primary mixin container", e);
        }
    }

    @Override
    public Collection<IContainerHandle> getMixinContainers() {
        return Collections.singletonList(getPrimaryContainer());
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return VelofineMixinService.class.getClassLoader().getResourceAsStream(name);
    }

    @Override
    public void checkEnv(Object bootSource) {
        // Single classloader environment; nothing to validate.
    }

    @Override
    public void beginPhase() {
        // No-op: MixinBridge drives transformation directly via Instrumentation, not a
        // delegated/registered transformer chain like LaunchWrapper's.
    }

    /**
     * {@code MixinServiceAbstract#getInternal} is {@code protected} - accessible to subclasses,
     * not to arbitrary same-package callers like {@link MixinBridge}. This just re-exposes it.
     */
    public <T extends org.spongepowered.asm.service.IMixinInternal> T internal(Class<T> type) {
        return this.getInternal(type);
    }

    // -- IClassProvider --

    @Override
    @Deprecated
    public URL[] getClassPath() {
        return new URL[0];
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }

    @Override
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, VelofineMixinService.class.getClassLoader());
    }

    @Override
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, VelofineMixinService.class.getClassLoader());
    }

    // -- IClassBytecodeProvider --

    @Override
    public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
        return getClassNode(name, true, ClassReader.EXPAND_FRAMES);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
        return getClassNode(name, runTransformers, ClassReader.EXPAND_FRAMES);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers, int readerFlags) throws ClassNotFoundException, IOException {
        String resource = name.replace('.', '/') + ".class";
        byte[] bytes;
        try (InputStream in = getResourceAsStream(resource)) {
            if (in == null) {
                throw new ClassNotFoundException(name);
            }
            bytes = in.readAllBytes();
        }
        if (runTransformers) {
            bytes = MixinBridge.transform(name, name, bytes);
        }
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, readerFlags);
        return node;
    }
}
