class A

fun main() {
    val <!UNUSED_VARIABLE!>foo<!> = ::<!UNRESOLVED_REFERENCE!>foo<!>
    
    ::<!UNRESOLVED_REFERENCE!>bar<!>
    
    A::<!UNRESOLVED_REFERENCE!>bar<!>
    
    <!UNRESOLVED_REFERENCE!>B<!>::<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>bar<!>
}
