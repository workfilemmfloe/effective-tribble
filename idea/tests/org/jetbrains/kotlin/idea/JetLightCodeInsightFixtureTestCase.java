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

package org.jetbrains.kotlin.idea;

import com.intellij.ide.startup.impl.StartupManagerImpl;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.newvfs.impl.VirtualDirectoryImpl;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.file.impl.FileManager;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.actions.internal.KotlinInternalMode;
import org.jetbrains.kotlin.idea.references.BuiltInsReferenceResolver;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.JetTestUtils;
import org.jetbrains.kotlin.utils.UtilsPackage;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public abstract class JetLightCodeInsightFixtureTestCase extends LightCodeInsightFixtureTestCase {
    private boolean kotlinInternalModeOriginalValue;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ((StartupManagerImpl) StartupManager.getInstance(getProject())).runPostStartupActivities();
        VirtualDirectoryImpl.allowRootAccess(JetTestUtils.getHomeDirectory());

        kotlinInternalModeOriginalValue = KotlinInternalMode.Instance.getEnabled();
        KotlinInternalMode.Instance.setEnabled(true);
    }

    @Override
    protected void tearDown() throws Exception {
        KotlinInternalMode.Instance.setEnabled(kotlinInternalModeOriginalValue);
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

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return getProjectDescriptorFromFileDirective();
    }

    protected LightProjectDescriptor getProjectDescriptorFromTestName() {
        String testName = StringUtil.toLowerCase(getTestName(false));

        if (testName.endsWith("runtime")) {
            return JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE;
        }
        else if (testName.endsWith("stdlib")) {
            return ProjectDescriptorWithStdlibSources.INSTANCE;
        }

        return JetLightProjectDescriptor.INSTANCE;
    }

    protected LightProjectDescriptor getProjectDescriptorFromFileDirective() {
        if (!isAllFilesPresentInTest()) {
            try {
                String fileText = FileUtil.loadFile(new File(getTestDataPath(), fileName()), true);

                List<String> withLibraryDirective = InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, "WITH_LIBRARY:");
                if (!withLibraryDirective.isEmpty()) {
                    return new JdkAndMockLibraryProjectDescriptor(PluginTestCaseBase.getTestDataPathBase() + "/" + withLibraryDirective.get(0), true);
                }
                else if (InTextDirectivesUtils.isDirectiveDefined(fileText, "RUNTIME")) {
                    return JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE;
                }
                else if (InTextDirectivesUtils.isDirectiveDefined(fileText, "RUNTIME_WITH_SOURCES")) {
                    return ProjectDescriptorWithStdlibSources.INSTANCE;
                }
                else if (InTextDirectivesUtils.isDirectiveDefined(fileText, "JS")) {
                    return JetStdJSProjectDescriptor.INSTANCE;
                }
            }
            catch (IOException e) {
                throw UtilsPackage.rethrow(e);
            }
        }

        return JetLightProjectDescriptor.INSTANCE;
    }

    protected boolean isAllFilesPresentInTest() {
        return getTestName(false).startsWith("AllFilesPresentIn");
    }

    @NotNull
    protected String fileName() {
        return getTestName(false) + ".kt";
    }

    protected boolean performNotWriteEditorAction(String actionId) {
        DataContext dataContext = ((EditorEx)myFixture.getEditor()).getDataContext();

        ActionManagerEx managerEx = ActionManagerEx.getInstanceEx();
        AnAction action = managerEx.getAction(actionId);
        AnActionEvent event = new AnActionEvent(null, dataContext, ActionPlaces.UNKNOWN, new Presentation(), managerEx, 0);

        action.update(event);
        if (!event.getPresentation().isEnabled()) {
            return false;
        }

        managerEx.fireBeforeActionPerformed(action, dataContext, event);
        action.actionPerformed(event);

        managerEx.fireAfterActionPerformed(action, dataContext, event);
        return true;
    }
}
