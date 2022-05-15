//fun foo() {
//    val x: String? = null
//    while (true) {
//        println(x ?: break)
//    }
//    x.length
//}


data class Box(val value: String?)

fun bar() {
    val b: Box? = Box(null)
    while (true) {
        println(b!!.value ?: break)
    }
    val nnBox: Box = b
    val nnValue: String = b.value
}