class Foo {
    var bar: Bar? = null
}
class Bar {
    var baz = 1
}
fun test(foo: Foo?) {
    foo?<caret>.bar?.baz = 2
}