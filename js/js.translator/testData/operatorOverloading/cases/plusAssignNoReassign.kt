package foo

class A() {

    var message = ""
    operator fun plusAssign(other: A) {
        message = message + "!"
    }

}

fun box(): Boolean {
    var c = A()
    c += A()
    c += A()
    return c.message == "!!"
}