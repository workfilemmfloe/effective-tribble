
apply { plugin("kotlin") }

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compileOnly(intellijDep()) { includeJars("openapi", "platform-api", "platform-impl", "java-api", "util", "jdom") }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

