// WITH_RUNTIME
class Foo(val id: Int, val name: String)

operator fun Foo.component1() = id
operator fun Foo.component2() = name
operator fun Foo.component3() = "$name: $id"

fun test() {
    listOf(Foo(123, "def"), Foo(456, "abc"))<caret>
}