// KT-4351 Cannot resolve reference to self in init of class local to function

fun f() {
    class MyClass() {
        {
            val <!UNUSED_VARIABLE!>x<!>: MyClass = MyClass()
        }

        fun member() {
            val <!UNUSED_VARIABLE!>x<!>: MyClass = MyClass()
        }
    }

    <!LOCAL_OBJECT_NOT_ALLOWED!>object MyObject<!> {
        {
            val <!UNUSED_VARIABLE!>obj<!>: MyObject = MyObject
        }
    }

    val <!UNUSED_VARIABLE!>x<!>: MyClass = MyClass()
}

val closure = {
    class MyClass {
        {
            val <!UNUSED_VARIABLE!>x<!>: MyClass = MyClass()
        }
    }
}
