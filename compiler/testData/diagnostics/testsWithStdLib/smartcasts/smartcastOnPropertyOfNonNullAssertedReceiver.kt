class Box(val value: String?)

fun test() {
    val b: Box? = Box(null)
    if (b!!.value != null) {
        val nnBox: Box = b
        val nnValue: String = b.value // 'b' is smartcasted to not-null, but `value` isn't
    }
}