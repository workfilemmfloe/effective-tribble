// IS_APPLICABLE: false
fun doSomething<T>(a: T) {}

fun foo() {
    val a = true
    val b = false
    if (!(a <caret>&& b)) {
        doSomething("test")
    }
}
