@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("ExtensionsKt")
package kotlin

import java.lang.reflect.Method
import kotlin.jvm.internal.Intrinsic
import kotlin.reflect.KClass

/**
 * This annotation indicates what exceptions should be declared by a function when compiled to a JVM method.
 *
 * Example:
 *
 * ```
 * throws(IOException::class)
 * fun readFile(name: String): String {...}
 * ```
 *
 * will be translated to
 *
 * ```
 * String readFile(String name) throws IOException {...}
 * ```
 *
 * @property exceptionClasses the list of checked exception classes that may be thrown by the function.
 */
@Retention(AnnotationRetention.SOURCE)
@Deprecated("Use 'kotlin.jvm.Throws' instead", ReplaceWith("kotlin.jvm.Throws"), DeprecationLevel.ERROR)
public annotation class throws(public vararg val exceptionClasses: KClass<out Throwable>)

/**
 * Returns the runtime Java class of this object.
 */
@Intrinsic("kotlin.javaClass.property")
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public val <T: Any> T.javaClass : Class<T>
    get() = (this as java.lang.Object).getClass() as Class<T>

/**
 * Returns the Java class for the specified type.
 */
@Intrinsic("kotlin.javaClass.function")
@Suppress("REIFIED_TYPE_PARAMETER_NO_INLINE")
@Deprecated("Use the class reference and .java extension property instead: MyClass::class.java", ReplaceWith("T::class.java"))
public fun <reified T: Any> javaClass(): Class<T> = T::class.java


/**
 * Returns the annotation type of this annotation.
 */
@Deprecated("Use annotationClass.java instead", ReplaceWith("annotationClass.java"))
public fun <T : Annotation> T.annotationType() : Class<out T> = annotationClass.java

/**
 * Invokes the underlying method represented by this [Method] object, on the specified [instance] with the specified parameters.
 */
@Suppress("NOTHING_TO_INLINE")
public inline operator fun Method.invoke(instance: Any, vararg args: Any?): Any? {
    return invoke(instance, *args)
}
