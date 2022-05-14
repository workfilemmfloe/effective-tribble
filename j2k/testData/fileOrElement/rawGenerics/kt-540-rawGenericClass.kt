package demo

class Collection<E>(e: E) {
    init {
        println(e)
    }
}

class Test {
    fun main() {
        val raw1 = Collection(1)
        val raw2 = Collection(1)
        val raw3 = Collection("1")
    }
}