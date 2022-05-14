package kotlin.test

fun init() {
    asserter = JsTestsAsserter()
}

public class JsTestsAsserter() : Asserter {
    public override fun fail(message: String?): Nothing = failWithMessage(message)
}

public inline fun assert(value: Boolean, message: String?): Unit = js("JsTests").assert(value, message)

private inline fun failWithMessage(message: String?): Nothing = js("JsTests").fail(message)
