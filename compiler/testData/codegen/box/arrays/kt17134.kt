// WITH_STDLIB
// TARGET_BACKEND: JVM

object A {
    @JvmStatic fun main(args: Array<String>) {
        val b = arrayOf(arrayOf(""))
        object {
            val c = b[0]
        }
    }
}

fun box(): String {
    A.main(emptyArray())
    return "OK"
}