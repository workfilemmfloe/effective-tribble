fun doSomething<T>(a: T) {}

fun test() {
    class Test {
        fun unaryPlus(): Test = Test()
        fun plus(a: Test): Test = Test()
        fun unaryMinus(): Test = Test()
    }
    val test = Test()
    doSomething((-((test + test).unaryPl<caret>us())).toString())
}
