package a

fun foo0(f: () -> String) = f
fun foo1(f: (Int) -> String) = f
fun foo2(f: (Int, String) -> String) = f

fun test1() {
    foo0 {
        ""
    }
    foo0 {
        <!DEPRECATED_LAMBDA_SYNTAX, EXPECTED_PARAMETERS_NUMBER_MISMATCH!>(s: String)<!> -> ""
    }
    foo0 {
        <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!><!CANNOT_INFER_PARAMETER_TYPE!>x<!>, <!CANNOT_INFER_PARAMETER_TYPE!>y<!><!> -> ""
    }
    foo0 {
        <!DEPRECATED_LAMBDA_SYNTAX!>(): <!EXPECTED_RETURN_TYPE_MISMATCH!>Int<!><!> -> 42
    }

    foo1 {
        ""
    }
    foo1 {
        <!DEPRECATED_LAMBDA_SYNTAX!>(<!EXPECTED_PARAMETER_TYPE_MISMATCH!>s: String<!>)<!> -> ""
    }
    foo1 {
        <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>x, <!CANNOT_INFER_PARAMETER_TYPE!>y<!><!> -> ""
    }
    foo1 {
        <!DEPRECATED_LAMBDA_SYNTAX!><!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>()<!>: <!EXPECTED_RETURN_TYPE_MISMATCH!>Int<!><!> -> 42
    }


    foo2 <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>{<!>
        ""
    }
    foo2 {
        <!DEPRECATED_LAMBDA_SYNTAX, EXPECTED_PARAMETERS_NUMBER_MISMATCH!>(<!EXPECTED_PARAMETER_TYPE_MISMATCH!>s: String<!>)<!> -> ""
    }
    foo2 {
        <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>x<!> -> ""
    }
    foo2 {
        <!DEPRECATED_LAMBDA_SYNTAX!><!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>()<!>: <!EXPECTED_RETURN_TYPE_MISMATCH!>Int<!><!> -> 42
    }
}