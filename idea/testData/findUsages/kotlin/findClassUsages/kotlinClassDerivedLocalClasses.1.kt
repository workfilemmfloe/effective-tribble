// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetClass
// OPTIONS: derivedClasses

fun foo() {
    open class B: A() {

    }

    open class C: Y {

    }

    fun bar() {
        trait Z: A {

        }

        trait U: Z {

        }
    }
}