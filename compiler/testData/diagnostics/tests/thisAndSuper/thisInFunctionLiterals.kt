class A(val a:Int) {
  class B() {
    fun Char.xx() : Any {
      this : Char
      val <!UNUSED_VARIABLE!>a<!> = {
        Double.() ->
        this : Double
        this@xx : Char
        this@B : B
        this@A : A
      }
      val <!UNUSED_VARIABLE!>b<!> = @a{Double.() -> this@a : Double + this@xx : Char}
      val <!UNUSED_VARIABLE!>c<!> = @a{() -> <!NO_THIS!>this@a<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>+<!> this@xx : Char}
      return (@a{Double.() -> this@a : Double + this@xx : Char})
    }
  }
}