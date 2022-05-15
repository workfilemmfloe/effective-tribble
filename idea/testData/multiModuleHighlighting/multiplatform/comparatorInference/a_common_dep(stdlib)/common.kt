package foo

public fun test(x: Comparator<in Int>) {
    x.hashCode()
}
