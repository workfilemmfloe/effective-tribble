fun test() {
    class Test{
        operator fun contains(fn: () -> Boolean) : Boolean = true
    }
    val test = Test()
    test.c<caret>ontains {
        true
    }
}
