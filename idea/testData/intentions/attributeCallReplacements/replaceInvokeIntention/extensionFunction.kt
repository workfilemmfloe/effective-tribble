fun test() {
    class Test()

    fun Test.invoke(): Unit = Unit

    val test = Test()
    test.i<caret>nvoke()
}
