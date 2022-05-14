fun check(expected: String, obj: Any?) {
    val actual = obj.toString()
    if (actual != expected)
        throw AssertionError("Expected: $expected, actual: $actual")
}


fun box(): String {
    check("kotlin.FunctionImpl0<kotlin.Unit>")
        { () : Unit -> }
    check("kotlin.FunctionImpl0<java.lang.Integer>")
        { () : Int -> 42 }
    check("kotlin.FunctionImpl1<java.lang.String, java.lang.Long>")
        { (s: String) : Long -> 42.toLong() }
    check("kotlin.FunctionImpl2<java.lang.Integer, java.lang.Integer, kotlin.Unit>")
        { (x: Int, y: Int) : Unit -> }

    check("kotlin.ExtensionFunctionImpl0<java.lang.Integer, kotlin.Unit>")
        { Int.() : Unit -> }
    check("kotlin.ExtensionFunctionImpl0<kotlin.Unit, java.lang.Integer>")
        { Unit.() : Int -> 42 }
    check("kotlin.ExtensionFunctionImpl1<java.lang.String, java.lang.String, java.lang.Long>")
        { String.(s: String) : Long -> 42.toLong() }
    check("kotlin.ExtensionFunctionImpl2<java.lang.Integer, java.lang.Integer, java.lang.Integer, kotlin.Unit>")
        { Int.(x: Int, y: Int) : Unit -> }

    return "OK"
}
