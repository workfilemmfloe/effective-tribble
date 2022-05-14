class My {
    val x: Int
        get() = field + if (z != "") 1 else 0

    val y: Int
        get() = field - if (z == "") 0 else 1

    val w: Int

    init {
        // Safe, val never has a setter
        x = 0
        this.y = 0
        // Unsafe
        w = this.<!DEBUG_INFO_LEAKING_THIS!>x<!> + <!DEBUG_INFO_LEAKING_THIS!>y<!>
    }

    val z = "1"
}
