// PARAM_DESCRIPTOR: value-parameter unaryPlus: kotlin.String.() -> kotlin.Unit defined in foo
// PARAM_TYPES: kotlin.String.() -> kotlin.Unit

fun foo(unaryPlus: String.() -> Unit) {
    <selection>+</selection> "A"
}
