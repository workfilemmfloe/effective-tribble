import test.*

fun box(): String {
    var encl1 = "fail";
    var encl2 = "fail";
    test {
        {
            val p = object {}
            encl1 = p.javaClass.enclosingMethod.declaringClass.name
            {

                val p = object {}
                encl2 = p.javaClass.enclosingMethod.declaringClass.name
            }()
        }()
    }

    if (encl1 != "Kt10259_1Kt\$box\$\$inlined\$test\$lambda$1") return "fail 1: $encl1"
    if (encl2 != "Kt10259_1Kt\$box\$\$inlined\$test\$lambda\$lambda$2") return "fail 2: $encl2"

    return "OK"
}
