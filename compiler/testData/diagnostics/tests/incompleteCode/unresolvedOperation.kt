fun foo(a: Int) {
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>!<!><!UNRESOLVED_REFERENCE!>bbb<!>
    <!UNRESOLVED_REFERENCE!>bbb<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>+<!> a
}