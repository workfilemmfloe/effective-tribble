// "Create header class implementation for platform JS" "true"

header enum class <caret>MyEnum {
    FIRST,
    SECOND,
    LAST;

    val num: Int

    companion object {
        fun byNum(num: Int): MyEnum = when (num) {
            1 -> FIRST
            2 -> SECOND
            else -> LAST
        }
    }
}