
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

dependencies {
    compile(intellijDep()) {
        includeJars("platform-api", "platform-impl", "extensions", rootProject = rootProject)
        isTransitive = false
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

ideaPlugin()