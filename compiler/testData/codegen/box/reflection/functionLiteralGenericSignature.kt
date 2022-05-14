import java.util.Date

fun assertGenericSuper(expected: String, function: Any?) {
    val clazz = (function as java.lang.Object).getClass()!!
    val genericSuper = clazz.getGenericSuperclass()!!
    if ("$genericSuper" != expected)
        throw AssertionError("Fail, expected: $expected, actual: $genericSuper")
}


val unitFun = { }
val intFun = { 42 }
val stringParamFun = { (x: String) : Unit -> }
val listFun = { (l: List<String>) : List<String> -> l }
val mutableListFun = { (l: MutableList<Double>) : MutableList<Int> -> null!! }
val funWithIn = { (x: Comparable<String>) : Unit -> }

val extensionFun = { Any.() : Unit -> }
val extensionWithArgFun = { Long.(x: Any) : Date -> Date() }

fun box(): String {
    assertGenericSuper("jet.FunctionImpl0<jet.Unit>", unitFun)
    assertGenericSuper("jet.FunctionImpl0<java.lang.Integer>", intFun)
    assertGenericSuper("jet.FunctionImpl1<java.lang.String, jet.Unit>", stringParamFun)
    assertGenericSuper("jet.FunctionImpl1<java.util.List<? extends java.lang.String>, java.util.List<? extends java.lang.String>>", listFun)
    assertGenericSuper("jet.FunctionImpl1<java.util.List<java.lang.Double>, java.util.List<java.lang.Integer>>", mutableListFun)
    assertGenericSuper("jet.FunctionImpl1<java.lang.Comparable<? super java.lang.String>, jet.Unit>", funWithIn)

    assertGenericSuper("jet.ExtensionFunctionImpl0<java.lang.Object, jet.Unit>", extensionFun)
    assertGenericSuper("jet.ExtensionFunctionImpl1<java.lang.Long, java.lang.Object, java.util.Date>", extensionWithArgFun)

    return "OK"
}
