import kotlin.reflect.KClass

annotation class Ann(val i: Int = 1, val i2: IntArray = intArrayOf(1), val i3: IntArray = intArrayOf(1), val klass: KClass<*> = A::class, val klass2: Array<KClass<*>> = arrayOf(A::class), val klass3: Array<KClass<*>> = arrayOf(A::class), val ann: Inner = Inner(), val ann2: Array<Inner> = arrayOf(Inner()), val ann3: Array<Inner> = arrayOf(Inner(), Inner()))

class A
annotation class Inner
