package org.jetbrains.container

import org.junit.Test
import java.io.Closeable
import kotlin.test.*

trait TestComponentInterface {
    public val disposed: Boolean
    fun foo()
}

trait TestClientComponentInterface {
}

class TestComponent : TestComponentInterface, Closeable {
    public override var disposed: Boolean = false
    override fun close() {
        disposed = true
    }

    override fun foo() {
        throw UnsupportedOperationException()
    }
}

class ManualTestComponent(val name: String) : TestComponentInterface, Closeable {
    public override var disposed: Boolean = false
    override fun close() {
        disposed = true
    }

    override fun foo() {
        throw UnsupportedOperationException()
    }
}

class TestClientComponent(val dep: TestComponentInterface) : TestClientComponentInterface, Closeable {
    override fun close() {
        if (dep.disposed)
            throw Exception("Dependency shouldn't be disposed before dependee")
        disposed = true
    }

    var disposed: Boolean = false
}

class TestClientComponent2() : TestClientComponentInterface {
}

class ComponentContainerTests {
    Test fun should_throw_when_not_composed() {
        val container = StorageComponentContainer("test")
        fails {
            container.resolve<TestComponentInterface>()
        }
    }

    Test fun should_resolve_to_null_when_empty() {
        val container = StorageComponentContainer("test").compose()
        assertNull(container.resolve<TestComponentInterface>())
    }

    Test fun should_resolve_to_instance_when_registered() {
        val container = StorageComponentContainer("test").register<TestComponent>().compose()
        val descriptor = container.resolve<TestComponentInterface>()
        assertNotNull(descriptor)
        val instance = descriptor!!.getValue() as TestComponentInterface
        assertNotNull(instance)
        fails {
            instance.foo()
        }
    }

    Test fun should_resolve_instance_dependency() {
        val container = StorageComponentContainer("test")
                .registerInstance(ManualTestComponent("name"))
                .register<TestClientComponent>()
                .compose()
        val descriptor = container.resolve<TestClientComponent>()
        assertNotNull(descriptor)
        val instance = descriptor!!.getValue() as TestClientComponent
        assertNotNull(instance)
        assertNotNull(instance.dep)
        fails {
            instance.dep.foo()
        }
        assertTrue(instance.dep is ManualTestComponent)
        assertEquals("name", (instance.dep as ManualTestComponent).name)
        container.close()
        assertTrue(instance.disposed)
        assertFalse(instance.dep.disposed) // should not dispose manually passed instances
    }

    Test fun should_resolve_type_dependency() {
        val container = StorageComponentContainer("test")
                .register<TestComponent>()
                .register<TestClientComponent>()
                .compose()

        val descriptor = container.resolve<TestClientComponent>()
        assertNotNull(descriptor)
        val instance = descriptor!!.getValue() as TestClientComponent
        assertNotNull(instance)
        assertNotNull(instance.dep)
        fails {
            instance.dep.foo()
        }
        container.close()
        assertTrue(instance.disposed)
        assertTrue(instance.dep.disposed)
    }

    Test fun should_resolve_multiple_types() {
        StorageComponentContainer("test")
                .register<TestComponent>()
                .register<TestClientComponent>()
                .register<TestClientComponent2>()
                .compose()
                .use {
                    val descriptor = it.resolveMultiple<TestClientComponentInterface>()
                    assertNotNull(descriptor)
                    assertEquals(2, descriptor.count())
                }
    }

    Test fun should_resolve_transient_types_to_different_instances() {
        StorageComponentContainer("test")
                .register<TestComponent>()
                .register<TestClientComponent>(ComponentLifetime.Transient)
                .compose()
                .use {
                    val descriptor1 = it.resolve<TestClientComponentInterface>()
                    assertNotNull(descriptor1)
                    val descriptor2 = it.resolve<TestClientComponentInterface>()
                    assertNotNull(descriptor2)
                    assertTrue(descriptor1 == descriptor2)
                    assertFalse(descriptor1!!.getValue() == descriptor2!!.getValue())
                }
    }

    Test fun should_resolve_singleton_types_to_same_instances() {
        StorageComponentContainer("test")
                .register<TestComponent>()
                .register<TestClientComponent>(ComponentLifetime.Singleton)
                .compose()
                .use {
                    val descriptor1 = it.resolve<TestClientComponentInterface>()
                    assertNotNull(descriptor1)
                    val descriptor2 = it.resolve<TestClientComponentInterface>()
                    assertNotNull(descriptor2)
                    assertTrue(descriptor1 == descriptor2)
                    assertTrue(descriptor1!!.getValue() == descriptor2!!.getValue())
                }
    }

}