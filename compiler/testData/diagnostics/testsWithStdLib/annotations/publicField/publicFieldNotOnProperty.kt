// !DIAGNOSTICS: -UNUSED_PARAMETER
class C {

    <!WRONG_ANNOTATION_TARGET!>@kotlin.jvm.publicField<!> constructor(s: String) {

    }

    <!WRONG_ANNOTATION_TARGET!>@kotlin.jvm.publicField<!> private fun foo(s: String = "OK") {

    }
}

<!WRONG_ANNOTATION_TARGET!>@kotlin.jvm.publicField<!>
fun foo() {
    <!WRONG_ANNOTATION_TARGET!>@kotlin.jvm.publicField<!> val <!UNUSED_VARIABLE!>x<!> = "A"
}