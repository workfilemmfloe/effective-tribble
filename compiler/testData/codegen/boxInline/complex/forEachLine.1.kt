import test.*
import java.util.*


fun sample(): Input {
    return Input("Hello", "World");
}

test fun testForEachLine() {
    val list = ArrayList<String>()
    val reader = sample()

    reader.forEachLine{
        list.add(it)
    }
}


fun box(): String {
    testForEachLine()

    return "OK"
}
