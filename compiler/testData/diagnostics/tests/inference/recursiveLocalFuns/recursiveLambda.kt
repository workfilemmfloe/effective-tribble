fun foo() {
    fun bar() = {
        <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>bar()<!>
    }
}