public class Testing {
    public static void test() {
        testing.<caret>
    }
}

// EXIST: jet1
// ABSENT: jet2

// Only two proposals expected
// NUMBER: 1
