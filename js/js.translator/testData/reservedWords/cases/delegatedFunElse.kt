package foo

// NOTE THIS FILE IS AUTO-GENERATED by the generateTestDataForReservedWords.kt. DO NOT EDIT!

interface Trait {
    fun `else`()
}

class TraitImpl : Trait {
    override fun `else`() { `else`() }
}

class TestDelegate : Trait by TraitImpl() {
    fun test() {
        testNotRenamed("else", { `else`() })
    }
}

fun box(): String {
    TestDelegate().test()

    return "OK"
}