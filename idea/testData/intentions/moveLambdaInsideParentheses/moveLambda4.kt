// IS_APPLICABLE: true
fun foo() {
    bar(a = 2) <caret>{
        val x = 3
        it * x
    }
}

fun bar(a: Int, b: (Int) -> Int) {
    b(a)
}
