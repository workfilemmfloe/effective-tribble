package foo

// NOTE THIS FILE IS AUTO-GENERATED by the generateTestDataForReservedWords.kt. DO NOT EDIT!

class TestClass {
    val yield: Int = 0

    fun test() {
        testNotRenamed("yield", { yield })
    }
}

fun box(): String {
    TestClass().test()

    return "OK"
}