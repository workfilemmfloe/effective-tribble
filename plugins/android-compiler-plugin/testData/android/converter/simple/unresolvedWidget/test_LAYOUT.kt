package kotlinx.android.synthetic.test

import kotlin.internal.flexible.ft

@kotlin.internal.flexible.InvalidWidgetType("KeyboardView")
val android.app.Activity.MyKeyboardView: ft<android.view.View, android.view.View?>
    get() = findViewById(0) as? android.view.View

@kotlin.internal.flexible.InvalidWidgetType("KeyboardView")
val android.app.Fragment.MyKeyboardView: ft<android.view.View, android.view.View?>
    get() = getView().findViewById(0) as? android.view.View

