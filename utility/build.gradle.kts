// Utility engine: OptiFine-parity QoL features and shader pipeline support.
// Configuration inherited from the root project's `subprojects` block.
//
// Phase 5 brings this module into existence as a real engine: it has an entry point, a config
// panel and the "safe-by-default on weak hardware" policy wired to core's hardware detection.
// It ships no actual QoL features yet - those are Phase 6 - and deliberately has no
// mixins.utility.json until there is something to inject.

dependencies {
    implementation(project(":core"))

    compileOnly(project(":mcstubs"))
}

tasks.test {
    failOnNoDiscoveredTests.set(false)
}
