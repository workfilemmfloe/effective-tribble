// See EA-76890 / KT-10843: NPE during analysis
fun lambda(x : Int?) = x?.<!NO_VALUE_FOR_PARAMETER, TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER, FUNCTION_CALL_EXPECTED!>let<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>l<!> {
    <!CANNOT_INFER_PARAMETER_TYPE!>y<!> ->
    if (<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>y<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>><!> 0) return<!UNRESOLVED_REFERENCE!>@l<!> x
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>y<!>
}<!NOT_NULL_ASSERTION_ON_LAMBDA_EXPRESSION!>!!<!>
