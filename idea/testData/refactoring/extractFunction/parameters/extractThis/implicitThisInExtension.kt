// PARAM_TYPES: Z
// PARAM_DESCRIPTOR: public fun Z.foo(): kotlin.Int defined in root package
class Z(val a: Int)

// SIBLING:
fun Z.foo(): Int {
    return <selection>a</selection> + 1
}
