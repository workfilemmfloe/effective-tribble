import java.util.*

class Outer {

    private abstract class Base {

        protected sealed class Sealed {
            object OOO : Sealed()
        }

        typealias OOO = Sealed.OOO
    }

    private class Derived : Base() {

        fun foo(): Sealed {
            ArrayList<Int>()
            return OOO
        }

    }
}