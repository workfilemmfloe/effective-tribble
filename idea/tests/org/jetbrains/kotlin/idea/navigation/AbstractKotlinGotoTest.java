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

package org.jetbrains.kotlin.idea.navigation;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.intellij.ide.util.gotoByName.FilteringGotoByModel;
import com.intellij.ide.util.gotoByName.GotoClassModel2;
import com.intellij.ide.util.gotoByName.GotoSymbolModel2;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.UsefulTestCase;
import kotlin.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.test.JetLightCodeInsightFixtureTestCase;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.ReferenceUtils;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractKotlinGotoTest extends JetLightCodeInsightFixtureTestCase {
    protected void doSymbolTest(String path) {
        myFixture.configureByFile(path);
        assertGotoSymbol(new GotoSymbolModel2(getProject()), myFixture.getEditor());
    }

    protected void doClassTest(String path) {
        myFixture.configureByFile(path);
        assertGotoSymbol(new GotoClassModel2(getProject()), myFixture.getEditor());
    }

    private String dirPath = null;

    @Override
    protected void setUp() {
        TestMetadata annotation = getClass().getAnnotation(TestMetadata.class);
        dirPath = annotation.value();

        super.setUp();
    }

    @Override
    protected void tearDown() {
        super.tearDown();
        dirPath = null;
    }

    @Override
    protected String getTestDataPath() {
        return dirPath;
    }

    @NotNull
    @Override
    protected String fileName() {
        return getTestName(true) + ".kt";
    }

    private static void assertGotoSymbol(@NotNull FilteringGotoByModel<Language> model, @NotNull Editor editor) {
        String documentText = editor.getDocument().getText();
        List<String> searchTextList = InTextDirectivesUtils.findListWithPrefixes(documentText, "// SEARCH_TEXT:");
        Assert.assertFalse("There's no search text in test data file given. Use '// SEARCH_TEXT:' directive",
                           searchTextList.isEmpty());

        List<String> expectedReferences = CollectionsKt.map(
                InTextDirectivesUtils.findLinesWithPrefixesRemoved(documentText, "// REF:"),
                new Function1<String, String>() {
                    @Override
                    public String invoke(String input) {
                        return input.trim();
                    }
                }
        );
        boolean enableCheckbox = InTextDirectivesUtils.isDirectiveDefined(documentText, "// CHECK_BOX");

        String searchText = searchTextList.get(0);

        List<Object> elementsByName = new ArrayList<Object>();

        String[] names = model.getNames(enableCheckbox);
        for (String name : names) {
            if (name != null && name.startsWith(searchText)) {
                elementsByName.addAll(Arrays.asList(model.getElementsByName(name, enableCheckbox, name + "*")));
            }
        }

        List<String> renderedElements = Lists.transform(elementsByName, new Function<Object, String>() {
            @Override
            public String apply(@Nullable Object element) {
                Assert.assertNotNull(element);
                Assert.assertTrue(element instanceof PsiElement);
                return ReferenceUtils.renderAsGotoImplementation((PsiElement) element);
            }
        });

        UsefulTestCase.assertOrderedEquals(Ordering.natural().sortedCopy(renderedElements), expectedReferences);
    }
}
