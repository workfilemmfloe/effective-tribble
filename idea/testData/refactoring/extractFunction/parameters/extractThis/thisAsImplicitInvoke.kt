// PARAM_DESCRIPTOR: public fun (kotlin.String.() -> kotlin.Unit).foo(a: kotlin.String): kotlin.Unit defined in root package
// PARAM_DESCRIPTOR: value-parameter a: kotlin.String defined in foo
// PARAM_TYPES: kotlin.String.() -> kotlin.Unit
// PARAM_TYPES: kotlin.String

fun (String.() -> Unit).foo(a: String) {
    <selection>a()</selection>
}