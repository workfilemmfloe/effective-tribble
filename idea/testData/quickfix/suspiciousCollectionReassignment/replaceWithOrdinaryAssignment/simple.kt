// "Replace with ordinary assignment" "true"
// TOOL: org.jetbrains.kotlin.idea.inspections.SuspiciousCollectionReassignmentInspection
// ACTION: Replace overloaded operator with function call
// WITH_RUNTIME
fun test(otherList: List<Int>, flag: Boolean) {
    var list = otherList
    if (flag) {
        list <caret>+= 4
    }
}
