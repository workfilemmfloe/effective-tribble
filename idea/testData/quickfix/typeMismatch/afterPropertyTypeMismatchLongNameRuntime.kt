// "Change 'f' type to '(Delegates) -> Unit'" "true"

import kotlin.properties.Delegates

fun foo() {
    var f: (Delegates) -> Unit = { (x: kotlin.properties.Delegates) ->  }<caret>
}