apply { plugin("kotlin") }

dependencies {

    compileOnly(ideaSdkDeps("openapi", "idea"))
    compileOnly(ideaPluginDeps("gradle-tooling-api", "gradle", plugin = "gradle"))
    compileOnly(ideaPluginDeps("Groovy", plugin = "Groovy"))

    compile(project(":idea"))
    compile(project(":idea:idea-jvm"))
    compile(project(":idea:kotlin-gradle-tooling"))

    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:frontend.script"))

    compile(project(":js:js.frontend"))

}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}