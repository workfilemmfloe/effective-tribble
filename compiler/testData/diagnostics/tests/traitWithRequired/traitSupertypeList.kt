open class bar()

interface Foo<!CONSTRUCTOR_IN_INTERFACE!>()<!> : <!INTERFACE_WITH_SUPERCLASS!>bar<!><!SUPERTYPE_INITIALIZED_IN_INTERFACE!>()<!>, <!MANY_CLASSES_IN_SUPERTYPE_LIST!>bar<!>, <!MANY_CLASSES_IN_SUPERTYPE_LIST, SUPERTYPE_APPEARS_TWICE!>bar<!> {
}

interface Foo2 : <!INTERFACE_WITH_SUPERCLASS!>bar<!>, Foo {
}

open class Foo1() : bar(), <!SUPERTYPE_NOT_INITIALIZED, MANY_CLASSES_IN_SUPERTYPE_LIST, SUPERTYPE_APPEARS_TWICE!>bar<!>, Foo, <!SUPERTYPE_APPEARS_TWICE!>Foo<!>() {}
open class Foo12 : bar(), <!SUPERTYPE_NOT_INITIALIZED, MANY_CLASSES_IN_SUPERTYPE_LIST, SUPERTYPE_APPEARS_TWICE!>bar<!> {}