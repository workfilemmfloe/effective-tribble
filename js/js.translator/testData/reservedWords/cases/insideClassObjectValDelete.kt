package foo

// NOTE THIS FILE IS AUTO-GENERATED by the generateTestDataForReservedWords.kt. DO NOT EDIT!

class TestClass {
    companion object {
        val delete: Int = 0

        fun test() {
            testNotRenamed("delete", { delete })
        }
    }
}

fun box(): String {
    TestClass.test()

    return "OK"
}