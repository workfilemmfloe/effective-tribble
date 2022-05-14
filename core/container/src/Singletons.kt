package org.jetbrains.container

import org.jetbrains.kotlin.di.getInfo
import java.io.Closeable
import java.util.ArrayList
import kotlin.properties.Delegates

enum class ComponentState {
    Null
    Initializing
    Initialized
    Corrupted
    Disposing
    Disposed
}

public abstract class SingletonDescriptor(val container: ComponentContainer) : ComponentDescriptor, Closeable {
    private var instance: Any? = null
    protected var state: ComponentState = ComponentState.Null
    private val disposableObjects by Delegates.lazy { ArrayList<Closeable>() }

    public override fun getValue(): Any {
        if (state == ComponentState.Corrupted)
            throw ContainerConsistencyException("Component descriptor $this is corrupted and cannot be accessed")
        if (state == ComponentState.Disposed)
            throw ContainerConsistencyException("Component descriptor $this is disposed and cannot be accessed");
        if (instance == null)
            createInstance(container);
        return instance!!
    }

    protected fun registerDisposableObject(ownedObject: Closeable) {
        disposableObjects.add(ownedObject);
    }

    protected abstract fun createInstance(context: ValueResolveContext): Any

    private fun createInstance(container: ComponentContainer) {
        when (state) {
            ComponentState.Null -> {
                try {
                    instance = createInstance(container.createResolveContext(this));
                    return;
                }
                catch (ex: Throwable) {
                    state = ComponentState.Corrupted;
                    for (disposable in disposableObjects)
                        disposable.close();
                    throw ex
                }
            }
            ComponentState.Initializing ->
                throw ContainerConsistencyException("Could not create the component $this because it is being initialized. Do we have undetected circular dependency?");
            ComponentState.Initialized ->
                throw ContainerConsistencyException("Could not get the component $this. Instance is null in Initialized state");
            ComponentState.Corrupted ->
                throw ContainerConsistencyException("Could not get the component $this because it is corrupted");
            ComponentState.Disposing ->
                throw ContainerConsistencyException("Could not get the component $this because it is being disposed");
            ComponentState.Disposed ->
                throw ContainerConsistencyException("Could not get the component $this because it is already disposed");
        }
    }

    private fun disposeImpl() {
        val wereInstance = instance;
        state = ComponentState.Disposing;
        instance = null; // cannot get instance any more
        try {
            if (wereInstance is Closeable)
                wereInstance.close();
            for (disposable in disposableObjects)
                disposable.close();
        }
        catch(ex: Throwable) {
            state = ComponentState.Corrupted;
            throw ex;
        }
        state = ComponentState.Disposed;
    }

    override fun close() {
        when (state) {
            ComponentState.Initialized ->
                disposeImpl();
            ComponentState.Corrupted -> {
            } // corrupted component is in the undefined state, ignore
            ComponentState.Null -> {
            } // it's ok to to remove null component, it may have been never needed

            ComponentState.Initializing ->
                throw ContainerConsistencyException("The component is being initialized and cannot be disposed.");
            ComponentState.Disposing ->
                throw ContainerConsistencyException("The component is already in disposing state.");
            ComponentState.Disposed ->
                throw ContainerConsistencyException("The component has already been destroyed.");
        }
    }
}

public abstract class SingletonComponentDescriptor(container: ComponentContainer, val klass: Class<*>) : SingletonDescriptor(container) {
    public override fun getRegistrations(): Iterable<Class<*>> = klass.getInfo().registrations
}

public class SingletonTypeComponentDescriptor(container: ComponentContainer, klass: Class<*>) : SingletonComponentDescriptor(container, klass) {
    override fun createInstance(context: ValueResolveContext): Any = createInstanceOf(klass, context)
    private fun createInstanceOf(klass: Class<*>, context: ValueResolveContext): Any {
        val binding = klass.bindToConstructor(context)
        state = ComponentState.Initializing
        for (argumentDescriptor in binding.argumentDescriptors) {
            if (argumentDescriptor is Closeable && argumentDescriptor !is SingletonDescriptor) {
                registerDisposableObject(argumentDescriptor)
            }
        }

        val constructor = binding.constructor
        val arguments = bindArguments(binding.argumentDescriptors)

        val instance = constructor.newInstance(*arguments.copyToArray())!!
        state = ComponentState.Initialized
        return instance
    }

    override fun getDependencies(context: ValueResolveContext): Collection<Class<*>> {
        val classInfo = klass.getInfo()
        return classInfo.constructorInfo?.parameters.orEmpty() + classInfo.setterInfos.flatMap { it.parameters }
    }
}