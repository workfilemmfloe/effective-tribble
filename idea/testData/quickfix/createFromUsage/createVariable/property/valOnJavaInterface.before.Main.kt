// "Create property 'foo'" "false"
// ACTION: Convert to expression body
// ACTION: Create extension property 'foo'
// ERROR: Unresolved reference: foo

internal fun test(a: A): String? {
    return a.<caret>foo
}