import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.apply
import org.jetbrains.kotlin.ideaExt.idea

inline fun Project.sourceSets(crossinline body: SourceSetsBuilder.() -> Unit) = SourceSetsBuilder(this).body()

class SourceSetsBuilder(val project: Project) {

    inline operator fun String.invoke(crossinline body: SourceSet.() -> Unit): SourceSet {
        val sourceSetName = this
        return project.sourceSets.maybeCreate(sourceSetName).apply {
            none()
            body()
        }
    }
}

fun SourceSet.none() {
    java.setSrcDirs(emptyList<String>())
    resources.setSrcDirs(emptyList<String>())
}

val SourceSet.projectDefault: Project.() -> Unit
    get() = {
        when (this@projectDefault.name) {
            "main" -> {
                java.srcDirs("src")
                this@projectDefault.resources.srcDir("resources")
            }
            "test" -> {
                java.srcDirs("test", "tests")
                this@projectDefault.resources.srcDir("testResources")
            }
        }
    }

val SourceSet.generatedTestDir: Project.() -> Unit
    get() = {
        val generationRoot = projectDir.resolve("tests-gen")
        java.srcDir(generationRoot.name)

        if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
            apply(plugin = "idea")
            idea {
                this.module.generatedSourceDirs.add(generationRoot)
            }
        }
    }


val Project.sourceSets: SourceSetContainer
    get() = javaPluginExtension().sourceSets

val Project.mainSourceSet: SourceSet
    get() = javaPluginExtension().mainSourceSet

val Project.testSourceSet: SourceSet
    get() = javaPluginExtension().testSourceSet

val JavaPluginExtension.mainSourceSet: SourceSet
    get() = sourceSets.getByName("main")

val JavaPluginExtension.testSourceSet: SourceSet
    get() = sourceSets.getByName("test")
