/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.google.common.base.Charsets
import com.intellij.openapi.util.io.FileUtil
import java.io.*
import java.util.*

object PropertiesFiles {
    @Throws(IOException::class)
    fun getProperties(filePath: File): Properties {
        if (filePath.isDirectory) {
            throw IllegalArgumentException(String.format("The path '%1\$s' belongs to a directory!", filePath.path))
        }
        if (!filePath.exists()) {
            return Properties()
        }
        val properties = Properties()
        InputStreamReader(BufferedInputStream(FileInputStream(filePath)), Charsets.UTF_8).use { reader -> properties.load(reader) }
        return properties
    }

    @Throws(IOException::class)
    fun savePropertiesToFile(properties: Properties, filePath: File, comments: String?) {
        FileUtil.createParentDirs(filePath)
        FileOutputStream(filePath).use { out ->
            // Note that we don't write the properties files in UTF-8; this will *not* write the
            // files with the default platform encoding; instead, it will write it using ISO-8859-1 and
            // \\u escaping syntax for other characters. This will work with older versions of the Gradle
            // plugin which does not read the .properties file with UTF-8 encoding. In the future when
            // nobody is using older (0.7.x) versions of the Gradle plugin anymore we can upgrade this
            properties.store(out, comments)
        }
    }
}