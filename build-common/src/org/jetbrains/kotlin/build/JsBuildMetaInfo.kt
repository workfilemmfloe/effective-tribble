/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build

import org.jetbrains.kotlin.utils.JsMetadataVersion

/**
 * If you want to add a new field, check its type is supported by [serializeToPlainText], [deserializeFromPlainText]
 */
data class JsBuildMetaInfo(
    override val isEAP: Boolean,
    override val compilerBuildVersion: String,
    override val languageVersionString: String,
    override val apiVersionString: String,
    override val multiplatformEnable: Boolean,
    override val metadataVersionMajor: Int,
    override val metadataVersionMinor: Int,
    override val metadataVersionPatch: Int,
    override val ownVersion: Int,
    override val coroutinesVersion: Int,
    override val multiplatformVersion: Int,
    override val pluginClasspaths: String
) : BuildMetaInfo {
    companion object : BuildMetaInfoFactory<JsBuildMetaInfo>(JsBuildMetaInfo::class) {
        override fun create(
            isEAP: Boolean,
            compilerBuildVersion: String,
            languageVersionString: String,
            apiVersionString: String,
            multiplatformEnable: Boolean,
            ownVersion: Int,
            coroutinesVersion: Int,
            multiplatformVersion: Int,
            metadataVersionArray: IntArray?,
            pluginClasspaths: String
        ): JsBuildMetaInfo {
            val metadataVersion = metadataVersionArray?.let(::JsMetadataVersion) ?: JsMetadataVersion.INSTANCE
            return JsBuildMetaInfo(
                isEAP = isEAP,
                compilerBuildVersion = compilerBuildVersion,
                languageVersionString = languageVersionString,
                apiVersionString = apiVersionString,
                multiplatformEnable = multiplatformEnable,
                metadataVersionMajor = metadataVersion.major,
                metadataVersionMinor = metadataVersion.minor,
                metadataVersionPatch = metadataVersion.patch,
                ownVersion = ownVersion,
                coroutinesVersion = coroutinesVersion,
                multiplatformVersion = multiplatformVersion,
                pluginClasspaths = pluginClasspaths
            )
        }
    }
}
