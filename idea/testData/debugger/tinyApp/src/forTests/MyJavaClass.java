package forTests;

import org.jetbrains.annotations.NotNull;

public class MyJavaClass {
    public void testFun() {
       int i = 1;
    }

    @NotNull
    public String testNotNullFun() {
       return "a";
    }

    public static int staticFun(Object s) {
        return 1;
    }
}
