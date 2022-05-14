
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

buildscript {
    repositories {
        mavenLocal()
        maven { setUrl(rootProject.extra["repo"]) }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${rootProject.extra["kotlinVersion"]}")
    }
}

apply { plugin("kotlin") }

repositories {
    mavenLocal()
    maven { setUrl(rootProject.extra["repo"]) }
    mavenCentral()
}

val nativePlatformUberjar = "$rootDir/dependencies/native-platform-uberjar.jar"

dependencies {
    compile(project(":compiler"))
    compile(files(nativePlatformUberjar))
    buildVersion()
}

configureKotlinProjectSources("compiler/daemon/daemon-client/src", sourcesBaseDir = rootDir)
configureKotlinProjectNoTests()

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs = listOf("-Xallow-kotlin-package", "-module-name", "kotlin-daemon-client")
}

tasks.withType<Jar> {
    setupRuntimeJar("Kotlin Daemon Client")
    from(zipTree(nativePlatformUberjar))
    archiveName = "kotlin-daemon-client.jar"
}

fixKotlinTaskDependencies()
