import test.*

fun test1(): String {
    return MyEnum.K.doSmth("O")
}

fun box(): String {
    val result = test1()
    if (result != "OK") return "fail1: ${result}"

    return "OK"
}