// CHOOSE_USE_SITE_TARGET: param
// WITH_RUNTIME
// IS_APPLICABLE: false

annotation class A

class Property {
    @A<caret>
    val foo: String by lazy { "" }
}