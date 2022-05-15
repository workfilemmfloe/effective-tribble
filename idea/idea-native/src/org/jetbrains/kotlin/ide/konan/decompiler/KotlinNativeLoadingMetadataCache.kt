/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan.decompiler

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.vfs.*
import com.intellij.util.containers.ContainerUtil.createConcurrentSoftValueMap
import org.jetbrains.kotlin.metadata.konan.KonanProtoBuf
import org.jetbrains.kotlin.serialization.konan.parseModuleHeader
import org.jetbrains.kotlin.serialization.konan.parsePackageFragment

class KotlinNativeLoadingMetadataCache : ApplicationComponent {

    companion object {
        @JvmStatic
        fun getInstance(): KotlinNativeLoadingMetadataCache =
            ApplicationManager.getApplication().getComponent(KotlinNativeLoadingMetadataCache::class.java)
    }

    private val packageFragmentCache = createConcurrentSoftValueMap<VirtualFile, KonanProtoBuf.LinkDataPackageFragment>()
    private val moduleHeaderCache = createConcurrentSoftValueMap<VirtualFile, KonanProtoBuf.LinkDataLibrary>()

    fun getCachedPackageFragment(virtualFile: VirtualFile): KonanProtoBuf.LinkDataPackageFragment =
        packageFragmentCache.computeIfAbsent(virtualFile) {
            parsePackageFragment(virtualFile.contentsToByteArray(false))
        }

    fun getCachedModuleHeader(virtualFile: VirtualFile): KonanProtoBuf.LinkDataLibrary =
        moduleHeaderCache.computeIfAbsent(virtualFile) {
            parseModuleHeader(virtualFile.contentsToByteArray(false))
        }

    override fun initComponent() {
        VirtualFileManager.getInstance().addVirtualFileListener(object : VirtualFileListener {
            override fun fileCreated(event: VirtualFileEvent) = invalidateCaches(event.file)
            override fun fileDeleted(event: VirtualFileEvent) = invalidateCaches(event.file)
            override fun fileMoved(event: VirtualFileMoveEvent) = invalidateCaches(event.file)
            override fun contentsChanged(event: VirtualFileEvent) = invalidateCaches(event.file)
            override fun propertyChanged(event: VirtualFilePropertyEvent) = invalidateCaches(event.file)
        })
    }

    private fun invalidateCaches(virtualFile: VirtualFile) {
        packageFragmentCache.remove(virtualFile)
        moduleHeaderCache.remove(virtualFile)
    }
}
