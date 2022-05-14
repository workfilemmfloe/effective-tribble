/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.refactoring.changeSignature.usages

import org.jetbrains.jet.lang.psi.JetEnumEntry
import org.jetbrains.jet.plugin.refactoring.changeSignature.JetChangeInfo
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.psiUtil.getParentByType
import org.jetbrains.jet.lang.psi.JetClass
import org.jetbrains.jet.lang.psi.JetDelegatorToSuperCall

public class JetEnumEntryWithoutSuperCallUsage(enumEntry: JetEnumEntry) : JetUsageInfo<JetEnumEntry>(enumEntry) {
    override fun processUsage(changeInfo: JetChangeInfo, element: JetEnumEntry): Boolean {
        if (changeInfo.getNewParameters().size > 0) {
            val psiFactory = JetPsiFactory(element)

            val enumClass = element.getParentByType(javaClass<JetClass>(), true)!!
            val delegatorToSuperCall = element.addAfter(
                    psiFactory.createDelegatorToSuperCall("${enumClass.getName()}()"),
                    element.getNameAsDeclaration()
            ) as JetDelegatorToSuperCall
            element.addBefore(psiFactory.createColon(), delegatorToSuperCall)

            return JetFunctionCallUsage(delegatorToSuperCall, enumClass, false).processUsage(changeInfo, delegatorToSuperCall)
        }

        return true
    }
}