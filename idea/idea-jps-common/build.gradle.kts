
apply { plugin("kotlin") }

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compile(project(":compiler:util"))
    compile(project(":compiler:cli-common"))
    compile(project(":compiler:frontend.java"))
    if (!isClionBuild()) {
        compile(ideaSdkCoreDeps("intellij-core", "util"))
    } else {
        compile(clionSdkDeps("util", "clion", "jdom"))
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

