fun <T> doSomething(a: T) {}

fun test(n: Int): String {
    var res: String

    if (3 > 2) {
        <caret>res = when(n) {
            1 -> {
                doSomething("***")
                "one"
            }
            else -> {
                doSomething("***")
                "two"
            }
        }
    } else {
        doSomething("***")
        res = "???"
    }

    return res
}
