abstract class Base<C> {
    fun foo(x: Derived<C>?) = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>x?.<!DEBUG_INFO_MISSING_UNRESOLVED!>foo<!>(null)<!>
}

class Derived<C> : Base<C>()

