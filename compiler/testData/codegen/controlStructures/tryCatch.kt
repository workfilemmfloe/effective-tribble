fun foo(s: String): String? {
  try {
    Integer.parseInt(s);
    return "no message";
  }
  catch(e: NumberFormatException) {
    return e.message // Work around an overload-resolution bug
  }
}
