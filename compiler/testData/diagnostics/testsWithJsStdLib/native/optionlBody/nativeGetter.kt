// !DIAGNOSTICS: -DEPRECATION
@nativeGetter
fun String.foo(n: Int): Int?
@nativeGetter
fun String.bar(n: Int): Int? = noImpl


external interface T {
    @nativeGetter
    fun foo(d: Double): String?
    @nativeGetter
    fun bar(d: Double): String? = noImpl
}