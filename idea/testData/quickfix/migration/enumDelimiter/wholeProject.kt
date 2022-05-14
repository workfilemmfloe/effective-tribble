// "Insert lacking comma(s) / semicolon(s) in the whole project" "true"

enum class First {
    RED GREEN,
    BLUE
}

enum class Second(val code: Int) {
    NORTH(2) SOUTH(4),
    EAST(6) WEST(8)
}

enum class Third {
    OK {
        override fun diag(): String = "OK"
    }
    ERROR<caret> {
        override fun diag(): String = "Failed"
    }
    
    open fun diag(): String = ""
}
