package foo

// NOTE THIS FILE IS AUTO-GENERATED by the generateTestDataForReservedWords.kt. DO NOT EDIT!

object TestObject {
    fun public() { public() }

    fun test() {
        testNotRenamed("public", { ::public })
    }
}

fun box(): String {
    TestObject.test()

    return "OK"
}