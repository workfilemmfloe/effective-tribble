package b;

import a.*;

class J {
    void bar() {
        APackage.setTest("");
        System.out.println(APackage.getTest());
    }
}
