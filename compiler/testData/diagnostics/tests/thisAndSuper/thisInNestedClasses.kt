class A(val a:Int) {

  class B() {
    val x = this@B : B
    val y = this@A : A
    val z = this : B
    val Int.xx : Int get() = this : Int
  }
}