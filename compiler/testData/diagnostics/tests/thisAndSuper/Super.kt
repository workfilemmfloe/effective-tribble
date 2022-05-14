package example;

trait T {
    fun foo() {}
}
open class C() {
    fun bar() {}
}

class A<E>() : C(), T {

    fun test() {
        <!SUPER_IS_NOT_AN_EXPRESSION!>super<!>
        <!SUPER_IS_NOT_AN_EXPRESSION!>super<T><!>
        <!AMBIGUOUS_SUPER!>super<!>.foo()
        super<T>.foo()
        super<C>.bar()
        super<T>@A.foo()
        super<C>@A.bar()
        super<<!NOT_A_SUPERTYPE!>E<!>>.bar()
        super<<!NOT_A_SUPERTYPE!>E<!>>@A.bar()
        super<<!NOT_A_SUPERTYPE!>Int<!>>.foo()
        super<<!SYNTAX!><!>>.foo()
        super<<!NOT_A_SUPERTYPE!>() -> Unit<!>>.foo()
        super<<!NOT_A_SUPERTYPE!>Unit<!>>.foo()
        <!DEBUG_INFO_MISSING_UNRESOLVED!>super<!><T><!UNRESOLVED_REFERENCE!>@B<!>.foo()
        <!DEBUG_INFO_MISSING_UNRESOLVED!>super<!><C><!UNRESOLVED_REFERENCE!>@B<!>.bar()
    }

    class B : T {
        fun test() {
            super<T>.foo();
            super<<!NOT_A_SUPERTYPE!>C<!>>.bar()
            super<C>@A.bar()
            super<T>@A.foo()
            super<T>@B.foo()
            super<<!NOT_A_SUPERTYPE!>C<!>>@B.foo()
            super.foo()
            <!SUPER_IS_NOT_AN_EXPRESSION!>super<!>
            <!SUPER_IS_NOT_AN_EXPRESSION!>super<T><!>
        }
    }
}

trait G<T> {
    fun foo() {}
}

class CG : G<Int> {
    fun test() {
        super<G>.foo() // OK
        super<G<!TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER!><Int><!>>.foo() // Warning
        super<<!NOT_A_SUPERTYPE!>G<<!UNRESOLVED_REFERENCE!>E<!>><!>>.foo() // Error
        super<<!NOT_A_SUPERTYPE!>G<String><!>>.foo() // Error
    }
}

// The case when no supertype is resolved
class ERROR<E>() : <!UNRESOLVED_REFERENCE!>UR<!> {

    fun test() {
        super.<!UNRESOLVED_REFERENCE!>foo<!>()
    }
}