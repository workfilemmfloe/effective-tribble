package foo.bar

/*p:foo.bar*/fun testComparisons(a: /*p:foo.bar*/A, b: /*p:foo.bar*/Int, c: /*p:foo.bar*/Any, na: /*p:foo.bar*/A?) {
    a /*c:foo.bar.A(equals)*/== c
    a /*c:foo.bar.A(equals)*/!= c
    na /*c:foo.bar.A(equals)*/== a
    na /*c:foo.bar.A(equals)*/== null

    a /*c:foo.bar.A(compareTo) p:foo.bar(compareTo) p:java.lang(compareTo) p:kotlin(compareTo) p:kotlin.annotation(compareTo) p:kotlin.jvm(compareTo) p:kotlin.io(compareTo) c:foo.bar.A(getCompareTo) c:foo.bar.A(getCOMPARETo)*/> b
    a /*c:foo.bar.A(compareTo) p:foo.bar(compareTo) p:java.lang(compareTo) p:kotlin(compareTo) p:kotlin.annotation(compareTo) p:kotlin.jvm(compareTo) p:kotlin.io(compareTo) c:foo.bar.A(getCompareTo) c:foo.bar.A(getCOMPARETo)*/< b
    a /*c:foo.bar.A(compareTo) p:foo.bar(compareTo) p:java.lang(compareTo) p:kotlin(compareTo) p:kotlin.annotation(compareTo) p:kotlin.jvm(compareTo) p:kotlin.io(compareTo) c:foo.bar.A(getCompareTo) c:foo.bar.A(getCOMPARETo)*/>= b
    a /*c:foo.bar.A(compareTo) p:foo.bar(compareTo) p:java.lang(compareTo) p:kotlin(compareTo) p:kotlin.annotation(compareTo) p:kotlin.jvm(compareTo) p:kotlin.io(compareTo) c:foo.bar.A(getCompareTo) c:foo.bar.A(getCOMPARETo)*/<= b

    a /*c:foo.bar.A(compareTo) p:foo.bar(compareTo) p:java.lang(compareTo) p:kotlin(compareTo) p:kotlin.annotation(compareTo) p:kotlin.jvm(compareTo) p:kotlin.io(compareTo) c:foo.bar.A(getCompareTo) c:foo.bar.A(getCOMPARETo)*/> c
    a /*c:foo.bar.A(compareTo) p:foo.bar(compareTo) p:java.lang(compareTo) p:kotlin(compareTo) p:kotlin.annotation(compareTo) p:kotlin.jvm(compareTo) p:kotlin.io(compareTo) c:foo.bar.A(getCompareTo) c:foo.bar.A(getCOMPARETo)*/< c
    a /*c:foo.bar.A(compareTo) p:foo.bar(compareTo) p:java.lang(compareTo) p:kotlin(compareTo) p:kotlin.annotation(compareTo) p:kotlin.jvm(compareTo) p:kotlin.io(compareTo) c:foo.bar.A(getCompareTo) c:foo.bar.A(getCOMPARETo)*/>= c
    a /*c:foo.bar.A(compareTo) p:foo.bar(compareTo) p:java.lang(compareTo) p:kotlin(compareTo) p:kotlin.annotation(compareTo) p:kotlin.jvm(compareTo) p:kotlin.io(compareTo) c:foo.bar.A(getCompareTo) c:foo.bar.A(getCOMPARETo)*/<= c
}
