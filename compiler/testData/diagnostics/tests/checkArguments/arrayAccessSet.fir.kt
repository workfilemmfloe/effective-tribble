// !DIAGNOSTICS: -UNUSED_PARAMETER

object A {
    operator fun set(x: Int, y: String = "y", z: Double) {
    }
}

object B {
    operator fun set(x: Int, y: String = "y", z: Double = 3.14, w: Char = 'w', v: Boolean) {
    }
}

object D {
    operator fun set(x: Int, vararg y: String, z: Double) {
    }
}

object Z {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun set() {
    }
}

fun test() {
    A[0] = ""
    A[0] = 2.72

    B[0] = ""
    B[0] = 2.72
    B[0] = true

    D[0] = ""
    D[0] = 2.72

    Z[<!TOO_MANY_ARGUMENTS!>0<!>] = <!TOO_MANY_ARGUMENTS!>""<!>
}
