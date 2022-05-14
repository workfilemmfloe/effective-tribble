// ERROR: Overload resolution ambiguity:  public constructor C(arg1: kotlin.Int, arg2: kotlin.Int) defined in C kotlin.jvm.overloads public constructor C(arg1: kotlin.Int, arg2: kotlin.Int = ..., arg3: kotlin.Int = ...) defined in C
class C [overloads] (arg1: Int, arg2: Int = 0, arg3: Int = 0) {
    private val field: Int

    init {
        var arg1 = arg1
        var arg3 = arg3
        arg1++
        print(arg1 + arg2)
        field = arg3
        arg3++
    }

    constructor(arg1: Int, arg2: Int) : this(arg1, arg2, 0) {
        var arg2 = arg2
        arg2++
    }
}

public object User {
    public fun main() {
        val c1 = C(100, 100, 100)
        val c2 = C(100, 100)
        val c3 = C(100)
    }
}