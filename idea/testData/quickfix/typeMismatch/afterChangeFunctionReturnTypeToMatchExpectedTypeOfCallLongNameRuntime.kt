// "Change 'bar' function return type to 'Module'" "true"

import kotlin.modules.Module

fun bar(): Module = kotlin.modules.ModuleBuilder("", "")
fun foo(): kotlin.modules.Module = bar()