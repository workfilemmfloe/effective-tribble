/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration.ui.notifications

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.formatter.KotlinFormatterUsageCollector

const val KOTLIN_UPDATE_CODE_STYLE_GROUP_ID = "Update Kotlin code style"

class KotlinCodeStyleNotification(formatterKind: KotlinFormatterUsageCollector.KotlinFormatterKind) :
    Notification(
        KOTLIN_UPDATE_CODE_STYLE_GROUP_ID,
        "Update Kotlin Code Style",
        "Want to try? $formatterKind",
        NotificationType.INFORMATION
    ) {
}

fun notifyNewKotlinStyleIfNeeded(project: Project) {
    val formatterKind = KotlinFormatterUsageCollector.getKotlinFormatterKind(project)
    KotlinCodeStyleNotification(formatterKind).notify(project)
}
