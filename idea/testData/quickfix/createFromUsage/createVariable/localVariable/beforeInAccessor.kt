// "Create local variable 'foo'" "true"
// ACTION: Create parameter 'foo'
// ERROR: Variable 'foo' must be initialized

class A {
    val t: Int get() {
        return <caret>foo
    }
}