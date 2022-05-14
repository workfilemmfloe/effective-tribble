import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar

description = "Kotlin CLion plugin"

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:1.2.3")
    }
}

plugins {
    `java-base`
}

val projectsToShadow = listOf(
        ":compiler:backend",
        ":compiler:backend-common",
        ":kotlin-build-common",
        ":compiler:cli-common",
        ":compiler:container",
        ":compiler:daemon-common",
        ":core",
        ":idea:formatter",
        ":compiler:frontend",
        ":compiler:frontend.java",
        ":compiler:frontend.script",
        ":idea:ide-common",
        ":idea",
        ":idea:idea-core",
        ":idea:idea-jps-common",
        ":compiler:ir.psi2ir",
        ":compiler:ir.tree",
        ":j2k",
        ":js:js.ast",
        ":js:js.frontend",
        ":js:js.parser",
        ":js:js.serializer",
        ":compiler:light-classes",
        ":compiler:plugin-api",
        ":kotlin-preloader",
        ":compiler:resolution",
        ":compiler:serialization",
        ":compiler:util",
        ":core:util.runtime")

val packedJars by configurations.creating
val sideJars by configurations.creating

dependencies {
    packedJars(commonDep("com.github.spullara.cli-parser", "cli-parser"))
    packedJars(preloadedDeps("protobuf-${rootProject.extra["versions.protobuf-java"]}"))
    packedJars(project(":kotlin-stdlib", configuration = "builtins"))
    sideJars(projectDist(":kotlin-script-runtime"))
    sideJars(projectDist(":kotlin-stdlib"))
    sideJars(projectDist(":kotlin-reflect"))
    sideJars(commonDep("io.javaslang", "javaslang"))
    sideJars(commonDep("javax.inject"))
    sideJars(preloadedDeps("markdown", "kotlinx-coroutines-core", "kotlinx-coroutines-jdk8", "java-api", "java-impl"))
    sideJars(ideaSdkDeps("asm-all"))
}

val jar = runtimeJar(task<ShadowJar>("shadowJar")) {
    from(files("$rootDir/resources/kotlinManifest.properties"))
    from(packedJars)
    for (p in projectsToShadow) {
        dependsOn("$p:classes")
        from(project(p).the<JavaPluginConvention>().sourceSets.getByName("main").output)
    }
}

clionPlugin {
    shouldRunAfter(":dist")
    from(jar)
    from(sideJars)
}

