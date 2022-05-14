import org.gradle.api.*
import org.gradle.api.tasks.*
import org.gradle.script.lang.kotlin.*
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.SourceDirectorySet
import org.gradle.jvm.tasks.Jar
import java.io.File


fun Project.fixKotlinTaskDependencies() {
    the<JavaPluginConvention>().sourceSets.all { sourceset ->
        val taskName = if (sourceset.name == "main") "classes" else (sourceset.name + "Classes")
        tasks.withType<Task> {
            if (name == taskName) {
                dependsOn("copy${sourceset.name.capitalize()}KotlinClasses")
            }
        }
    }
}

fun Jar.setupRuntimeJar(implementationTitle: String): Unit {
    dependsOn(":prepare:build.version:prepare")
    manifest.attributes.apply {
        put("Built-By", project.rootProject.extra["manifest.impl.vendor"])
        put("Implementation-Vendor", project.rootProject.extra["manifest.impl.vendor"])
        put("Implementation-Title", implementationTitle)
        put("Implementation-Version", project.rootProject.extra["build.number"])
    }
    from(project.configurations.getByName("build-version").files, action = { into("META-INF/") })
}

fun Project.buildVersion(): Dependency {
    val cfg = configurations.create("build-version")
    return dependencies.add(cfg.name, dependencies.project(":prepare:build.version", configuration = "default"))
}

fun Project.commonDep(coord: String): String {
    val parts = coord.split(':')
    return when (parts.size) {
        1 -> "$coord:$coord:${rootProject.extra["versions.$coord"]}"
        2 -> "${parts[0]}:${parts[1]}:${rootProject.extra["versions.${parts[1]}"]}"
        3 -> coord
        else -> throw IllegalArgumentException("Illegal maven coordinates: $coord")
    }
}

fun Project.commonDep(group: String, artifact: String): String = "$group:$artifact:${rootProject.extra["versions.$artifact"]}"

fun DependencyHandler.projectDep(name: String): Dependency = project(name, configuration = "default")
fun DependencyHandler.projectDepIntransitive(name: String): Dependency =
        project(name, configuration = "default").apply { isTransitive = false }

val protobufLiteProject = ":custom-dependencies:protobuf-lite"
fun KotlinDependencyHandler.protobufLite(): ProjectDependency =
        project(protobufLiteProject, configuration = "default").apply { isTransitive = false }
val protobufLiteTask = "$protobufLiteProject:prepare"

fun KotlinDependencyHandler.protobufFull(): ProjectDependency =
        project(protobufLiteProject, configuration = "relocated").apply { isTransitive = false }
val protobufFullTask = "$protobufLiteProject:prepare-relocated-protobuf"

fun Project.getCompiledClasses() = the<JavaPluginConvention>().sourceSets.getByName("main").output
fun Project.getSources() = the<JavaPluginConvention>().sourceSets.getByName("main").allSource
fun Project.getResourceFiles() = the<JavaPluginConvention>().sourceSets.getByName("main").resources


private fun Project.configureKotlinProjectSourceSet(srcs: Iterable<File>, sourceSetName: String, getSources: SourceSet.() -> SourceDirectorySet) =
        configure<JavaPluginConvention> {
            sourceSets.getByName(sourceSetName).apply {
                getSources().setSrcDirs(srcs)
            }
        }

private fun Project.configureKotlinProjectSourceSet(vararg srcs: String, sourceSetName: String, getSources: SourceSet.() -> SourceDirectorySet, sourcesBaseDir: File? = null) =
        configureKotlinProjectSourceSet(srcs.map { File(sourcesBaseDir ?: projectDir, it) }, sourceSetName, getSources)

fun Project.configureKotlinProjectSources(vararg srcs: String, sourcesBaseDir: File? = null) =
        configureKotlinProjectSourceSet(*srcs, sourceSetName = "main", getSources = { this.getJava() }, sourcesBaseDir = sourcesBaseDir)

fun Project.configureKotlinProjectSources(srcs: Iterable<File>) =
        configureKotlinProjectSourceSet(srcs, sourceSetName = "main", getSources = { this.getJava() })

fun Project.configureKotlinProjectSourcesDefault(sourcesBaseDir: File? = null) = configureKotlinProjectSources("src", sourcesBaseDir = sourcesBaseDir)

fun Project.configureKotlinProjectResources(vararg srcs: String, sourcesBaseDir: File? = null) =
        configureKotlinProjectSourceSet(*srcs, sourceSetName = "main", getSources = { this.getResources() }, sourcesBaseDir = sourcesBaseDir)

fun Project.configureKotlinProjectResources(srcs: Iterable<File>) =
        configureKotlinProjectSourceSet(srcs, sourceSetName = "main", getSources = { this.getResources() })

fun Project.configureKotlinProjectNoTests() {
    configureKotlinProjectSourceSet(sourceSetName = "test", getSources = { this.getJava() })
    configureKotlinProjectSourceSet(sourceSetName = "test", getSources = { this.getResources() })
}

