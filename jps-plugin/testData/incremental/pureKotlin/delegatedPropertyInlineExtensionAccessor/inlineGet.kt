package inline

import kotlin.reflect.KProperty

inline fun Inline.getValue(receiver: Any?, prop: KProperty<*>): Int {
    return 0
}
