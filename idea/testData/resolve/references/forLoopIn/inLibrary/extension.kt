fun main() {
  for (i <caret>in "") {}
}

// MULTIRESOLVE
// REF: (for kotlin.CharSequence in kotlin.text).iterator()
// REF: (in kotlin.CharIterator).next()
// REF: (in kotlin.Iterator).hasNext()
