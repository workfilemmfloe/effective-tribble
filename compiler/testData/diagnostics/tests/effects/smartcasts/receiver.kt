// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

import kotlin.internal.*

@Returns(ConstantValue.FALSE)
fun @receiver:Equals(ConstantValue.NOT_NULL) Any?.isNull() = this == null

fun testSmartcastOnReceiver(x: Int?) {
    if (x.isNull()) {
        x<!UNSAFE_CALL!>.<!>inc()
    } else {
        <!DEBUG_INFO_SMARTCAST!>x<!>.dec()
    }
}