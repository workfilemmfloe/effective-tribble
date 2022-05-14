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

package org.jetbrains.jet.completion;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import kotlin.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.completion.util.UtilPackage;
import org.jetbrains.jet.plugin.JetLightCodeInsightFixtureTestCase;
import org.jetbrains.jet.plugin.caches.resolve.LibraryModificationTracker;
import org.jetbrains.jet.plugin.project.TargetPlatform;

import java.io.File;

public abstract class JetFixtureCompletionBaseTestCase extends JetLightCodeInsightFixtureTestCase {
    private Pair<Boolean, Boolean> savedAutoCompleteSettings;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        savedAutoCompleteSettings = setAutoCompleteSetting(new Pair<Boolean, Boolean>(false, false));
    }

    @Override
    protected void tearDown() throws Exception {
        setAutoCompleteSetting(savedAutoCompleteSettings);

        super.tearDown();
    }

    private static Pair<Boolean, Boolean> setAutoCompleteSetting(Pair<Boolean, Boolean> value){
        CodeInsightSettings settings = CodeInsightSettings.getInstance();
        boolean oldValue1 = settings.AUTOCOMPLETE_ON_CODE_COMPLETION;
        boolean oldValue2 = settings.AUTOCOMPLETE_ON_SMART_TYPE_COMPLETION;
        settings.AUTOCOMPLETE_ON_CODE_COMPLETION = value.first;
        settings.AUTOCOMPLETE_ON_SMART_TYPE_COMPLETION = value.second;
        return new Pair<Boolean, Boolean>(oldValue1, oldValue2);
    }

    public abstract TargetPlatform getPlatform();

    @Nullable
    protected abstract LookupElement[] complete(int invocationCount);

    protected int defaultInvocationCount() { return 0; }

    public void doTest(String testPath) throws Exception {
        setUpFixture(testPath);

        String fileText = FileUtil.loadFile(new File(testPath), true);
        UtilPackage.testCompletion(fileText, getPlatform(), new Function1<Integer, LookupElement[]>() {
            @Override
            public LookupElement[] invoke(Integer invocationCount) {
                return complete(invocationCount);
            }
        }, defaultInvocationCount());
    }

    protected void setUpFixture(@NotNull String testPath) {
        //TODO: this is a hacky workaround for js second completion tests failing with PsiInvalidElementAccessException
        LibraryModificationTracker.getInstance(getProject()).incModificationCount();

        myFixture.configureByFile(testPath);
    }
}
