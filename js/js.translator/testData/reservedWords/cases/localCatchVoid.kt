package foo

// NOTE THIS FILE IS AUTO-GENERATED by the generateTestDataForReservedWords.kt. DO NOT EDIT!

fun box(): String {
    

    
    try {
        throw Exception()
    }
    catch(void: Exception) {
        testRenamed("void", { void })
    }

    return "OK"
}