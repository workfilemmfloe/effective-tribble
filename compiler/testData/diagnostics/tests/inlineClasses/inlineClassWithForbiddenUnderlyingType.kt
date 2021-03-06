// FIR_IDENTICAL
// !LANGUAGE: +InlineClasses, -JvmInlineValueClasses

inline class Foo<T>(val x: <!VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>T<!>)
inline class FooNullable<T>(val x: <!VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>T?<!>)

inline class FooGenericArray<T>(val x: <!VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>Array<T><!>)
inline class FooGenericArray2<T>(val x: <!VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>Array<Array<T>><!>)

inline class FooStarProjectedArray(val x: Array<*>)
inline class FooStarProjectedArray2(val x: Array<Array<*>>)

inline class Bar(val u: <!VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>Unit<!>)
inline class BarNullable(val u: Unit?)

inline class Baz(val u: <!VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>Nothing<!>)
inline class BazNullable(val u: Nothing?)
