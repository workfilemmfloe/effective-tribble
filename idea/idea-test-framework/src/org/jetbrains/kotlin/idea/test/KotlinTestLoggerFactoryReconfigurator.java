/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.test;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.testFramework.TestLoggerFactory;
import org.apache.log4j.LogManager;
import org.apache.log4j.xml.DOMConfigurator;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

class KotlinTestLoggerFactoryReConfigurator {
    private static final String SYSTEM_MACRO = "$SYSTEM_DIR$";
    private static final String APPLICATION_MACRO = "$APPLICATION_DIR$";
    private static final String LOG_DIR_MACRO = "$LOG_DIR$";
    private static final long LOG_SIZE_LIMIT = 100 * 1024 * 1024;

    private KotlinTestLoggerFactoryReConfigurator() {
    }

    public static void reconfigure() {
        // Activate init() for TestLoggerFactory
        Logger.getInstance(KotlinTestLoggerFactoryReConfigurator.class);

        try {
            File logXmlFile = new File(PathManager.getHomePath(), "../test-log.xml");
            if (!logXmlFile.exists()) {
                return;
            }

            String logDir = TestLoggerFactory.getTestLogDir();
            String text = FileUtil.loadFile(logXmlFile);
            text = StringUtil.replace(text, SYSTEM_MACRO, StringUtil.replace(PathManager.getSystemPath(), "\\", "\\\\"));
            text = StringUtil.replace(text, APPLICATION_MACRO, StringUtil.replace(PathManager.getHomePath(), "\\", "\\\\"));
            text = StringUtil.replace(text, LOG_DIR_MACRO, StringUtil.replace(logDir, "\\", "\\\\"));

            File logDirFile = new File(logDir);
            if (!logDirFile.mkdirs() && !logDirFile.exists()) {
                throw new IOException("Unable to create log dir: " + logDirFile);
            }

            System.setProperty("log4j.defaultInitOverride", "true");
            try {
                DOMConfigurator domConfigurator = new DOMConfigurator();
                domConfigurator.doConfigure(new StringReader(text), LogManager.getLoggerRepository());
            }
            catch (ClassCastException e) {
                //noinspection UseOfSystemOutOrSystemErr
                System.err.println("log.xml content:\n" + text);
                throw e;
            }

            File ideaLog = new File(TestLoggerFactory.getTestLogDir(), "idea.log");
            if (ideaLog.exists() && ideaLog.length() >= LOG_SIZE_LIMIT) {
                FileUtil.writeToFile(ideaLog, "");
            }
        }
        catch (Throwable e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }
}