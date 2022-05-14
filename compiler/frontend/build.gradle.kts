
apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(project(":core"))
    compile(project(":compiler:util"))
    compile(project(":compiler:container"))
    compile(project(":compiler:resolution"))
    compile(projectDist(":kotlin-script-runtime"))
    compile(commonDep("io.javaslang","javaslang"))
    if (isClionBuild()) {
        compile(preloadedDeps("java-api", "java-impl"))
        compile(clionSdkDeps("extensions", "clion"))
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

