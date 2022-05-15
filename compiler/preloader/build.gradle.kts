
description = "Kotlin Preloader"

apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compileOnly(intellijDep()) { includeJars("asm-all") }
}

sourceSets {
    "main" {
        java {
            srcDirs( "src", "instrumentation/src")
        }
    }
    "test" {}
}

runtimeJar {
    manifest.attributes.put("Main-Class", "org.jetbrains.kotlin.preloading.Preloader")
}

dist()
