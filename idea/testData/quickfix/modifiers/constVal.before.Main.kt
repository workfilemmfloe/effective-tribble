// "Add 'const' modifier" "true"
package constVal

val i = 1

annotation class Fancy(val param: Int)

@Fancy(<caret>i) class D
