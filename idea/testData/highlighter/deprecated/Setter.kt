fun test() {
    MyClass().test1
    MyClass().<warning descr="'setter for test1' is deprecated. Use A instead">test1</warning> = 0

    MyClass().<warning descr="'setter for test1' is deprecated. Use A instead">test1</warning>++
    MyClass().<warning descr="'setter for test1' is deprecated. Use A instead">test1</warning>--

    ++MyClass().<warning descr="'setter for test1' is deprecated. Use A instead">test1</warning>
    --MyClass().<warning descr="'setter for test1' is deprecated. Use A instead">test1</warning>

    MyClass().<warning descr="'setter for test1' is deprecated. Use A instead">test1</warning> += 1
    MyClass().<warning descr="'setter for test1' is deprecated. Use A instead">test1</warning> -= 1
    MyClass().<warning descr="'setter for test1' is deprecated. Use A instead">test1</warning> /= 1
    MyClass().<warning descr="'setter for test1' is deprecated. Use A instead">test1</warning> *= 1

    test2 + 1
    <warning descr="'setter for test2' is deprecated. Use A instead">test2</warning> = 10
}

class MyClass() {
    public var test1: Int = 0
      [deprecated("Use A instead")] set
}

public var test2: Int = 0
      [deprecated("Use A instead")] set