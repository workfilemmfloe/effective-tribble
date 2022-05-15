/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.ProjectTopics
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectBundle
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.configuration.ui.KotlinConfigurationCheckerComponent
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.versions.SuppressNotificationState
import org.jetbrains.kotlin.idea.versions.UnsupportedAbiVersionNotificationPanelProvider
import org.jetbrains.kotlin.idea.versions.createComponentActionLabel
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform

// Code is partially copied from com.intellij.codeInsight.daemon.impl.SetupSDKNotificationProvider
class KotlinSetupEnvironmentNotificationProvider(
        private val myProject: Project,
        notifications: EditorNotifications) : EditorNotifications.Provider<EditorNotificationPanel>() {

    init {
        myProject.messageBus.connect(myProject).subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                notifications.updateAllNotifications()
            }
        })
    }

    override fun getKey(): Key<EditorNotificationPanel> = KEY

    override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor): EditorNotificationPanel? {
        if (file.fileType != KotlinFileType.INSTANCE) {
            return null
        }

        val psiFile = PsiManager.getInstance(myProject).findFile(file) as? KtFile ?: return null
        if (psiFile.language !== KotlinLanguage.INSTANCE) {
            return null
        }

        val module = ModuleUtilCore.findModuleForPsiElement(psiFile) ?: return null
        if (!ModuleRootManager.getInstance(module).fileIndex.isInSourceContent(file)) {
            return null
        }

        if (ModuleRootManager.getInstance(module).sdk == null &&
            TargetPlatformDetector.getPlatform(psiFile) == JvmPlatform) {
            return createSetupSdkPanel(myProject, psiFile)
        }

        if (!KotlinConfigurationCheckerComponent.getInstance(module.project).isSyncing &&
            !SuppressNotificationState.isKotlinNotConfiguredSuppressed(module.toModuleGroup()) &&
            !hasAnyKotlinRuntimeInScope(module) &&
            UnsupportedAbiVersionNotificationPanelProvider.collectBadRoots(module).isEmpty()
        ) {
            return createKotlinNotConfiguredPanel(module)
        }

        return null
    }

    companion object {
        private val KEY = Key.create<EditorNotificationPanel>("Setup SDK")

        private fun createSetupSdkPanel(project: Project, file: PsiFile): EditorNotificationPanel {
            return EditorNotificationPanel().apply {
                setText(ProjectBundle.message("project.sdk.not.defined"))
                createActionLabel(ProjectBundle.message("project.sdk.setup")) {
                    ProjectSettingsService.getInstance(project).chooseAndSetSdk() ?: return@createActionLabel

                    runWriteAction {
                        val module = ModuleUtilCore.findModuleForPsiElement(file)
                        if (module != null) {
                            ModuleRootModificationUtil.setSdkInherited(module)
                        }
                    }
                }
            }
        }

        private fun createKotlinNotConfiguredPanel(module: Module): EditorNotificationPanel {
            return EditorNotificationPanel().apply {
                setText("Kotlin not configured")
                val configurators = getAbleToRunConfigurators(module).toList()
                if (!configurators.isEmpty()) {
                    createComponentActionLabel("Configure") { label ->
                        val singleConfigurator = configurators.singleOrNull()
                        if (singleConfigurator != null) {
                            singleConfigurator.apply(module.project)
                        }
                        else {
                            val configuratorsPopup = createConfiguratorsPopup(module.project, configurators)
                            configuratorsPopup.showUnderneathOf(label)
                        }
                    }

                    createComponentActionLabel("Ignore") {
                        SuppressNotificationState.suppressKotlinNotConfigured(module)
                        EditorNotifications.getInstance(module.project).updateAllNotifications()
                    }
                }
            }
        }

        private fun KotlinProjectConfigurator.apply(project: Project) {
            configure(project, emptyList())
            EditorNotifications.getInstance(project).updateAllNotifications()
            checkHideNonConfiguredNotifications(project)
        }

        fun createConfiguratorsPopup(project: Project, configurators: List<KotlinProjectConfigurator>): ListPopup {
            val step = object : BaseListPopupStep<KotlinProjectConfigurator>("Choose Configurator", configurators) {
                override fun getTextFor(value: KotlinProjectConfigurator?) = value?.presentableText ?: "<none>"

                override fun onChosen(selectedValue: KotlinProjectConfigurator?, finalChoice: Boolean): PopupStep<*>? {
                    return doFinalStep {
                        selectedValue?.apply(project)
                    }
                }
            }
            return JBPopupFactory.getInstance().createListPopup(step)
        }
    }
}
