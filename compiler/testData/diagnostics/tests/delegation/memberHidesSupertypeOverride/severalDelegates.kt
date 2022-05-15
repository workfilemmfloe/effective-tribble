interface Base1 {
    fun test() = "OK"
}

interface Base2 {
    fun test2() = "OK"
}


class Delegate1 : Base1

class Delegate2 : Base2


public abstract class MyClass : Base1, Base2 {
    override fun test(): String {
        return "Class"
    }

    override fun test2(): String {
        return "Class"
    }
}

<!MANY_IMPL_MEMBER_NOT_IMPLEMENTED, DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE, DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE!>class A<!> : MyClass(), Base1 by Delegate1(), <!SUPERTYPE_APPEARS_TWICE!>Base1<!> by <!TYPE_MISMATCH!>Delegate2()<!> {

}