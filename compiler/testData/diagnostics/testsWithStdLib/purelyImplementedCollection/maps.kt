// !DIAGNOSTICS: -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE
import java.util.*
import java.util.concurrent.*

fun bar(): String? = null
val nullableInt: Int? = null

fun hashMapTest() {
    var x: HashMap<String, Int> = HashMap<String, Int>()
    x.put(<!NULL_FOR_NONNULL_TYPE!>null<!>, <!NULL_FOR_NONNULL_TYPE!>null<!>)
    x.put("", <!NULL_FOR_NONNULL_TYPE!>null<!>)
    x.put(<!TYPE_MISMATCH!>bar()<!>, 1)
    x.put("", 1)

    <!OI;TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>x[<!NI;NULL_FOR_NONNULL_TYPE!>null<!>]<!> = 1
    <!OI;TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>x[<!NI;TYPE_MISMATCH!>bar()<!>]<!> = 1
    <!OI;TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>x[""]<!> = <!NI;TYPE_MISMATCH!>nullableInt<!>
    x[""] = 1

    val b1: MutableMap<String, Int?> = <!TYPE_MISMATCH!>x<!>
    val b2: MutableMap<String, Int> = x
    val b3: Map<String?, Int> = <!TYPE_MISMATCH!>x<!>
    val b4: Map<String?, Int?> = <!TYPE_MISMATCH!>x<!>
    val b5: Map<String, Int?> = x

    val b6: Int = <!TYPE_MISMATCH!>x[""]<!>
    val b7: Int = <!TYPE_MISMATCH!>x.get("")<!>

    val b8: Int? = x.get("")
}

fun treeMapTest() {
    var x: TreeMap<String, Int> = TreeMap<String, Int>()
    x.put(<!NULL_FOR_NONNULL_TYPE!>null<!>, <!NULL_FOR_NONNULL_TYPE!>null<!>)
    x.put("", <!NULL_FOR_NONNULL_TYPE!>null<!>)
    x.put(<!TYPE_MISMATCH!>bar()<!>, 1)
    x.put("", 1)

    <!OI;TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>x[<!NI;NULL_FOR_NONNULL_TYPE!>null<!>]<!> = 1
    <!OI;TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>x[<!NI;TYPE_MISMATCH!>bar()<!>]<!> = 1
    <!OI;TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>x[""]<!> = <!NI;TYPE_MISMATCH!>nullableInt<!>
    x[""] = 1

    val b1: MutableMap<String, Int?> = <!TYPE_MISMATCH!>x<!>
    val b2: MutableMap<String, Int> = x
    val b3: Map<String?, Int> = <!TYPE_MISMATCH!>x<!>
    val b4: Map<String?, Int?> = <!TYPE_MISMATCH!>x<!>
    val b5: Map<String, Int?> = x

    val b6: Int = <!TYPE_MISMATCH!>x[""]<!>
    val b7: Int = <!TYPE_MISMATCH!>x.get("")<!>

    val b8: Int? = x.get("")
}

fun concurrentHashMapTest() {
    var x: ConcurrentHashMap<String, Int> = ConcurrentHashMap<String, Int>()
    x.put(<!NULL_FOR_NONNULL_TYPE!>null<!>, <!NULL_FOR_NONNULL_TYPE!>null<!>)
    x.put("", <!NULL_FOR_NONNULL_TYPE!>null<!>)
    x.put(<!TYPE_MISMATCH!>bar()<!>, 1)
    x.put("", 1)

    <!OI;TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>x[<!NI;NULL_FOR_NONNULL_TYPE!>null<!>]<!> = 1
    <!OI;TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>x[<!NI;TYPE_MISMATCH!>bar()<!>]<!> = 1
    <!OI;TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>x[""]<!> = <!NI;TYPE_MISMATCH!>nullableInt<!>
    x[""] = 1

    val b1: MutableMap<String, Int?> = <!TYPE_MISMATCH!>x<!>
    val b2: MutableMap<String, Int> = x
    val b3: Map<String?, Int> = <!TYPE_MISMATCH!>x<!>
    val b4: Map<String?, Int?> = <!TYPE_MISMATCH!>x<!>
    val b5: Map<String, Int?> = x

    val b6: Int = <!TYPE_MISMATCH!>x[""]<!>
    val b7: Int = <!TYPE_MISMATCH!>x.get("")<!>

    val b8: Int? = x.get("")
}
