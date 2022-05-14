fun box(): String = "OK"

operator fun Throwable.inc(): Throwable = null!!

fun boo(): Any {
    var i = 0
    val e: Throwable = null!!
    throw e++
    return i++
}
