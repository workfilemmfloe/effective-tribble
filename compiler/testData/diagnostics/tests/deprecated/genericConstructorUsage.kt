// !DIAGNOSTICS: -UNUSED_EXPRESSION, -UNUSED_PARAMETER

open class C<T>() {
    @Deprecated("")
    constructor(p: Int) : this(){}
}

class D : <!DEPRECATED_SYMBOL_WITH_MESSAGE!>C<String><!>(1)