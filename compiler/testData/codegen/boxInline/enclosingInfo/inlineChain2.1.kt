import test.*

fun box(): String {
    val res = call {
        test { "OK" }
    }

    var enclosingMethod = res.javaClass.enclosingMethod
    if (enclosingMethod?.name != "invoke") return "fail 1: ${enclosingMethod?.name}"

    var enclosingClass = res.javaClass.enclosingClass
    if (enclosingClass?.name != "InlineChain2_1Kt\$box$\$inlined\$call$1") return "fail 2: ${enclosingClass?.name}"

    val res2 = call {
        call {
            test { "OK" }
        }
    }

    enclosingMethod = res2.javaClass.enclosingMethod
    if (enclosingMethod?.name != "invoke") return "fail 1: ${enclosingMethod?.name}"

    enclosingClass = res2.javaClass.enclosingClass
    if (enclosingClass?.name != "InlineChain2_1Kt\$box$\$inlined\$call\$lambda\$lambda$2") return "fail 2: ${enclosingClass?.name}"

    return res2()
}
