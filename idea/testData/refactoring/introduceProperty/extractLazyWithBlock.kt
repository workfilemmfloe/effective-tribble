// WITH_RUNTIME
// EXTRACTION_TARGET: lazy property

class A(val n: Int = 1) {
    val m: Int = 2

    fun foo(): Int {
        return if (n > 1) <selection>{
            println(n)
            m + n + 1
        }</selection> else 0
    }
}

