// "Add 'kotlin.Any' as upper bound for E" "true"
// ERROR: <html>Type parameter bound for <b>U</b> in <table><tr><td width="10%"></td><td align="right" colspan="2" style="white-space:nowrap;font-weight:bold;"><b>fun</b> &lt;T : kotlin.Any, U : kotlin.Any&gt; foo</td><td style="white-space:nowrap;font-weight:bold;">(</td><td align="right" style="white-space:nowrap;font-weight:bold;">x: T,</td><td align="right" style="white-space:nowrap;font-weight:bold;">y: U</td><td style="white-space:nowrap;font-weight:bold;">)</td><td style="white-space:nowrap;font-weight:bold;">: kotlin.Int</td></tr></table> is not satisfied: inferred type <font color=red><b>F</b></font> is not a subtype of <b>kotlin.Any</b></html>

fun <T : Any, U: Any> foo(x: T, y: U) = 1

fun <E, F> bar(x: E, y: F) = <caret>foo(x, y)
