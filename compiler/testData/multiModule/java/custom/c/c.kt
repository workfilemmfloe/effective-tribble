package test

import custom.*

public class KotlinC: AClass() {
    public fun returnA(): AClass {}

    public fun paramA(a: AClass) {}

    public fun paramB(b: BClass) {}

    public fun returnB(): BClass { }

    @AAnnotation fun annoA() {}

    @BAnnotation fun annoB() {}
}