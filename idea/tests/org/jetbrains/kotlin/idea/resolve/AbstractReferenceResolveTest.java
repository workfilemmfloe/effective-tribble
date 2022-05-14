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

package org.jetbrains.kotlin.idea.resolve;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.intellij.ide.startup.impl.StartupManagerImpl;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.vfs.newvfs.impl.VirtualDirectoryImpl;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.file.impl.FileManager;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.JetWithJdkAndRuntimeLightProjectDescriptor;
import org.jetbrains.kotlin.idea.references.BuiltInsReferenceResolver;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.JetTestUtils;
import org.jetbrains.kotlin.test.ReferenceUtils;
import org.jetbrains.kotlin.test.util.UtilPackage;
import org.junit.Assert;

import java.util.List;
import java.util.Set;

public abstract class AbstractReferenceResolveTest extends LightPlatformCodeInsightFixtureTestCase {
    public static class ExpectedResolveData {
        private final Boolean shouldBeUnresolved;
        private final String referenceToString;

        public ExpectedResolveData(Boolean shouldBeUnresolved, String referenceToString) {
            this.shouldBeUnresolved = shouldBeUnresolved;
            this.referenceToString = referenceToString;
        }

        public boolean shouldBeUnresolved() {
            return shouldBeUnresolved;
        }

        public String getReferenceString() {
            return referenceToString;
        }
    }

    public static final String MULTIRESOLVE = "MULTIRESOLVE";
    public static final String REF_EMPTY = "REF_EMPTY";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ((StartupManagerImpl) StartupManager.getInstance(getProject())).runPostStartupActivities();
        VirtualDirectoryImpl.allowRootAccess(JetTestUtils.getHomeDirectory());
    }

    @Override
    protected void tearDown() throws Exception {
        VirtualDirectoryImpl.disallowRootAccess(JetTestUtils.getHomeDirectory());

        Set<JetFile> builtInsSources = getProject().getComponent(BuiltInsReferenceResolver.class).getBuiltInsSources();
        FileManager fileManager = ((PsiManagerEx) PsiManager.getInstance(getProject())).getFileManager();

        super.tearDown();

        // Restore mapping between PsiFiles and VirtualFiles dropped in FileManager.cleanupForNextTest(),
        // otherwise built-ins psi elements will become invalid in next test.
        for (JetFile source : builtInsSources) {
            FileViewProvider provider = source.getViewProvider();
            fileManager.setViewProvider(provider.getVirtualFile(), provider);
        }
    }

    protected void doTest(@NotNull String path) {
        assert path.endsWith(".kt") : path;
        UtilPackage.configureWithExtraFile(myFixture, path, ".Data");
        performChecks();
    }

    protected void performChecks() {
        if (InTextDirectivesUtils.isDirectiveDefined(myFixture.getFile().getText(), MULTIRESOLVE)) {
            doMultiResolveTest();
        }
        else {
            doSingleResolveTest();
        }
    }

    protected void doSingleResolveTest() {
        ExpectedResolveData expectedResolveData = readResolveData(myFixture.getFile().getText());

        int offset = myFixture.getEditor().getCaretModel().getOffset();
        PsiReference psiReference = myFixture.getFile().findReferenceAt(offset);

        checkReferenceResolve(expectedResolveData, offset, psiReference);
    }

    protected void doMultiResolveTest() {
        List<String> expectedReferences = ReferenceUtils.getExpectedReferences(myFixture.getFile().getText());

        PsiReference psiReference = myFixture.getFile().findReferenceAt(myFixture.getEditor().getCaretModel().getOffset());

        assertTrue(psiReference instanceof PsiPolyVariantReference);

        PsiPolyVariantReference variantReference = (PsiPolyVariantReference) psiReference;

        ResolveResult[] results = variantReference.multiResolve(true);

        List<String> actualResolvedTo = Lists.newArrayList();
        for (ResolveResult result : results) {
            PsiElement resolvedToElement = result.getElement();
            assertNotNull(resolvedToElement);

            actualResolvedTo.add(ReferenceUtils.renderAsGotoImplementation(resolvedToElement));
        }

        assertOrderedEquals(Ordering.natural().sortedCopy(actualResolvedTo), Ordering.natural().sortedCopy(expectedReferences));
    }

    @NotNull
    public static ExpectedResolveData readResolveData(String fileText) {
        boolean shouldBeUnresolved = InTextDirectivesUtils.isDirectiveDefined(fileText, REF_EMPTY);
        List<String> refs = ReferenceUtils.getExpectedReferences(fileText);

        String referenceToString;
        if (shouldBeUnresolved) {
            Assert.assertTrue("REF: directives will be ignored for " + REF_EMPTY + " test: " + refs, refs.isEmpty());
            referenceToString = "<empty>";
        }
        else {
            assertTrue("Must be a single ref: " + refs + ".\n" +
                       "Use " + MULTIRESOLVE + " if you need multiple refs\n" +
                       "Use "+ REF_EMPTY + " for an unresolved reference",
                       refs.size() == 1);
            referenceToString = refs.get(0);
            Assert.assertNotNull("Test data wasn't found, use \"// REF: \" directive", referenceToString);
        }

        return new ExpectedResolveData(shouldBeUnresolved, referenceToString);
    }

    public static void checkReferenceResolve(ExpectedResolveData expectedResolveData, int offset, PsiReference psiReference) {
        if (psiReference != null) {
            PsiElement resolvedTo = psiReference.resolve();
            if (resolvedTo != null) {
                String resolvedToElementStr = ReferenceUtils.renderAsGotoImplementation(resolvedTo);
                String notEqualMessage = String.format("Found reference to '%s', but '%s' was expected",
                                                       resolvedToElementStr, expectedResolveData.getReferenceString());
                assertEquals(notEqualMessage, expectedResolveData.getReferenceString(), resolvedToElementStr);
            }
            else {
                if (!expectedResolveData.shouldBeUnresolved()) {
                    Assert.assertNull(
                            String.format("Element %s wasn't resolved to anything, but %s was expected",
                                          psiReference, expectedResolveData.getReferenceString()),
                            expectedResolveData.getReferenceString());
                }
            }
        }
        else {
            Assert.assertNull(
                    String.format("No reference found at offset: %s, but one resolved to %s was expected",
                                  offset, expectedResolveData.getReferenceString()),
                    expectedResolveData.getReferenceString());
        }
    }

    @Nullable
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE;
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return "./";
    }
}
