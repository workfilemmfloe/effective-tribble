// !DIAGNOSTICS: -UNUSED_PARAMETER

trait Foo<T> {
    fun foo(l: List<T>) {}
}

trait Bar<T> {
    fun foo(l: List<T>) {}
}

class Baz(): Foo<String>, Bar<Int> {
    <!CONFLICTING_JVM_DECLARATIONS!>fun foo(l: List<Long>)<!> {}
}