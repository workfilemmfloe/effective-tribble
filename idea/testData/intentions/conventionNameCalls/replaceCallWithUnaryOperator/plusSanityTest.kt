// INTENTION_TEXT: Replace with '+' operator
fun test() {
    class Test {
        operator fun unaryPlus(): Test = Test()
    }
    val test = Test()
    test.unaryPl<caret>us()
}
