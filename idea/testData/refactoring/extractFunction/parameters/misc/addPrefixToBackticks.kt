// PARAM_TYPES: X
// PARAM_TYPES: Y
// PARAM_DESCRIPTOR: internal final fun X.test(): kotlin.Unit defined in Y
// PARAM_DESCRIPTOR: internal final class Y defined in root package
class X(val x: Int)

// SIBLING:
class Y(val y: Int) {
    fun X.test() {
        <selection>`x`plus y</selection>
    }
}