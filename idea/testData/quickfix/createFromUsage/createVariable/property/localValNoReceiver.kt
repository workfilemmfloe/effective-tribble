// "Create property 'foo'" "false"
// ACTION: Convert to expression body
// ACTION: Create parameter 'foo'
// ACTION: Create local variable 'foo'
// ERROR: Unresolved reference: foo

fun test() {
    fun nestedTest(): Int {
        return <caret>foo
    }
}
