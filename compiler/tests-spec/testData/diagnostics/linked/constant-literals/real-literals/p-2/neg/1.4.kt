/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: constant-literals, real-literals -> paragraph 2 -> sentence 1
 * NUMBER: 4
 * DESCRIPTION: Real literals suffixed by f/F (float suffix) with an exponent mark without digits after it.
 */

// TESTCASE NUMBER: 1
val value_1 = <!FLOAT_LITERAL_OUT_OF_RANGE!>0.0EF<!>

// TESTCASE NUMBER: 2
val value_2 = <!FLOAT_LITERAL_OUT_OF_RANGE!>00.0ef<!>

// TESTCASE NUMBER: 3
val value_3 = <!FLOAT_LITERAL_OUT_OF_RANGE!>000.00e-f<!>

// TESTCASE NUMBER: 4
val value_4 = <!FLOAT_LITERAL_OUT_OF_RANGE!>0000.000E+F<!>

// TESTCASE NUMBER: 5
val value_5 = <!FLOAT_LITERAL_OUT_OF_RANGE!>1.0eF<!>

// TESTCASE NUMBER: 6
val value_6 = <!FLOAT_LITERAL_OUT_OF_RANGE!>22.00E+f<!>

// TESTCASE NUMBER: 7
val value_7 = <!FLOAT_LITERAL_OUT_OF_RANGE!>333.000ef<!>

// TESTCASE NUMBER: 8
val value_8 = <!FLOAT_LITERAL_OUT_OF_RANGE!>4444.0000Ef<!>

// TESTCASE NUMBER: 9
val value_9 = <!FLOAT_LITERAL_OUT_OF_RANGE!>55555.0e+F<!>

// TESTCASE NUMBER: 10
val value_10 = <!FLOAT_LITERAL_OUT_OF_RANGE!>666666.00E-F<!>

// TESTCASE NUMBER: 11
val value_11 = <!FLOAT_LITERAL_OUT_OF_RANGE!>7777777.000eF<!>

// TESTCASE NUMBER: 12
val value_12 = <!FLOAT_LITERAL_OUT_OF_RANGE!>88888888.0000ef<!>

// TESTCASE NUMBER: 13
val value_13 = <!FLOAT_LITERAL_OUT_OF_RANGE!>999999999.0E-F<!>

// TESTCASE NUMBER: 14
val value_14 = <!FLOAT_LITERAL_OUT_OF_RANGE!>7.888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888e+f<!>

// TESTCASE NUMBER: 15
val value_15 = <!FLOAT_LITERAL_OUT_OF_RANGE!>0.000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000eF<!>

// TESTCASE NUMBER: 16
val value_16 = <!FLOAT_LITERAL_OUT_OF_RANGE!>0.000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000e-f<!>

// TESTCASE NUMBER: 17
val value_17 = <!FLOAT_LITERAL_OUT_OF_RANGE!>0.000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000e+F<!>
