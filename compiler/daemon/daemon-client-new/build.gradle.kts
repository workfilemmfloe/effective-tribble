import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

description = "Kotlin Daemon Client New"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

val nativePlatformVariants = listOf(
    "windows-amd64",
    "windows-i386",
    "osx-amd64",
    "osx-i386",
    "linux-amd64",
    "linux-i386",
    "freebsd-amd64-libcpp",
    "freebsd-amd64-libstdcpp",
    "freebsd-i386-libcpp",
    "freebsd-i386-libstdcpp"
)

dependencies {
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":compiler:daemon-common-new"))
    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(project(":kotlin-daemon-client"))
    embeddedComponents(project(":kotlin-daemon-client"))
    compileOnly(project(":js:js.frontend"))
    compileOnly(commonDep("net.rubygrapefruit", "native-platform"))
    compileOnly(intellijDep()) { includeIntellijCoreJarDependencies(project) }

    embeddedComponents(project(":compiler:daemon-common")) { isTransitive = false }
    embeddedComponents(commonDep("net.rubygrapefruit", "native-platform"))
    nativePlatformVariants.forEach {
        embeddedComponents(commonDep("net.rubygrapefruit", "native-platform", "-$it"))
    }
    compile(projectDist(":kotlin-reflect"))
    compile(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8")) {
        isTransitive = false
    }
    compile(commonDep("io.ktor", "ktor-network")) {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-jdk8")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-common")
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

noDefaultJar()

runtimeJar(task<ShadowJar>("shadowJar")) {
    from(mainSourceSet.output)
    fromEmbeddedComponents()
}

sourcesJar()

javadocJar()

dist()

ideaPlugin()

publish()
