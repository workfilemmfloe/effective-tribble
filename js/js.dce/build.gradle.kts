
apply { plugin("kotlin") }

dependencies {
    compile(project(":compiler:util"))
    compile(project(":js:js.ast"))
    compile(project(":js:js.translator"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

