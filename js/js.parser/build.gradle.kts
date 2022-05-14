
apply { plugin("kotlin") }

dependencies {
    compile(project(":compiler:util"))
    compile(project(":js:js.ast"))
    compile(preloadedDeps("json-org"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

