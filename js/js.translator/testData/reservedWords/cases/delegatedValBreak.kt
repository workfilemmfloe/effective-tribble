package foo

// NOTE THIS FILE IS AUTO-GENERATED by the generateTestDataForReservedWords.kt. DO NOT EDIT!

interface Trait {
    val `break`: Int
}

class TraitImpl : Trait {
    override val `break`: Int = 0
}

class TestDelegate : Trait by TraitImpl() {
    fun test() {
        testNotRenamed("break", { `break` })
    }
}

fun box(): String {
    TestDelegate().test()

    return "OK"
}