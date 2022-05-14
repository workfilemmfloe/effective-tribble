/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.android.configure

import com.android.tools.idea.gradle.AndroidProjectKeys
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import org.jetbrains.kotlin.idea.inspections.gradle.KotlinPlatformGradleDetector

class PlatformAndroidGradleDetector : KotlinPlatformGradleDetector {
    override fun getResolvedKotlinStdlibVersionByModuleData(moduleData: DataNode<*>, libraryIds: List<String>): String? {
        ExternalSystemApiUtil
                .findAllRecursively(moduleData, AndroidProjectKeys.JAVA_PROJECT).asSequence()
                .flatMap { it.data.jarLibraryDependencies.asSequence() }
                .forEach {
                    val libraryName = it.name
                    if (libraryName.substringBeforeLast("-") in libraryIds) return libraryName.substringAfterLast("-")
                }
        return null
    }
}