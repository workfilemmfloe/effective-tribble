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

package org.jetbrains.jet.plugin.framework;

import com.intellij.framework.library.LibraryVersionProperties;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.JarVersionDetectionUtil;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryPresentationProvider;
import com.intellij.openapi.vfs.JarFile;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.JetIcons;
import org.jetbrains.jet.utils.PathUtil;

import javax.swing.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class JavaRuntimePresentationProvider extends LibraryPresentationProvider<LibraryVersionProperties> {
    public static JavaRuntimePresentationProvider getInstance() {
        return LibraryPresentationProvider.EP_NAME.findExtension(JavaRuntimePresentationProvider.class);
    }

    protected JavaRuntimePresentationProvider() {
        super(JavaRuntimeLibraryDescription.KOTLIN_JAVA_RUNTIME_KIND);
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return JetIcons.SMALL_LOGO_13;
    }

    @Nullable
    @Override
    public LibraryVersionProperties detect(@NotNull List<VirtualFile> classesRoots) {
        VirtualFile stdJar = getRuntimeJar(classesRoots);
        if (stdJar != null) {
            try {
                JarFile zipFile = JarFileSystem.getInstance().getJarFile(stdJar);
                String version = JarVersionDetectionUtil.detectJarVersion(zipFile);
                return new LibraryVersionProperties(version);
            }
            catch (IOException e) {
                return null;
            }
        }

        return null;
    }

    @Nullable
    public static VirtualFile getRuntimeJar(@NotNull List<VirtualFile> classesRoots) {
        return getJarFile(classesRoots, PathUtil.KOTLIN_JAVA_RUNTIME_JAR);
    }

    @Nullable
    public static VirtualFile getRuntimeSrcJar(@NotNull List<VirtualFile> classesRoots) {
        return getJarFile(classesRoots, PathUtil.KOTLIN_JAVA_RUNTIME_SRC_JAR);
    }

    private static VirtualFile getJarFile(@NotNull List<VirtualFile> classesRoots, @NotNull String jarName) {
        for (VirtualFile root : classesRoots) {
            if (root.getName().equals(jarName)) {
                return root;
            }
        }

        return null;
    }

    @Nullable
    public static VirtualFile getRuntimeJar(@NotNull Library library) {
        return getRuntimeJar(Arrays.asList(library.getFiles(OrderRootType.CLASSES)));
    }

    @Nullable
    public static VirtualFile getRuntimeSrcJar(@NotNull Library library) {
        return getRuntimeSrcJar(Arrays.asList(library.getFiles(OrderRootType.SOURCES)));
    }
}
