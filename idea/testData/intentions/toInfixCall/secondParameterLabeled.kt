// IS_APPLICABLE: false
class Foo {
    infix fun foo(x: Int = 0, y: Int = 0) {
    }
}

fun bar(baz: Foo) {
    baz.<caret>foo(y = 1)
}
