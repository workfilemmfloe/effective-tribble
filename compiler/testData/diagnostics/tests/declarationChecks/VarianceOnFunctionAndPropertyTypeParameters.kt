fun <<!VARIANCE_ON_TYPE_PARAMETER_OF_FUNCTION_OR_PROPERTY!>in<!> T> f() {
    
}

fun <<!VARIANCE_ON_TYPE_PARAMETER_OF_FUNCTION_OR_PROPERTY!>out<!> T> g() {

}

fun <<!VARIANCE_ON_TYPE_PARAMETER_OF_FUNCTION_OR_PROPERTY!>out<!> T, <!VARIANCE_ON_TYPE_PARAMETER_OF_FUNCTION_OR_PROPERTY!>in<!> X, Y> h() {

}

val <<!VARIANCE_ON_TYPE_PARAMETER_OF_FUNCTION_OR_PROPERTY!>out<!> T> T.x: Int
    get() = 1

val <<!VARIANCE_ON_TYPE_PARAMETER_OF_FUNCTION_OR_PROPERTY!>in<!> T> T.y: Int
    get() = 1