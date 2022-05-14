import kotlin.platform.platformStatic

object A {

    val b: String = "OK"

    platformStatic fun test1() : String {
        return b
    }

    platformStatic fun test2() : String {
        return test1()
    }

    fun test3(): String {
        return "1".test5()
    }

    platformStatic fun test4(): String {
        return "1".test5()
    }

    platformStatic fun String.test5() : String {
        return this + b
    }
}

fun box(): String {
    if (A.(A::test1)() != "OK") return "fail 1"

    if (A.(A::test2)() != "OK") return "fail 2"

    if (A.(A::test3)() != "1OK") return "fail 3"

    if (A.(A::test4)() != "1OK") return "fail 4"

    return "OK"
}