// WITH_RUNTIME
fun main(args: Array<String>){
    val x = mapOf("a" to "b")
    val y = "abcd" +<caret> x["a"]
}
