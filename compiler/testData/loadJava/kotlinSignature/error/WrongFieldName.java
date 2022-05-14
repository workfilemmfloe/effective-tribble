package test;

import jet.runtime.typeinfo.KotlinSignature;

import java.lang.String;

public class WrongFieldName {
    @KotlinSignature("var bar: String")
    public String foo;
}
