import kotlinApi.*

internal class C {
    fun foo(): Int {
        "a".extensionProperty = 1
        return "b".extensionProperty
    }
}