package foo

// NOTE THIS FILE IS AUTO-GENERATED by the generateTestDataForReservedWords.kt. DO NOT EDIT!

class TestClass {
    companion object {
        var enum: Int = 0

        fun test() {
            testNotRenamed("enum", { enum })
        }
    }
}

fun box(): String {
    TestClass.test()

    return "OK"
}