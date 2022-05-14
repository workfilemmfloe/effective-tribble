import kotlin.reflect.KProperty

class Delegate {
    var inner = 1
    fun getValue(t: Any?, p: KProperty<*>): Int = inner
}

class A {
  fun Delegate.setValue(t: Any?, p: KProperty<*>, i: Int) { inner = i }

  var prop: Int by Delegate()
}

fun box(): String {
  val c = A()
  if(c.prop != 1) return "fail get"
  c.prop = 2
  if (c.prop != 2) return "fail set"
  return "OK"
}
