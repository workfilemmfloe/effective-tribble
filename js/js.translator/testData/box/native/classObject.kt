package foo

@native
class A(val c: Int) {
    @native
    companion object {
        val g: Int = noImpl
        val c: String = noImpl
    }
}

fun box(): String {
    if (A.g != 3) return "fail1"
    if (A.c != "hoooray") return "fail2"
    if (A(2).c != 2) return "fail3"

    return "OK"
}
