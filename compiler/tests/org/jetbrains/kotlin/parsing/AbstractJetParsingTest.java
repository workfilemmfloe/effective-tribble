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

package org.jetbrains.kotlin.parsing;

import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.util.Getter;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.ParsingTestCase;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.JetNodeTypes;
import org.jetbrains.kotlin.psi.IfNotParsed;
import org.jetbrains.kotlin.psi.JetElement;
import org.jetbrains.kotlin.psi.JetPsiFactory;
import org.jetbrains.kotlin.psi.JetVisitorVoid;
import org.jetbrains.kotlin.test.JetTestUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.jetbrains.kotlin.psi.PsiPackage.JetPsiFactory;

public abstract class AbstractJetParsingTest extends ParsingTestCase {
    static {
        System.setProperty("idea.platform.prefix", "Idea");
    }

    @Override
    protected String getTestDataPath() {
        return JetTestUtils.getHomeDirectory();
    }

    public AbstractJetParsingTest() {
        super(".", "kt", new JetParserDefinition());
    }

    private static void checkPsiGetters(JetElement elem) throws Throwable {
        Method[] methods = elem.getClass().getDeclaredMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            if (!methodName.startsWith("get") && !methodName.startsWith("find") ||
                methodName.equals("getReference") ||
                methodName.equals("getReferences") ||
                methodName.equals("getUseScope") ||
                methodName.equals("getPresentation")) {
                continue;
            }

            if (!Modifier.isPublic(method.getModifiers())) continue;
            if (method.getParameterTypes().length > 0) continue;

            Class<?> declaringClass = method.getDeclaringClass();
            if (!declaringClass.getName().startsWith("org.jetbrains.kotlin")) continue;

            Object result = method.invoke(elem);
            if (result == null) {
                for (Annotation annotation : method.getDeclaredAnnotations()) {
                    if (annotation instanceof IfNotParsed) {
                        assertNotNull(
                                "Incomplete operation in parsed OK test, method " + methodName +
                                " in " + declaringClass.getSimpleName() + " returns null. Element text: \n" + elem.getText(),
                                PsiTreeUtil.findChildOfType(elem, PsiErrorElement.class));
                    }
                }
            }
        }
    }

    protected void doParsingTest(@NotNull String filePath) throws Exception {
        doBaseTest(filePath, JetNodeTypes.JET_FILE);
    }

    protected void doExpressionCodeFragmentParsingTest(@NotNull String filePath) throws Exception {
        doBaseTest(filePath, JetNodeTypes.EXPRESSION_CODE_FRAGMENT);
    }

    protected void doBlockCodeFragmentParsingTest(@NotNull String filePath) throws Exception {
        doBaseTest(filePath, JetNodeTypes.BLOCK_CODE_FRAGMENT);
    }

    private void doBaseTest(@NotNull String filePath, @NotNull IElementType fileType) throws Exception {
        myFileExt = FileUtilRt.getExtension(PathUtil.getFileName(filePath));
        myFile = createFile(filePath, fileType);

        myFile.acceptChildren(new JetVisitorVoid() {
            @Override
            public void visitJetElement(@NotNull JetElement element) {
                element.acceptChildren(this);
                try {
                    checkPsiGetters(element);
                }
                catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            }
        });

        doCheckResult(myFullDataPath, filePath.replaceAll("\\.kts?", ".txt"), toParseTreeText(myFile, false, false).trim());
    }

    private PsiFile createFile(@NotNull String filePath, @NotNull IElementType fileType) throws Exception {
        JetPsiFactory psiFactory = JetPsiFactory(myProject);
        if (fileType == JetNodeTypes.EXPRESSION_CODE_FRAGMENT) {
            return psiFactory.createExpressionCodeFragment(loadFile(filePath), null);
        }
        else if (fileType == JetNodeTypes.BLOCK_CODE_FRAGMENT) {
            return psiFactory.createBlockCodeFragment(loadFile(filePath), null);
        }
        else {
            return createPsiFile(FileUtil.getNameWithoutExtension(PathUtil.getFileName(filePath)), loadFile(filePath));
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        // Temp workaround for setting FileTypeRegistry.ourInstanceGetter to CoreFileTypeRegistry forever
        // Reproducible with pattern test kind:
        // org.jetbrains.kotlin.parsing.JetParsingTestGenerated$Psi||org.jetbrains.kotlin.codegen.TestlibTest||org.jetbrains.kotlin.completion.MultiFileJvmBasicCompletionTestGenerated

        //noinspection AssignmentToStaticFieldFromInstanceMethod
        FileTypeRegistry.ourInstanceGetter = new Getter<FileTypeRegistry>() {
            @Override
            public FileTypeRegistry get() {
                return FileTypeManager.getInstance();
            }
        };
    }
}
