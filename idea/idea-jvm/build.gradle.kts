
apply { plugin("kotlin") }

dependencies {
    compile(project(":idea"))
    compile(project(":compiler:light-classes"))
    compile(project(":compiler:frontend.java"))

    compileOnly(ideaSdkDeps("openapi", "idea", "boot", "gson", "swingx-core"))

    compile(ideaPluginDeps("idea-junit", plugin = "junit"))
    compile(ideaPluginDeps("testng", "testng-plugin", plugin = "testng"))

    compile(ideaPluginDeps("coverage", plugin = "coverage"))

    compile(ideaPluginDeps("java-decompiler", plugin = "java-decompiler"))

    compile(ideaPluginDeps("IntelliLang", plugin = "IntelliLang"))
    compile(ideaPluginDeps("copyright", plugin = "copyright"))
    compile(ideaPluginDeps("properties", plugin = "properties"))
    compile(ideaPluginDeps("java-i18n", plugin = "java-i18n"))
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDirs("../idea-repl/src")
    }
}

