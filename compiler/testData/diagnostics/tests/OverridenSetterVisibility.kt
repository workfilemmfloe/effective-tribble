public interface ITest {
    public var prop : Int
        get() = 12
        set(value) {}
}

abstract class ATest {
    protected open var prop2 : Int
        get() = 13
        set(value) {}
}

class Test: ATest(), ITest {
    override var prop : Int
        get() = 12
        <!CANNOT_WEAKEN_ACCESS_PRIVILEGE, PRIVATE_SETTER_FOR_OPEN_PROPERTY!>private<!> set(value) {}

    override var prop2 : Int
        get() = 14
        <!CANNOT_CHANGE_ACCESS_PRIVILEGE!>internal<!> set(value) {}
}

fun main(args: Array<String>) {
    val test = Test()
    <!INVISIBLE_SETTER!>test.prop<!> = 12

    val itest: ITest = test
    itest.prop = 12 // No error here
}
