// "Delete redundant extension property" "false"
// ACTION: Move to companion object

class C : Thread() {
    var Thread.<caret>priority: Int
        get() = getPriority()
        set(value) {
            this@C.setPriority(value)
        }
}
