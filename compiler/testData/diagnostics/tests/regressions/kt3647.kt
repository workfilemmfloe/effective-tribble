// KT-3647 Unexpected compilation error: "Expression is inaccessible from a nested class"

class Test(val value: Int) {
    class object {
        fun create(init: () -> Int): Test {
            return Test(init())
        }
    }
}
