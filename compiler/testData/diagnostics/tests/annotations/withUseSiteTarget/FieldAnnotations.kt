import kotlin.reflect.KProperty

annotation class Ann

class CustomDelegate {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): String = prop.name
}

<!INAPPLICABLE_TARGET_ON_PROPERTY, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@field:Ann<!>
class SomeClass {

    <!INAPPLICABLE_TARGET_ON_PROPERTY, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@field:Ann<!>
    constructor()

    @field:Ann
    protected val simpleProperty: String = "text"

    @field:[Ann]
    protected val simplePropertyWithAnnotationList: String = "text"

    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@field:Ann<!>
    protected val delegatedProperty: String by CustomDelegate()

    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@field:Ann<!>
    val propertyWithCustomGetter: Int
        get() = 5

    <!INAPPLICABLE_TARGET_ON_PROPERTY, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@field:Ann<!>
    fun anotherFun(<!INAPPLICABLE_TARGET_ON_PROPERTY!>@field:Ann<!> <!UNUSED_PARAMETER!>s<!>: String) {
        <!INAPPLICABLE_TARGET_ON_PROPERTY!>@field:Ann<!>
        val <!UNUSED_VARIABLE!>localVariable<!> = 5
    }

}

class WithPrimaryConstructor(@field:Ann val a: String)
