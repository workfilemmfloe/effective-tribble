package test;

class EnumAutoGeneratedMethods {
    void foo() {
        TestEnum[] vals = TestEnum.values();
        assert TestEnum.valueOf("first") == TestEnum.first;
    }
}
