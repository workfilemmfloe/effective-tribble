class A() {
  fun equals(<warning>a</warning> : Any?) : Boolean = false
}

class B() {
  fun equals(<warning>a</warning> : Any?) : Boolean? = false
}

class C() {
  fun equals(<warning>a</warning> : Any?) : Int = 0
}

fun f(): Unit {
  var x: Int? = <warning>1</warning>
  x = 1
  x <error>+</error> 1
  x <error>plus</error> 1
  x <error><</error> 1
  x <error>+=</error> 1

  x == 1
  x != 1

  <error>A() == 1</error>
  B() <error>==</error> 1
  C() <error>==</error> 1

  <error>x === "1"</error>
  <error>x !== "1"</error>

  x === 1
  x !== 1

  x<error>..</error>2
  <error>x</error> in 1..2

  val y : Boolean? = true
  false || <error>y</error>
  <error>y</error> && true
  <error>y</error> && <error>1</error>
}