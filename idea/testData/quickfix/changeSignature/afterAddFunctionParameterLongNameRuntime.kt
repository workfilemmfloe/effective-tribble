// "Add parameter to function 'foo'" "true"
// DISABLE-ERRORS

import java.util.LinkedHashSet

fun foo(x: Int, linkedHashSet: LinkedHashSet<Int>) {
    foo(, java.util.LinkedHashSet<Int>());
    foo(1, java.util.LinkedHashSet<Int>());
    foo(2, java.util.LinkedHashSet<Int>());
}