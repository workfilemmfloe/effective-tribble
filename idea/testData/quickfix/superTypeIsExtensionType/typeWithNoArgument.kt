// "Convert supertype to '(String) -> Unit'" "true"

class Foo : <caret>String.() -> Unit {
    override fun invoke(p1: String) {
    }
}