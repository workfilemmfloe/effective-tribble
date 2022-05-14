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

package org.jetbrains.kotlin.js.test.semantics;

public final class InvokeConventionTest extends AbstractExpressionTest {

    public InvokeConventionTest() {
        super("invoke/");
    }

    public void testInvokeMethod() throws Exception {
        checkFooBoxIsOk();
    }

    public void testExplicitInvokeLambda() throws Exception {
        checkFooBoxIsOk();
    }

    public void testInvokeOnExprByConvention() throws Exception {
        checkFooBoxIsOk();
    }

    public void testInvokeInExtensionFunctionLiteral() throws Exception {
        fooBoxTest();
    }

    public void testInvokeInFunctionLiteral() throws Exception {
        fooBoxTest();
    }

    public void testInvokeWithExtensionReceiver() throws Exception {
        fooBoxTest();
    }

    public void testInvokeWithDispatchReceiver() throws Exception {
        fooBoxTest();
    }

    public void testInvokeWithDispatchAndExtensionReceivers() throws Exception {
        fooBoxTest();
    }

    public void testInfixCall() throws Exception {
        checkFooBoxIsOk();
    }

    public void testInheritFromFunctionTraits() throws Exception {
        checkFooBoxIsOk();
    }

    public void testInvokeWithImplicitDispatchReceiverAndExtensionReceiver() throws Exception {
        checkFooBoxIsOk();
    }
}
