import org.jetbrains.kotlin.gradle.dsl.Coroutines

plugins {
    kotlin("jvm")
}

jvmTarget = "1.6"

dependencies {
    compile(project(":kotlin-script-runtime"))
    compile(projectDist(":kotlin-stdlib"))
    compile(project(":kotlin-scripting-common"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

kotlin.experimental.coroutines = Coroutines.ENABLE

dist()

ideaPlugin()

standardPublicJars()

publish()
