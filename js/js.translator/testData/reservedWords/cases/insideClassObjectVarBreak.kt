package foo

// NOTE THIS FILE IS AUTO-GENERATED by the generateTestDataForReservedWords.kt. DO NOT EDIT!

class TestClass {
    companion object {
        var `break`: Int = 0

        fun test() {
            testNotRenamed("break", { `break` })
        }
    }
}

fun box(): String {
    TestClass.test()

    return "OK"
}