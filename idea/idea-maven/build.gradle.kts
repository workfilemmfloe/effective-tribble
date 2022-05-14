
apply { plugin("kotlin") }

dependencies {
    compileOnly(ideaSdkDeps("openapi", "idea", "gson"))
    compileOnly(ideaPluginDeps("maven", "maven-server-api", plugin = "maven"))

    compile(project(":core:util.runtime"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:util"))
    compile(project(":compiler:cli-common"))
    compile(project(":kotlin-build-common"))

    compile(project(":js:js.frontend"))

    compile(project(":idea"))
    compile(project(":idea:idea-jvm"))
    compile(project(":idea:idea-jps-common"))

    testCompile(project(":compiler.tests-common"))
    testCompile(project(":idea:idea-test-framework"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}