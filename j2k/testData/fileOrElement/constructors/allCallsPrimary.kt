package pack

class C(arg1: Int, arg2: Int = 0, arg3: Int = 0)

public class User {
    companion object {
        public fun main() {
            val c1 = C(100, 100, 100)
            val c2 = C(100, 100)
            val c3 = C(100)
        }
    }
}