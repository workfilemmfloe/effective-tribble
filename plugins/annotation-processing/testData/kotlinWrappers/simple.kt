// test.Simple

package test

abstract class Simple(private val prop: String) {
    protected val anotherProp: Int = 5
    
    abstract fun strFun(val x: Long): String
    fun voidFun() {}
}