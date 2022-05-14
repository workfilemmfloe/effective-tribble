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

package org.jetbrains.jet.plugin.actions;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ex.MessagesEx;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.j2k.Converter;
import org.jetbrains.jet.j2k.visitors.ClassVisitor;

import java.io.IOException;
import java.util.*;

public class JavaToKotlinActionUtil {

    static void setClassIdentifiers(@NotNull Converter converter, @NotNull PsiFile psiFile) {
        ClassVisitor c = new ClassVisitor();
        psiFile.accept(c);
        converter.setClassIdentifiers(c.getClassIdentifiers());
    }

    @NotNull
    private static List<VirtualFile> getChildrenRecursive(@Nullable VirtualFile baseDir) {
        List<VirtualFile> result = new LinkedList<VirtualFile>();
        VirtualFile[] children = baseDir != null ? baseDir.getChildren() : VirtualFile.EMPTY_ARRAY;
        result.addAll(Arrays.asList(children));
        for (VirtualFile f : children)
            result.addAll(getChildrenRecursive(f));
        return result;
    }

    @NotNull
    /*package*/ static List<PsiFile> getAllJavaFiles(@NotNull VirtualFile[] vFiles, Project project) {
        Set<VirtualFile> filesSet = allVirtualFiles(vFiles);
        PsiManager manager = PsiManager.getInstance(project);
        List<PsiFile> res = new ArrayList<PsiFile>();
        for (VirtualFile file : filesSet) {
            PsiFile psiFile = manager.findFile(file);
            if (psiFile != null && psiFile.getFileType() instanceof JavaFileType) {
                res.add(psiFile);
            }
        }
        return res;
    }

    @NotNull
    public static Set<VirtualFile> allVirtualFiles(@NotNull VirtualFile[] vFiles) {
        Set<VirtualFile> filesSet = new HashSet<VirtualFile>();
        for (VirtualFile f : vFiles) {
            filesSet.add(f);
            filesSet.addAll(getChildrenRecursive(f));
        }
        return filesSet;
    }

    static void reformatFiles(List<VirtualFile> allJetFiles, final Project project) {
        for (final VirtualFile vf : allJetFiles)
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    if (vf != null) {
                        PsiFile psiFile = PsiManager.getInstance(project).findFile(vf);
                        if (psiFile != null) {
                            CodeStyleManager.getInstance(project).reformat(psiFile);
                        }
                    }
                }
            });
    }

    @NotNull
    static List<VirtualFile> convertFiles(final Converter converter, List<PsiFile> allJavaFilesNear) {
        final List<VirtualFile> result = new LinkedList<VirtualFile>();
        for (final PsiFile f : allJavaFilesNear) {
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    VirtualFile vf = convertOneFile(converter, f);
                    if (vf != null) {
                        result.add(vf);
                    }
                }
            });
        }
        return result;
    }

    static void deleteFiles(List<PsiFile> allJavaFilesNear) {
        for (final PsiFile f : allJavaFilesNear) {
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    try {
                        PsiManager manager = f.getManager();
                        VirtualFile vFile = f.getVirtualFile();
                        if (vFile != null) {
                            vFile.delete(manager);
                        }
                    } catch (IOException ignored) {
                    }
                }
            });
        }
    }

    @Nullable
    private static VirtualFile convertOneFile(Converter converter, PsiFile psiFile) {
        try {
            VirtualFile virtualFile = psiFile.getVirtualFile();
            if (psiFile instanceof PsiJavaFile && virtualFile != null) {
                String result = "";
                try {
                    result = converter.convertFile((PsiJavaFile) psiFile).toKotlin();
                } catch (Exception e) {
                    //noinspection CallToPrintStackTrace
                    e.printStackTrace();
                }
                PsiManager manager = psiFile.getManager();
                assert manager != null;
                VirtualFile copy = virtualFile.copy(manager, virtualFile.getParent(), virtualFile.getNameWithoutExtension() + ".kt");
                copy.setBinaryContent(CharsetToolkit.getUtf8Bytes(result));
                return copy;
            }
        } catch (Exception ex) {
            MessagesEx.error(psiFile.getProject(), ex.getMessage()).showLater();
        }
        return null;
    }

    static void renameFiles(@NotNull List<PsiFile> psiFiles) {
        for (final PsiFile f : psiFiles) {
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    try {
                        PsiManager manager = f.getManager();
                        VirtualFile vFile = f.getVirtualFile();
                        if (vFile != null) {
                            vFile.copy(manager, vFile.getParent(), vFile.getNameWithoutExtension() + ".java.old");
                            vFile.delete(manager);
                        }
                    } catch (IOException ignored) {
                    }
                }
            });
        }
    }
}
