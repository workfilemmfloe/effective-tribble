// "Create property 'foo' from usage" "true"
// ERROR: Property must be initialized

fun test() {
    val a: Int = Unit.foo
}

val Unit.foo: Int
