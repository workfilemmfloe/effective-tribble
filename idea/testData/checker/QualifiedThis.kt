class Dup {
  fun Dup() : Unit {
    this<error>@Dup</error>
  }
}

class A() {
  fun foo() : Unit {
    this@A
    this<error>@a</error>
    this
  }

  val x = this@A.foo()
  val y = this.foo()
  val z = foo()
}

fun foo1() : Unit {
  <error>this</error>
  this<error>@a</error>
}

