package com.myapp

import android.app.Activity
import kotlinx.android.synthetic.layout.*
import kotlinx.android.synthetic.layout1.*

class MyActivity: Activity() {
    val button = this.login
    val button1 = this.loginButton
}

// 1 public _\$_findCachedViewById
// 1 public _\$_clearFindViewByIdCache
// 1 GETSTATIC com/myapp/R\$id\.login : I
// 1 GETSTATIC com/myapp/R\$id\.loginButton : I
// 2 INVOKEVIRTUAL com/myapp/MyActivity\._\$_findCachedViewById
// 2 CHECKCAST android/widget/Button