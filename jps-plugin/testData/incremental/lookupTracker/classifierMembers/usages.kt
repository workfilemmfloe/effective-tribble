package foo

import bar.*

/*p:foo*/fun usages(i: /*p:foo*/I) {
    /*p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io*/A()./*c:foo.A*/a
    /*p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io*/A()./*c:foo.A*/b
    /*p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io*/A()./*c:foo.A*/c
    /*p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io*/A()./*c:foo.A*/d = "new value"
    /*p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io*/A()./*c:foo.A*/foo()
    /*p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io*/A./*c:foo.A*/B()./*c:foo.A.B*/a
    /*p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io*/A./*c:foo.A*/B./*c:foo.A.B c:foo.A.B.CO*/bar(1)
    /*p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io*/A./*c:foo.A*/B./*c:foo.A.B*/CO./*c:foo.A.B.CO*/bar(1)
    /*p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io*/A
    /*p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io*/A./*c:foo.A c:foo.A.Companion*/a
    /*p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io*/A./*c:foo.A c:foo.A.Companion*/baz()
    /*p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io*/A./*c:foo.A*/Companion
    /*p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io*/A./*c:foo.A*/Companion./*c:foo.A.Companion*/baz()
    /*p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io*/A./*c:foo.A*/O
    /*p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io*/A./*c:foo.A*/O./*c:foo.A.O*/v = "OK"
    /*p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io*/A./*c:foo.A c:foo.A.Companion p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io c:foo.A.Companion(getVala) c:foo.A.Companion(getVALA)*/vala
    /*p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io*/A./*c:foo.A c:foo.A.Companion p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io c:foo.A.Companion(getVara) c:foo.A.Companion(getVARA)*/vara()

    i./*c:foo.I*/a = 2
    /*p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io*/Obj./*c:foo.Obj*/a
    /*p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io*/Obj./*c:foo.Obj*/foo()
    var ii: /*p:foo*/I = /*p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io*/Obj
    ii./*c:foo.I*/a
    ii./*c:foo.I*/foo()
    /*p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io*/Obj./*c:foo.Obj*/b
    val iii = /*p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io*/Obj./*c:foo.Obj*/bar()
    iii./*c:foo.I*/foo()

    /*p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io*/E./*c:foo.E*/X
    /*p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io*/E./*c:foo.E*/X./*c:foo.E*/a
    /*p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io*/E./*c:foo.E*/Y./*c:foo.E*/foo()
    /*p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io*/E./*c:foo.E*/values
    /*p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io*/E./*c:foo.E*/values()
    /*p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io*/E./*c:foo.E*/valueOf("")
    /*p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io*/E./*c:foo.E*/foo
    /*p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io*/E./*c:foo.E*/bar()
}

/*p:foo*/fun classifiers(
        a: /*p:foo*/A,
        ab: /*p:foo*/A./*c:foo.A*/B,
        ac: /*p:foo*/A./*c:foo.A*/C,
        abCo: /*p:foo*/A./*c:foo.A*/B./*c:foo.A.B*/CO,
        aCompanion: /*p:foo*/A./*c:foo.A*/Companion,
        aO: /*p:foo*/A./*c:foo.A*/O,
        i: /*p:foo*/I,
        ni: /*p:foo*/I./*c:foo.I*/NI,
        obj: /*p:foo*/Obj,
        e: /*p:foo*/E
) {}
