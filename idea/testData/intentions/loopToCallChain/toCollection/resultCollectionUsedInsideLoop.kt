// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filter{}.forEach{}'"
// INTENTION_TEXT_2: "Replace with 'asSequence().filter{}.forEach{}'"
import java.util.ArrayList

fun foo(list: List<String>): List<Int> {
    val result = ArrayList<Int>()
    <caret>for (s in list) {
        if (s.length > result.size) {
            result.add(s.hashCode())
        }
    }
    return result
}