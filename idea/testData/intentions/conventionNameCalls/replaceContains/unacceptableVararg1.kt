// IS_APPLICABLE: false
fun test() {
    class Test{
        operator fun contains(vararg b: Int, c: Int = 0): Boolean = true
    }
    val test = Test()
    test.contains<caret>(c=5)
}
