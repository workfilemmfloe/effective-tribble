import kotlin.reflect.*

var x: Int = 42
val y: String get() = "y"

fun testX() {
    val xx = ::x
    xx : KMutableTopLevelProperty<Int>
    xx : KMutableTopLevelVariable<Int>
    xx : KTopLevelProperty<Int>
    xx : KTopLevelVariable<Int>
    xx : KMutableProperty<Int>
    xx : KMutableVariable<Int>
    xx : KProperty<Int>
    xx : KCallable<Int>

    xx.name : String
    xx.get() : Int
    xx.set(239)
}

fun testY() {
    val yy = ::y
    <!TYPE_MISMATCH!>yy<!> : KMutableTopLevelProperty<String>
    yy : KTopLevelVariable<String>
    <!TYPE_MISMATCH!>yy<!> : KMutableProperty<String>
    yy : KProperty<String>
    yy : KCallable<String>

    yy.name : String
    yy.get() : String
    yy.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>set<!>("yy")
}
