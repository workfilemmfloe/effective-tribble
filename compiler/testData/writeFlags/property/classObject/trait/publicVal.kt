interface Test {
  companion object {
    public val prop: Int = 0;
  }
}

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop
// FLAGS: ACC_PUBLIC, ACC_FINAL, ACC_STATIC, ACC_DEPRECATED

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test$Companion, prop
// FLAGS: ACC_PUBLIC, ACC_FINAL, ACC_STATIC, ACC_DEPRECATED