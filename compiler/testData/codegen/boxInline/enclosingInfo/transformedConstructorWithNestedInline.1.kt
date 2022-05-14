import test.*

fun box(): String {
    /*This captured parameter would be added to object constructor*/
    val captured = "OK";
    var z: Any = "fail"
    val res = test {

        call {
            z = {
                captured
            }
        }

        (z as Function0<String>)()
    }


    val enclosingConstructor = z.javaClass.enclosingConstructor
    if (enclosingConstructor?.name != "TransformedConstructorWithNestedInline_1Kt\$box$\$inlined\$test$1") return "fail 1: ${enclosingConstructor?.name}"

    val enclosingClass = z.javaClass.enclosingClass
    if (enclosingClass?.name != "TransformedConstructorWithNestedInline_1Kt\$box$\$inlined\$test$1") return "fail 2: ${enclosingClass?.name}"

    return res.a()
}
