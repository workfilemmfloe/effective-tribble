// "Create property 'foo' as constructor parameter" "false"
// ACTION: Create member property 'foo'
// ACTION: Convert to expression body
// ACTION: Create extension property 'foo'
// ACTION: Rename reference
// ERROR: Unresolved reference: foo

fun test(): String? {
    return A().<caret>foo
}