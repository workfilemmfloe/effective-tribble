// "Remove 'in' modifier" "true"
class Foo<out T> {}

fun bar(x : Foo<<caret>in Any>) {}
