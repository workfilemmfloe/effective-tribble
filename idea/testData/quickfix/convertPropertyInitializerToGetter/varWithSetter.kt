// "Convert property initializer to getter" "true"

fun String.foo() = "bar"
fun nop() {

}

interface A {
    var name = <caret>"The quick brown fox jumps over the lazy dog".foo()
        set(value) = nop()
}