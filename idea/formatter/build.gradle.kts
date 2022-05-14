
apply { plugin("kotlin") }

dependencies {
    compile(project(":compiler:frontend"))
    if (!isClionBuild()) {
        compile(ideaSdkDeps("openapi"))
    } else {
        compile(clionSdkDeps("openapi"))
        compile(preloadedDeps("java-api", "java-impl"))
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

