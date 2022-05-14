/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/anonymousObject/anonymousObjectOnCallSiteSuperParams.1.kt
 */

package foo

import test.*

fun box() : String {
    val o = "O"
    val result = doWork {
        val k = "K"
        val s = object : A<String>("11") {
            override fun getO(): String {
                return o;
            }

            override fun getK(): String {
                return k;
            }
        }

        s.getO() + s.getK() + s.param
    }

    if (result != "OK11") return "fail $result"

    return "OK"
}

