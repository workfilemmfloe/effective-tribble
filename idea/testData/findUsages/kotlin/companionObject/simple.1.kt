fun main(args: Array<String>) {
    val p: Foo = Foo() // simple class usage

    // companion object usages
    Foo.f()
    val x = Foo

    Foo.Companion.f()
    val xx = Foo.Companion
}