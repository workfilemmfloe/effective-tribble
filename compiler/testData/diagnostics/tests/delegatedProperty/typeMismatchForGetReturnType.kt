val c: Int by <!DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH!>Delegate()<!>

class Delegate {
  fun get(t: Any?, p: PropertyMetadata): String {
    t.equals(p) // to avoid UNUSED_PARAMETER warning
    return ""
  }
}
