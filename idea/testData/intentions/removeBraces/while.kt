fun <T> doSomething(a: T) {}

fun foo() {
    while (true) {
        doSomething("test")
    <caret>}
}
