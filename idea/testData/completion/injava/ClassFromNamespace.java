public class Testing {
    public static void test() {
        jettesting.some.<caret>
    }
}

// EXIST: ClassFromJet
// EXIST: namespace
// NUMBER: 2
