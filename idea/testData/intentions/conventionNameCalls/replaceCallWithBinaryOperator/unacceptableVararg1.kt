// IS_APPLICABLE: false
fun test() {
    class Test{
        operator fun plus(vararg b: Int, c: Int = 0): Int = 0
    }
    val test = Test()
    test.plus<caret>(c=5)
}
