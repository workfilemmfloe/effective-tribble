package kotlin.properties

import java.util.NoSuchElementException
import kotlin.reflect.KProperty

/**
 * Standard property delegates.
 */
public object Delegates {
    /**
     * Returns a property delegate for a read/write property with a non-`null` value that is initialized not during
     * object construction time but at a later time. Trying to read the property before the initial value has been
     * assigned results in an exception.
     */
    public fun <T: Any> notNull(): ReadWriteProperty<Any?, T> = NotNullVar()

    /**
     * Returns a property delegate for a read/write property that calls a specified callback function when changed.
     * @param initialValue the initial value of the property.
     * @param onChange the callback which is called after the change of the property is made. The value of the property
     *  has already been changed when this callback is invoked.
     */
    public inline fun <T> observable(initialValue: T, crossinline onChange: (property: KProperty<*>, oldValue: T, newValue: T) -> Unit):
        ReadWriteProperty<Any?, T> = object : ObservableProperty<T>(initialValue) {
            override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) = onChange(property, oldValue, newValue)
        }

    /**
     * Returns a property delegate for a read/write property that calls a specified callback function when changed,
     * allowing the callback to veto the modification.
     * @param initialValue the initial value of the property.
     * @param onChange the callback which is called before a change to the property value is attempted.
     *  The value of the property hasn't been changed yet, when this callback is invoked.
     *  If the callback returns `true` the value of the property is being set to the new value,
     *  and if the callback returns `false` the new value is discarded and the property remains its old value.
     */
    public inline fun <T> vetoable(initialValue: T, crossinline onChange: (property: KProperty<*>, oldValue: T, newValue: T) -> Boolean):
        ReadWriteProperty<Any?, T> = object : ObservableProperty<T>(initialValue) {
            override fun beforeChange(property: KProperty<*>, oldValue: T, newValue: T): Boolean = onChange(property, oldValue, newValue)
        }

    /**
     * Returns a property delegate for a read/write property that stores its value in a map, using the property name
     * as a key.
     * @param map the map where the property values are stored.
     */
    @Deprecated("Delegate property to the map itself without creating a wrapper.", ReplaceWith("map", "kotlin.properties.get", "kotlin.properties.set"))
    public fun <T> mapVar(map: MutableMap<in String, Any?>): ReadWriteProperty<Any?, T> {
        return FixedMapVar<Any?, String, T>(map, propertyNameSelector, throwKeyNotFound)
    }

    /**
     * Returns a property delegate for a read/write property that stores its value in a map, using the property name
     * as a key.
     * @param map the map where the property values are stored.
     * @param default the function returning the value of the property for a given object if it's missing from the given map.
     */
    public fun <T> mapVar(map: MutableMap<in String, Any?>,
                          default: (thisRef: Any?, desc: String) -> T): ReadWriteProperty<Any?, T> {
        return FixedMapVar<Any?, String, T>(map, propertyNameSelector, default)
    }

    /**
     * Returns a property delegate for a read-only property that takes its value from a map, using the property name
     * as a key.
     * @param map the map where the property values are stored.
     */
    @Deprecated("Delegate property to the map itself without creating a wrapper.", ReplaceWith("map", "kotlin.properties.get"))
    public fun <T> mapVal(map: Map<in String, Any?>): ReadOnlyProperty<Any?, T> {
        return FixedMapVal<Any?, String, T>(map, propertyNameSelector, throwKeyNotFound)
    }

    /**
     * Returns a property delegate for a read-only property that takes its value from a map, using the property name
     * as a key.
     * @param map the map where the property values are stored.
     * @param default the function returning the value of the property for a given object if it's missing from the given map.
     */
    public fun <T> mapVal(map: Map<in String, Any?>,
                          default: (thisRef: Any?, desc: String) -> T): ReadOnlyProperty<Any?, T> {
        return FixedMapVal<Any?, String, T>(map, propertyNameSelector, default)
    }
}


private class NotNullVar<T: Any>() : ReadWriteProperty<Any?, T> {
    private var value: T? = null

    public override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value ?: throw IllegalStateException("Property ${property.name} should be initialized before get.")
    }

    public override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}

/**
 * Implements the core logic of a property delegate for a read/write property that calls callback functions when changed.
 * @param initialValue the initial value of the property.
 */
public abstract class ObservableProperty<T>(initialValue: T) : ReadWriteProperty<Any?, T> {
    private var value = initialValue

    /**
     *  The callback which is called before a change to the property value is attempted.
     *  The value of the property hasn't been changed yet, when this callback is invoked.
     *  If the callback returns `true` the value of the property is being set to the new value,
     *  and if the callback returns `false` the new value is discarded and the property remains its old value.
     */
    protected open fun beforeChange(property: KProperty<*>, oldValue: T, newValue: T): Boolean = true

    /**
     * The callback which is called after the change of the property is made. The value of the property
     * has already been changed when this callback is invoked.
     */
    protected open fun afterChange (property: KProperty<*>, oldValue: T, newValue: T): Unit {}

    public override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }

    public override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val oldValue = this.value
        if (!beforeChange(property, oldValue, value)) {
            return
        }
        this.value = value
        afterChange(property, oldValue, value)
    }
}

/**
 * Implements the core logic for a property delegate that stores property values in a map.
 * @param T the type of the object that owns the delegated property.
 * @param K the type of key in the map.
 * @param V the type of the property value.
 */
public abstract class MapVal<T, K, out V>() : ReadOnlyProperty<T, V> {
    /**
     * Returns the map used to store the values of the properties of the given object instance.
     * @param ref the object instance for which the map is requested.
     */
    protected abstract fun map(ref: T): Map<in K, Any?>

    /**
     * Returns the map key used to store the values of the given property.
     * @param property the property for which the key is requested.
     */
    protected abstract fun key(property: KProperty<*>): K

    /**
     * Returns the property value to be used when the map does not contain the corresponding key.
     * @param ref the object instance for which the value was requested.
     * @param property the property for which the value was requested.
     */
    protected open fun default(ref: T, property: KProperty<*>): V {
        throw NoSuchElementException("The value for property ${property.name} is missing in $ref.")
    }

    public override fun getValue(thisRef: T, property: KProperty<*>) : V {
        val map = map(thisRef)
        val key = key(property)
        return map.getOrElse(key, { default(thisRef, property) }) as V
    }
}

/**
 * Implements the core logic for a read/write property delegate that stores property values in a map.
 * @param T the type of the object that owns the delegated property.
 * @param K the type of key in the map.
 * @param V the type of the property value.
 */
public abstract class MapVar<T, K, V>() : MapVal<T, K, V>(), ReadWriteProperty<T, V> {
    protected abstract override fun map(ref: T): MutableMap<in K, Any?>

    public override fun setValue(thisRef: T, property: KProperty<*>, value: V) {
        val map = map(thisRef)
        map.put(key(property), value)
    }

    override fun getValue(thisRef: T, property: KProperty<*>): V {
        return super<MapVal>.getValue(thisRef, property)
    }
}

private val propertyNameSelector: (KProperty<*>) -> String = {it.name}
private val throwKeyNotFound: (Any?, Any?) -> Nothing = {thisRef, key -> throw NoSuchElementException("The value for key $key is missing from $thisRef.") }

/**
 * Implements a read-only property delegate that stores the property values in a given map instance and uses the given
 * callback functions for calculating the key and the default value for each property.
 * @param map the map used to store the values.
 * @param key the function to calculate the map key from a property metadata object.
 * @param default the function returning the value of the property for a given object if it's missing from the given map.
 */
public open class FixedMapVal<T, K, out V>(
        private val map: Map<in K, Any?>,
        private val key: (KProperty<*>) -> K,
        private val default: (ref: T, key: K) -> V = throwKeyNotFound
) : MapVal<T, K, V>() {
    protected override fun map(ref: T): Map<in K, Any?> {
        return map
    }

    protected override fun key(property: KProperty<*>): K {
        return (key)(property)
    }

    protected override fun default(ref: T, property: KProperty<*>): V {
        return (default)(ref, key(property))
    }
}

/**
 * Implements a read/write property delegate that stores the property values in a given map instance and uses the given
 * callback functions for calculating the key and the default value for each property.
 * @param map the map used to store the values.
 * @param key the function to calculate the map key from a property metadata object.
 * @param default the function returning the value of the property for a given object if it's missing from the given map.
 */
public open class FixedMapVar<T, K, V>(
        private val map: MutableMap<in K, Any?>,
        private val key: (KProperty<*>) -> K,
        private val default: (ref: T, key: K) -> V = throwKeyNotFound
) : MapVar<T, K, V>() {
    protected override fun map(ref: T): MutableMap<in K, Any?> {
        return map
    }

    protected override fun key(property: KProperty<*>): K {
        return (key)(property)
    }

    protected override fun default(ref: T, property: KProperty<*>): V {
        return (default)(ref, key(property))
    }
}
