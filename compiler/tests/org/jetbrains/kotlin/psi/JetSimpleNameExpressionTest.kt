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

package org.jetbrains.kotlin.psi

import org.junit.Assert
import org.jetbrains.kotlin.test.JetLiteFixture
import org.jetbrains.kotlin.cli.jvm.compiler.JetCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles

public class JetSimpleNameExpressionTest : JetLiteFixture() {
    public fun testGetReceiverExpressionIdentifier() {
        // Binary Expressions
        assertReceiver("1 + 2", "1")
        assertReceiver("1 in array(1)", "array(1)")
        assertReceiver("1 !in array(1)", "array(1)")
        assertReceiver("1 to 2", "1")
    }
    private fun assertReceiver(exprString: String, expected: String) {
        val expression = JetPsiFactory(getProject()).createExpression(exprString) as JetBinaryExpression
        Assert.assertEquals(expected, expression.getOperationReference().getReceiverExpression()!!.getText())
    }
    override fun createEnvironment(): JetCoreEnvironment {
        return JetCoreEnvironment.createForTests(getTestRootDisposable()!!, CompilerConfiguration(), EnvironmentConfigFiles.JVM_CONFIG_FILES)
    }
}
