package foo

// NOTE THIS FILE IS AUTO-GENERATED by the generateTestDataForReservedWords.kt. DO NOT EDIT!

class TestClass {
    var NaN: Int = 0

    fun test() {
        testNotRenamed("NaN", { NaN })
    }
}

fun box(): String {
    TestClass().test()

    return "OK"
}