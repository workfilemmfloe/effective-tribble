//NO_CHECK_LAMBDA_INLINING
fun test(): String = fun (): String {
    foo { return "OK" }
    return "fail"
} ()

fun test2(): String = (l@ fun (): String {
    foo { return@l "OK" }
    return "fail"
}) ()

fun test3(): String = (l@ fun bar(): String {
    foo { return@bar "OK" }
    return "fail"
}) ()

fun box(): String {
    if (test() != "OK") return "fail 1: ${test()}"

    if (test2() != "OK") return "fail 2: ${test2()}"

    return test3()
}