public object C {
    JvmStatic public fun main(args: Array<String>) {
        when (args.size()) {
            1 -> {
                run {
                    val a = 1
                    print("1")
                }
                run {
                    val a = 2
                    print("2")
                }
            }

            2 -> {
                val a = 2
                print("2")
            }
        }
    }
}