// PARAM_TYPES: Z
class Z(val a: Int)

// SIBLING:
fun Z.foo(): Int {
    return <selection>this.a</selection> + 1
}
