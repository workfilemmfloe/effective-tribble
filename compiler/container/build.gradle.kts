
apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(project(":core:util.runtime"))
    compile(commonDep("javax.inject"))
    testCompile(projectDist(":kotlin-test:kotlin-test-jvm"))
    testCompile(projectDist(":kotlin-test:kotlin-test-junit"))
    testCompile(commonDep("junit:junit"))
    if (!isClionBuild()) {
        compile(ideaSdkCoreDeps("intellij-core"))
        testRuntime(ideaSdkCoreDeps("trove4j", "intellij-core"))
    } else {
        compile(clionSdkDeps("util"))
        testRuntime(clionSdkDeps("trove4j"))
        //todo[Alefas]: tests for CLion?
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar {}

projectTest {
    dependsOnTaskIfExistsRec("dist", project = rootProject)
    dependsOn(":prepare:mock-runtime-for-test:dist")
    workingDir = rootDir
}
