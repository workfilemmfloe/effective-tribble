fun foo(s: String?, b1: Boolean, b2: Boolean) {
    x(
            if (b1) {
                print(s!!.length)
                1
            } else {
                if (b2) {
                    print(s!!.length + 1)
                }
                else {
                    print(s!!.length - 1)
                }
                2
            },
            <caret>s
    )
}

fun x(p1: Int, p2: String) { }