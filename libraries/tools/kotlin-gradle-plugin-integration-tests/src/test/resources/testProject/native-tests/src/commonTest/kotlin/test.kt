/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.foo.test

import org.foo.*
import kotlin.test.*

@Test
fun fooTest() {
    println(foo())
}

@Test
fun barTest() {
    println(bar())
}

class Bar : Foo()

@Test
fun barfooTest() {
    println(Bar().foo())
}
