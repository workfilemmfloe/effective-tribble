package test

/**

 *
 *
 * Test function

 *
 * @param first - Some
 * @param second - Other
 */
fun <caret>testFun(first: String, second: Int) = 12

// INFO: <b>internal</b> <b>fun</b> testFun(first: String, second: Int): Int<br/><p>Test function<br/><br/><br/><b>@param</b> - <i>first</i> - Some<br/><b>@param</b> - <i>second</i> - Other<br/></p>