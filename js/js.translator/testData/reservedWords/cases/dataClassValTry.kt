package foo

// NOTE THIS FILE IS AUTO-GENERATED by the generateTestDataForReservedWords.kt. DO NOT EDIT!

data class DataClass(val `try`: String) {
    init {
        testNotRenamed("try", { `try` })
    }
}

fun box(): String {
    DataClass("123")

    return "OK"
}