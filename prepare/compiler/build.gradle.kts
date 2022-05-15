import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import proguard.gradle.ProGuardTask

description = "Kotlin Compiler"

plugins {
    // HACK: java plugin makes idea import dependencies on this project as source (with empty sources however),
    // this prevents reindexing of kotlin-compiler.jar after build on every change in compiler modules
    java
}

// You can run Gradle with "-Pkotlin.build.proguard=true" to enable ProGuard run on kotlin-compiler.jar (on TeamCity, ProGuard always runs)
val shrink = findProperty("kotlin.build.proguard")?.toString()?.toBoolean() ?: hasProperty("teamcity")

val fatJarContents by configurations.creating
val fatJarContentsStripMetadata by configurations.creating
val fatJarContentsStripServices by configurations.creating

// JPS build assumes fat jar is built from embedded configuration,
// but we can't use it in gradle build since slightly more complex processing is required like stripping metadata & services from some jars
if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
    val embedded by configurations
    embedded.apply {
        extendsFrom(fatJarContents)
        extendsFrom(fatJarContentsStripMetadata)
        extendsFrom(fatJarContentsStripServices)
    }
}

val runtimeJar by configurations.creating
val compile by configurations  // maven plugin writes pom compile scope from compile configuration by default
val proguardLibraries by configurations.creating {
    extendsFrom(compile)
}

// Libraries to copy to the lib directory
val libraries by configurations.creating

val default by configurations
default.extendsFrom(runtimeJar)

val compilerBaseName = name

val outputJar = fileFrom(buildDir, "libs", "$compilerBaseName.jar")

val compilerModules: Array<String> by rootProject.extra

dependencies {
    compile(kotlinStdlib())
    compile(project(":kotlin-script-runtime"))
    compile(project(":kotlin-reflect"))
    compile(commonDep("org.jetbrains.intellij.deps", "trove4j"))

    proguardLibraries(project(":kotlin-annotations-jvm"))
    proguardLibraries(
        files(
            firstFromJavaHomeThatExists("jre/lib/rt.jar", "../Classes/classes.jar"),
            firstFromJavaHomeThatExists("jre/lib/jsse.jar", "../Classes/jsse.jar"),
            toolsJar()
        )
    )

    compilerModules.forEach {
        fatJarContents(project(it)) { isTransitive = false }
    }

    libraries(intellijDep()) { includeIntellijCoreJarDependencies(project) { it.startsWith("trove4j") } }
    libraries(commonDep("io.ktor", "ktor-network"))
    libraries(kotlinStdlib("jdk8"))

    libraries(project(":kotlin-annotation-processing-runtime")) { isTransitive = false }
    libraries(project(":kotlin-android-extensions-runtime")) { isTransitive = false }
    libraries(project(":kotlin-scripting-jvm")) { isTransitive = false }
    libraries(project(":kotlin-annotations-android")) { isTransitive = false }
    libraries(project(":kotlin-annotation-processing")) { isTransitive = false }
    libraries(project(":kotlin-reflect")) { isTransitive = false }
    libraries(project(":plugins:android-extensions-compiler")) { isTransitive = false }
    libraries(project(":kotlin-compiler")) { isTransitive = false }
    libraries(project(":kotlin-scripting-compiler")) { isTransitive = false }
    libraries(project(":plugins:jvm-abi-gen")) { isTransitive = false }
    libraries(project(":kotlin-scripting-common")) { isTransitive = false }
    libraries(project(":kotlin-test:kotlin-test-testng")) { isTransitive = false }
    libraries(project(":kotlin-annotation-processing-cli")) { isTransitive = false }
    libraries(project(":kotlin-runner")) { isTransitive = false }
    libraries(project(":kotlin-test:kotlin-test-junit5")) { isTransitive = false }
    libraries(project(":kotlin-imports-dumper-compiler-plugin")) { isTransitive = false }
    libraries(project(":kotlin-test:kotlin-test-js")) { isTransitive = false }
    libraries(project(":kotlin-annotations-jvm")) { isTransitive = false }
    libraries(project(":kotlinx-serialization-compiler-plugin")) { isTransitive = false }
    libraries(project(":kotlin-daemon-client")) { isTransitive = false }
    libraries(project(":kotlin-test:kotlin-test-jvm")) { isTransitive = false }
    libraries(project(":kotlin-test:kotlin-test-common")) { isTransitive = false }
    libraries(project(":kotlin-test:kotlin-test-junit")) { isTransitive = false }
    libraries(project(":kotlin-sam-with-receiver-compiler-plugin")) { isTransitive = false }
    libraries(project(":kotlin-preloader")) { isTransitive = false }
    libraries(project(":kotlin-allopen-compiler-plugin")) { isTransitive = false }
    libraries(project(":kotlin-noarg-compiler-plugin")) { isTransitive = false }
    libraries(project(":kotlin-main-kts")) { isTransitive = false }
    libraries(project(":kotlin-source-sections-compiler-plugin")) { isTransitive = false }
    libraries(project(":kotlin-script-runtime")) { isTransitive = false }
    libraries(project(":kotlin-ant")) { isTransitive = false }
    libraries(project(":kotlin-test:kotlin-test-annotations-common")) { isTransitive = false }
    libraries(project(":libraries:tools:mutability-annotations-compat")) { isTransitive = false }

    fatJarContents(kotlinBuiltins())
    fatJarContents(commonDep("javax.inject"))
    fatJarContents(commonDep("org.jline", "jline"))
    fatJarContents(commonDep("org.fusesource.jansi", "jansi"))
    fatJarContents(protobufFull())
    fatJarContents(commonDep("com.google.code.findbugs", "jsr305"))
    fatJarContents(commonDep("io.javaslang", "javaslang"))
    fatJarContents(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }

    fatJarContents(intellijCoreDep()) { includeJars("intellij-core") }
    fatJarContents(intellijDep()) { includeJars("jna-platform") }

    if (Platform.P192.orHigher()) {
        fatJarContents(intellijDep()) { includeJars("lz4-java-1.6.0") }
    } else {
        fatJarContents(intellijDep()) { includeJars("lz4-1.3.0") }
    }
    
    if (Platform.P183.orHigher()) {
        fatJarContents(intellijCoreDep()) { includeJars("java-compatibility-1.0.1") }
    }

    fatJarContents(intellijDep()) {
        includeIntellijCoreJarDependencies(project) {
            !(it.startsWith("jdom") || it.startsWith("log4j") || it.startsWith("trove4j"))
        }
    }

    fatJarContentsStripServices(jpsStandalone()) { includeJars("jps-model") }

    fatJarContentsStripMetadata(intellijDep()) { includeJars("oro-2.0.8", "jdom", "log4j" ) }
}

publish()

noDefaultJar()

val packCompiler by task<ShadowJar> {
    configurations = listOf(fatJarContents)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    destinationDir = File(buildDir, "libs")

    setupPublicJar(compilerBaseName, "before-proguard")

    dependsOn(fatJarContentsStripServices)
    from {
        fatJarContentsStripServices.files.map {
            zipTree(it).matching { exclude("META-INF/services/**") }
        }
    }

    dependsOn(fatJarContentsStripMetadata)
    from {
        fatJarContentsStripMetadata.files.map {
            zipTree(it).matching { exclude("META-INF/jb/**", "META-INF/LICENSE") }
        }
    }

    manifest.attributes["Class-Path"] = compilerManifestClassPath
    manifest.attributes["Main-Class"] = "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler"
}

val proguard by task<ProGuardTask> {
    dependsOn(packCompiler)
    configuration("$rootDir/compiler/compiler.pro")

    val outputJar = fileFrom(buildDir, "libs", "$compilerBaseName-after-proguard.jar")

    inputs.files(packCompiler.outputs.files.singleFile)
    outputs.file(outputJar)

    libraryjars(mapOf("filter" to "!META-INF/versions/**"), proguardLibraries)

    printconfiguration("$buildDir/compiler.pro.dump")
}

val pack = if (shrink) proguard else packCompiler

dist(targetName = "$compilerBaseName.jar", fromTask = pack) {
    from(libraries)
}

runtimeJarArtifactBy(pack, pack.outputs.files.singleFile) {
    name = compilerBaseName
    classifier = ""
}

sourcesJar {
    from {
        compilerModules.map {
            project(it).mainSourceSet.allSource
        }
    }
}

javadocJar()

tasks.register("listDistProjects") {
    doLast {
        rootProject.getTasksByName("dist", true).forEach {
            println(it.project.path)
        }
    }
}