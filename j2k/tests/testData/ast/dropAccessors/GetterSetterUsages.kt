public class AAA {
    public var x: Int = 42

    public fun foo() {
        x = x + 1
    }

    public fun bar(other: AAA) {
        other.x = other.x + 1
    }
}
