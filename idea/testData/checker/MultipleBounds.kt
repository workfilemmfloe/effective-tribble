package Jet87

open class A() {
  fun foo() : Int = 1
}

trait B {
  fun bar() : Double = 1.0;
}

class C() : A(), B

class D() {
  class object : A(), B {}
}

class Test1<T : A>()
  where
    T : B,
    <error>B</error> : T, // error
    class object T : A,
    class object T : B,
    class object <error>B</error> : T
  {

  fun test(t : T) {
    T.foo()
    T.bar()
    t.foo()
    t.bar()
  }
}

fun test() {
  Test1<<error>B</error>>()
  Test1<<error>A</error>>()
  Test1<C>()
}

class Foo() {}

class Bar<T : <warning>Foo</warning>>

class Buzz<T> where T : <warning>Bar<<error>Int</error>></warning>, T : <error>nioho</error>

class X<T : <warning>Foo</warning>>
class Y<<error>T</error> : <warning>Foo</warning>> where T : <warning>Bar<Foo></warning>

fun <T : A> test2(t : T)
  where
    T : B,
    <error>B</error> : T,
    class object <error>B</error> : T,
    class object T : B,
    class object T : A
{
  T.foo()
  T.bar()
  t.foo()
  t.bar()
}

val t1 = test2<<error>A</error>>(A())
val t2 = test2<<error>B</error>>(C())
val t3 = test2<C>(C())

class Test<<error>T</error>>
  where
    class object T : <error>Foo</error>,
    class object T : A {}

val <T, B : T> x : Int = 0
