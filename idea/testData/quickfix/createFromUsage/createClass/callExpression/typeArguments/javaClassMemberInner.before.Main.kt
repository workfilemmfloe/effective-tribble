// "Create class 'Foo'" "true"
// ERROR: Unresolved reference: Foo

class A<T> internal constructor(val b: B<T>) {
    internal fun test() = b.<caret>Foo<Int, String>(2, "2")
}