internal class Test {
    fun printNumbers(number: Int) {
        for (i in 2..Math.sqrt(number.toDouble()) + 1 - 1)
            println(i)
    }
}