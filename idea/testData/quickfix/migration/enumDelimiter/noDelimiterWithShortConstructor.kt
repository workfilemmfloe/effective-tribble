// "Insert lacking comma(s) / semicolon(s)" "true"

enum class MyEnum(val z: Int) {
    A(3)  B<caret>(7)  C(12)
    fun foo() = z * 2
}