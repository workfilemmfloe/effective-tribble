// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetClass
// OPTIONS: derivedInterfaces
trait X {

}

open class <caret>A: X {

}

open class C: Y {

}

trait Z: A {

}
