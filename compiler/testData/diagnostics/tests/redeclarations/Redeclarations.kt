// FILE: f.kt
package redeclarations
  object <!REDECLARATION, REDECLARATION!>A<!> {
    val x : Int = 0

    val A = 1
  }

  class <!REDECLARATION!>A<!> {}

  val <!REDECLARATION, REDECLARATION!>A<!> = 1

// FILE: f.kt
  package redeclarations.<!REDECLARATION!>A<!>
    class A {}
