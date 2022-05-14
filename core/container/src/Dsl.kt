/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.di

import org.jetbrains.container.StorageComponentContainer
import org.jetbrains.container.registerInstance
import org.jetbrains.container.registerSingleton
import kotlin.properties.ReadOnlyProperty

public fun createContainer(id: String, init: StorageComponentContainer.() -> Unit): StorageComponentContainer {
    val c = StorageComponentContainer(id)
    c.init()
    c.compose()
    return c
}

public inline fun <reified T> StorageComponentContainer.useImpl() {
    registerSingleton(javaClass<T>())
}

public inline fun <reified T> StorageComponentContainer.get(): T {
    return resolve(javaClass<T>(), unknownContext)!!.getValue() as T
}

public fun StorageComponentContainer.useInstance(instance: Any) {
    registerInstance(instance)
}

public class InjectedProperty<T>(
        private val container: StorageComponentContainer, private val requestedComponent: Class<T>
) : ReadOnlyProperty<Any?, T> {
    override fun get(thisRef: Any?, desc: PropertyMetadata): T {
        return container.resolve(requestedComponent)!!.getValue() as T
    }
}

public inline fun <R, reified T> injected(container: StorageComponentContainer): ReadOnlyProperty<R, T>
        = InjectedProperty(container, javaClass<T>())