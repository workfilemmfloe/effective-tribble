import kotlin.modules.*

fun project() {
    module("kunit") {
        // TODO how to refer to the dir of the module?
        classpath += "lib/junit-4.9.jar"

        addSourceFiles("src/main/kotlin")
    }
}