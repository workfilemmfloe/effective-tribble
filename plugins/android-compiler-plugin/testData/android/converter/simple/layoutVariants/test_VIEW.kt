package kotlinx.android.synthetic.test.view

import kotlin.internal.flexible.ft

val android.view.View.button: ft<android.view.View, android.view.View?>
    get() = findViewById(0) as? android.view.View

