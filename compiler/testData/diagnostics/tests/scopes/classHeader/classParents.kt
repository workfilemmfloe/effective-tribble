// !DIAGNOSTICS: -UNUSED_PARAMETER

interface I<F, G, H>

class A(impl: Interface) : <!UNRESOLVED_REFERENCE!>Nested<!>(), <!UNRESOLVED_REFERENCE, DELEGATION_NOT_TO_INTERFACE!>Interface<!> by impl, <!UNRESOLVED_REFERENCE!>Inner<!>, I<<!UNRESOLVED_REFERENCE!>Nested<!>, <!UNRESOLVED_REFERENCE!>Interface<!>, <!UNRESOLVED_REFERENCE!>Inner<!>> {

    class Nested

    inner class Inner

    interface Interface
}
