fun test() {
    MyClass().test1
    MyClass().<info descr="'setter for test1' is deprecated. Use A instead">test1</info> = 0

    MyClass().<info descr="'setter for test1' is deprecated. Use A instead">test1</info>++
    MyClass().<info descr="'setter for test1' is deprecated. Use A instead">test1</info>--

    ++MyClass().<info descr="'setter for test1' is deprecated. Use A instead">test1</info>
    --MyClass().<info descr="'setter for test1' is deprecated. Use A instead">test1</info>

    MyClass().<info descr="'setter for test1' is deprecated. Use A instead">test1</info> += 1
    MyClass().<info descr="'setter for test1' is deprecated. Use A instead">test1</info> -= 1
    MyClass().<info descr="'setter for test1' is deprecated. Use A instead">test1</info> /= 1
    MyClass().<info descr="'setter for test1' is deprecated. Use A instead">test1</info> *= 1

    test2
    <info descr="'setter for test2' is deprecated. Use A instead">test2</info> = 10
}

class MyClass() {
    <info>public</info> var <info>test1</info>: Int = 0
      [deprecated("Use A instead")] <info>set</info>
}

<info>public</info> var <info>test2</info>: Int = 0
      [deprecated("Use A instead")] <info>set</info>