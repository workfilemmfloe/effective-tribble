extra["versions.shadow"] = "4.0.3"
extra["versions.native-platform"] = "0.14"

buildscript {
    val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

    val buildSrcKotlinVersion: String by extra(findProperty("buildSrc.kotlin.version")?.toString() ?: embeddedKotlinVersion)
    val buildSrcKotlinRepo: String? by extra(findProperty("buildSrc.kotlin.repo") as String?)

    repositories {
        if (cacheRedirectorEnabled) {
            maven("https://cache-redirector.jetbrains.com/jcenter.bintray.com")
        } else {
            jcenter()
        }

        buildSrcKotlinRepo?.let {
            maven(url = it)
        }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$buildSrcKotlinVersion")
        classpath("org.jetbrains.kotlin:kotlin-sam-with-receiver:$buildSrcKotlinVersion")
    }
}

val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

logger.info("buildSrcKotlinVersion: " + extra["buildSrcKotlinVersion"])
logger.info("buildSrc kotlin compiler version: " + org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION)
logger.info("buildSrc stdlib version: " + KotlinVersion.CURRENT)

apply {
    plugin("kotlin")
    plugin("kotlin-sam-with-receiver")
}

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        register("pill-configurable") {
            id = "pill-configurable"
            implementationClass = "org.jetbrains.kotlin.pill.PillConfigurablePlugin"
        }
        register("jps-compatible") {
            id = "jps-compatible"
            implementationClass = "org.jetbrains.kotlin.pill.JpsCompatiblePlugin"
        }
    }
}

fun Project.getBooleanProperty(name: String): Boolean? = this.findProperty(name)?.let {
    val v = it.toString()
    if (v.isBlank()) true
    else v.toBoolean()
}

rootProject.apply {
    from(rootProject.file("../gradle/versions.gradle.kts"))
}

val isTeamcityBuild = project.hasProperty("teamcity") || System.getenv("TEAMCITY_VERSION") != null
val intellijUltimateEnabled by extra(project.getBooleanProperty("intellijUltimateEnabled") ?: isTeamcityBuild)
val intellijSeparateSdks by extra(project.getBooleanProperty("intellijSeparateSdks") ?: false)
val verifyDependencyOutput by extra( getBooleanProperty("kotlin.build.dependency.output.verification") ?: isTeamcityBuild)

extra["intellijReleaseType"] = if (extra["versions.intellijSdk"]?.toString()?.endsWith("SNAPSHOT") == true)
    "snapshots"
else
    "releases"

extra["versions.androidDxSources"] = "5.0.0_r2"

extra["customDepsOrg"] = "kotlin.build"

repositories {
    maven("https://jetbrains.bintray.com/intellij-third-party-dependencies/")
    maven("https://plugins.gradle.org/m2/")
    jcenter()

    extra["buildSrcKotlinRepo"]?.let {
        maven(url = it)
    }
}

dependencies {
    compile(kotlin("stdlib", embeddedKotlinVersion))
    compile("net.rubygrapefruit:native-platform:${property("versions.native-platform")}")
    compile("net.rubygrapefruit:native-platform-windows-amd64:${property("versions.native-platform")}")
    compile("net.rubygrapefruit:native-platform-windows-i386:${property("versions.native-platform")}")
    compile("com.jakewharton.dex:dex-method-list:3.0.0")

    compile("com.github.jengelman.gradle.plugins:shadow:${property("versions.shadow")}")
    compile("org.jetbrains.intellij.deps:asm-all:7.0.1")

    compile("gradle.plugin.org.jetbrains.gradle.plugin.idea-ext:gradle-idea-ext:0.5")
}

samWithReceiver {
    annotation("org.gradle.api.HasImplicitReceiver")
}

fun Project.`samWithReceiver`(configure: org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverExtension.() -> Unit): Unit =
        extensions.configure("samWithReceiver", configure)

tasks["build"].dependsOn(":prepare-deps:build")

allprojects {
    tasks.register("checkBuild")

    afterEvaluate {
        apply(from = "$rootDir/../gradle/cacheRedirector.gradle.kts")
    }
}