// IS_AVAILABLE: true
// ERROR: <html>None of the following functions can be called with the arguments supplied. <ul><li>bar(<font color=red><b>Int = ...</b></font>, <font color=red><b>(Int) &rarr; Int</b></font>) <i>defined in</i> root package</li><li>bar(<font color=red><b>Int</b></font>, <font color=red><b>Int</b></font>, <font color=red><b>(Int) &rarr; Int</b></font>) <i>defined in</i> root package</li></ul></html>
// ERROR: Unresolved reference: it

fun foo() {
    bar(<caret>{ it })
}

fun bar(a: Int = 0, f: (Int) -> Int) { }
fun bar(a: Int, b: Int, f: (Int) -> Int) { }

