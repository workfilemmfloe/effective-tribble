
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile("org.jetbrains.kotlin:kotlin-native-shared")
    compile(project(":kotlin-native:backend.native"))

    compile("org.jetbrains.kotlin:kotlin-native-gradle-plugin")

    compileOnly(project(":idea:idea-gradle"))
    compileOnly(project(":idea:idea-native"))

    compileOnly(project(":idea")) { isTransitive = false }
    compileOnly(project(":idea:idea-jvm"))
    compile(project(":idea:kotlin-gradle-tooling"))

    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:frontend.script"))

    compile(project(":js:js.frontend"))

    compileOnly(intellijDep())
    compileOnly(intellijPluginDep("gradle"))
    compileOnly(intellijPluginDep("Groovy"))
    compileOnly(intellijPluginDep("junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

configureInstrumentation()
