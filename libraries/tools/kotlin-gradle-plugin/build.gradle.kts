/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.gradle.publish.PluginBundleExtension
import com.gradle.publish.PluginConfig
import org.gradle.jvm.tasks.Jar
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile

description = "Kotlin Gradle plugin"

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:0.9.13")
        classpath("com.github.jengelman.gradle.plugins:shadow:${property("versions.shadow")}")
    }
}

apply {
    `java`
    plugin("kotlin")
    plugin("groovy")
    plugin("org.jetbrains.dokka")
}

repositories {
    jcenter()
    mavenLocal()
    maven(url = "http://repository.jetbrains.com/utils/")
    maven(url = "https://maven.google.com")
}

val agp25 by configurations.creating
val agp25CompileOnly by configurations.creating
val packedJars by configurations.creating

val projectsToInclude = listOf(
        ":kotlin-build-common",
        ":core:util.runtime",
        ":compiler:cli-common",
        ":compiler:daemon-common",
        ":kotlin-daemon-client"
)

dependencies {
    projectsToInclude.forEach {
        compileOnly(project(it)) { isTransitive = false }
        packedJars(project(it)) { isTransitive = false }
    }

    compile(project(":kotlin-gradle-plugin-api"))

    compile(project(":kotlin-stdlib"))
    compile(project(":kotlin-reflect"))
    runtime(project(":kotlin-android-extensions"))

    compile(project(path = ":kotlin-annotation-processing-gradle", configuration = "runtimeJar"))

    compileOnly("com.android.tools.build:gradle:2.0.0")
    compileOnly("org.codehaus.groovy:groovy-all:2.3.9")
    compileOnly("org.jetbrains.kotlin:gradle-api:2.2")

    agp25CompileOnly("com.android.tools.build:gradle:3.0.0-alpha1")
    agp25CompileOnly("org.codehaus.groovy:groovy-all:2.3.9")
    agp25CompileOnly(gradleApi())

    testCompile(project(path = ":kotlin-build-common", configuration = "tests-jar"))
    testCompile(project(":kotlin-test::kotlin-test-junit"))
    testCompile("junit:junit:4.12")
}

val JDK_18 = rootProject.extra["JDK_18"]!!.toString()
tasks.withType<KotlinJvmCompile> {
    kotlinOptions.jdkHome = JDK_18
}

val compileKotlin = tasks.getByName("compileKotlin")!!
val compileGroovy = tasks.getByName("compileGroovy") as GroovyCompile
compileKotlin.dependsOn(compileGroovy)
compileGroovy.dependsOn.remove("compileJava")

val main = the<JavaPluginConvention>().sourceSets["main"]!!
val groovyClassesDir = file("$buildDir/mainGroovyClasses")
compileGroovy.destinationDir = groovyClassesDir
main.compileClasspath += files(groovyClassesDir)
compileGroovy.classpath = main.compileClasspath - files(groovyClassesDir)
main.java.srcDirs += files("src/main/kotlin")

the<JavaPluginConvention>().sourceSets {
    "agp25" {
        compileClasspath += configurations.compile + configurations.agp25CompileOnly + the<JavaPluginConvention>().sourceSets["main"].output
        compileClasspath += files(groovyClassesDir)
    }
}

val compileAgp25Kotlin = tasks.getByName("compileAgp25Kotlin")!!
compileAgp25Kotlin.dependsOn(compileGroovy)

val processResources = tasks.getByName("processResources") as ProcessResources
processResources.expand(project.properties)

cleanArtifacts()
noDefaultJar()
runtimeJar(task<ShadowJar>("shadowJar"))  {
    from(packedJars)
    from(compileGroovy.destinationDir)
    from(the<JavaPluginConvention>().sourceSets["agp25"]!!.output)
    from(main.output)
}
sourcesJar()
javadocJar()
publish()

val dokka = tasks.getByName("dokka") as DokkaTask
dokka.apply {
    outputFormat = "markdown"
    includes = listOf("$projectDir/Module.md")
}

projectTest {
    executable = "$JDK_18/bin/java"
}

fun PluginBundleExtension.pluginConfig(name: String, id: String, description: String) {
    val config = PluginConfig(name).also {
        it.id = id
        it.description = description
        it.displayName = description
    }
    this.plugins.add(config)
}

pluginBundle {
    pluginConfig(name = "kotlinJvmPlugin", id = "org.jetbrains.kotlin.jvm", description = "Kotlin JVM plugin")
    pluginConfig(name = "kotlinAndroidPlugin", id = "org.jetbrains.kotlin.android", description = "Kotlin Android plugin")
    pluginConfig(name = "kotlinAndroidExtensionsPlugin", id = "org.jetbrains.kotlin.android.extensions", description = "Kotlin Android Extensions plugin")
    pluginConfig(name = "kotlinKaptPlugin", id = "org.jetbrains.kotlin.kapt", description = "Kotlin Kapt plugin")
}


val install = tasks.getByName("install")!!
install.dependsOn(
        ":kotlin-android-extensions:install",
        ":kotlin-annotation-processing-gradle:install",
        ":kotlin-build-common:install",
        ":kotlin-compiler-embeddable:install",
        ":kotlin-gradle-plugin-api:install",
        ":kotlin-stdlib:install",
        ":kotlin-reflect:install"
)

// Validate that all dependencies 'install' tasks are added to 'test' dependencies
// Test dependencies are specified as paths to avoid forcing dependency resolution
// and also to avoid specifying evaluationDependsOn for each testCompile dependency.
gradle.taskGraph.whenReady {
    val notAddedTestTasks = arrayListOf<String>()
    val installDependencies = install.dependsOn
    val runtimeConfigurations = sequenceOf("runtime", "runtimeOnly").map { configurations.getByName(it) }
    val allDependencies = runtimeConfigurations.flatMap { it.allDependencies.asSequence() }

    for (dependency in allDependencies.filterIsInstance<ProjectDependency>()) {
        val task = dependency.dependencyProject.tasks.findByName("install")
        if (task != null && task.path !in installDependencies) {
            notAddedTestTasks.add("\"${task.path}\"")
        }
    }

    if (!notAddedTestTasks.isEmpty()) {
        val tasksLines = notAddedTestTasks.joinToString(",\n  ")
        throw GradleException("Add the following tasks to ${install.path} dependencies:\n  $tasksLines")
    }
}