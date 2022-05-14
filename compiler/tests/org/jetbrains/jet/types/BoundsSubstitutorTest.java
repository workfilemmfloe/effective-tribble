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

package org.jetbrains.jet.types;

import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.lazy.KotlinTestWithEnvironment;
import org.jetbrains.jet.lang.resolve.lazy.LazyResolveTestUtil;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.BoundsSubstitutor;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.Collection;
import java.util.Collections;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

public class BoundsSubstitutorTest extends KotlinTestWithEnvironment {
    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_ONLY);
    }

    public void testSimpleSubstitution() throws Exception {
        doTest("fun <T> f(l: List<T>): T",
               "fun <T> f(l: kotlin.List<kotlin.Any?>): kotlin.Any?");
    }

    public void testParameterInBound() throws Exception {
        doTest("fun <T, R : List<T>> f(l: List<R>): R",
               "fun <T, R : kotlin.List<kotlin.Any?>> f(l: kotlin.List<kotlin.List<kotlin.Any?>>): kotlin.List<kotlin.Any?>");
    }

    public void testWithWhere() throws Exception {
        doTest("fun <T> f(l: List<T>): T where T : Any",
               "fun <T : kotlin.Any> f(l: kotlin.List<kotlin.Any>): kotlin.Any");
    }

    public void testWithWhereTwoBounds() throws Exception {
        doTest("fun <T, R> f(l: List<R>): R where T : List<R>, R : Any",
               "fun <T : kotlin.List<kotlin.Any>, R : kotlin.Any> f(l: kotlin.List<kotlin.Any>): kotlin.Any");
    }

    public void testWithWhereTwoBoundsReversed() throws Exception {
        doTest("fun <T, R> f(l: List<R>): R where T : Any, R : List<T>",
               "fun <T : kotlin.Any, R : kotlin.List<kotlin.Any>> f(l: kotlin.List<kotlin.List<kotlin.Any>>): kotlin.List<kotlin.Any>");
    }

    //public void testWithWhereTwoBoundsLoop() throws Exception {
    //    doTest("fun <T, R> f(l: List<R>): R where T : R, R : T",
    //           "");
    //}

    private void doTest(String text, String expected) {
        JetFile jetFile = JetPsiFactory(getProject()).createFile("fun.kt", text);
        ModuleDescriptor module = LazyResolveTestUtil.resolveLazily(Collections.singletonList(jetFile), getEnvironment());
        Collection<FunctionDescriptor> functions = module.getPackage(FqName.ROOT).getMemberScope().getFunctions(Name.identifier("f"));
        assert functions.size() == 1 : "Many functions defined";
        FunctionDescriptor function = ContainerUtil.getFirstItem(functions);

        FunctionDescriptor substituted = BoundsSubstitutor.substituteBounds(function);
        String actual = DescriptorRenderer.COMPACT.render(substituted);
        assertEquals(expected, actual);
    }
}
