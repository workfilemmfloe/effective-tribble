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

package org.jetbrains.jet.plugin.libraries;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.JetWithJdkAndRuntimeLightProjectDescriptor;

import java.io.File;

public class NavigateToStdlibSourceRegressionTest extends NavigateToLibraryRegressionTest {
    /**
     * Regression test against KT-3186
     */
    public void testRefToAssertEquals() {
        PsiElement navigationElement = configureAndResolve("import kotlin.test.assertEquals; val x = <caret>assertEquals(1, 2)");
        assertEquals("Test.kt", navigationElement.getContainingFile().getName());
    }

    @Override
    protected void tearDown() throws Exception {
        // Workaround for IDEA's bug during tests.
        // After tests IDEA disposes VirtualFiles within LocalFileSystem, but doesn't rebuild indices.
        // This causes library source files to be impossible to find via indices
        super.tearDown();
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                LightPlatformTestCase.closeAndDeleteProject();
            }
        });
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return ProjectDescriptorWithStdlibSources.INSTANCE;
    }

    private static class ProjectDescriptorWithStdlibSources extends JetWithJdkAndRuntimeLightProjectDescriptor {
        public static final ProjectDescriptorWithStdlibSources INSTANCE = new ProjectDescriptorWithStdlibSources();

        @Override
        public void configureModule(@NotNull Module module, @NotNull ModifiableRootModel model, @Nullable ContentEntry contentEntry) {
            super.configureModule(module, model, contentEntry);

            Library library = model.getModuleLibraryTable().getLibraryByName("myLibrary");
            assert library != null;
            Library.ModifiableModel modifiableModel = library.getModifiableModel();
            modifiableModel.addRoot(VfsUtil.getUrlForLibraryRoot(new File("libraries/stdlib/src")), OrderRootType.SOURCES);
            modifiableModel.commit();
        }
    }
}
