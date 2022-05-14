
description = "Kotlin Build Common"

apply { plugin("kotlin") }

dependencies {
    compile(project(":core:util.runtime"))
    compile(project(":compiler:util"))
    compile(project(":compiler:cli-common"))
    compile(project(":compiler:frontend.java"))
    compile(project(":js:js.serializer"))
    compile(project(":js:js.frontend"))
    if (!isClionBuild()) {
        compile(ideaSdkDeps("util"))
    } else {
        compile(clionSdkDeps("util"))
    }
    testCompile(commonDep("junit:junit"))
    testCompile(project(":compiler.tests-common"))
    testCompile(protobufFull())
    testRuntime(projectDist(":kotlin-stdlib"))
    testRuntime(projectDist(":kotlin-reflect"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

runtimeJar()

testsJar()

projectTest()
