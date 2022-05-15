/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.tests.generator.TestGroup
import org.jetbrains.kotlin.generators.tests.generator.testGroup
import org.jetbrains.kotlin.jps.build.*
import org.jetbrains.kotlin.jps.build.android.AbstractAndroidJpsTestCase
import org.jetbrains.kotlin.jps.build.dependeciestxt.actualizeMppJpsIncTestCaseDirs
import org.jetbrains.kotlin.jps.incremental.AbstractJsProtoComparisonTest
import org.jetbrains.kotlin.jps.incremental.AbstractJvmProtoComparisonTest

fun addJpsTestGroups() {
/*    testGroup("jps-plugin/jps-tests/test", "jps-plugin/testData") {
        testClass<AbstractIncrementalJpsTest> {
            model("incremental/multiModule/common", extension = null, excludeParentDirs = true)
            model("incremental/multiModule/jvm", extension = null, excludeParentDirs = true)
            model("incremental/multiModule/multiplatform/custom", extension = null, excludeParentDirs = true)
            model("incremental/pureKotlin", extension = null, recursive = false)
            model("incremental/withJava", extension = null, excludeParentDirs = true)
            model("incremental/inlineFunCallSite", extension = null, excludeParentDirs = true)
            model("incremental/classHierarchyAffected", extension = null, excludeParentDirs = true)
        }

        actualizeMppJpsIncTestCaseDirs(testDataRoot, "incremental/multiModule/multiplatform/withGeneratedContent")

        testClass<AbstractIncrementalJsJpsTest> {
            model("incremental/multiModule/common", extension = null, excludeParentDirs = true)
        }

        testClass<AbstractMultiplatformJpsTestWithGeneratedContent> {
            model(
                "incremental/multiModule/multiplatform/withGeneratedContent", extension = null, excludeParentDirs = true,
                testClassName = "MultiplatformMultiModule", recursive = true
            )
        }

        testClass<AbstractJvmLookupTrackerTest> {
            model("incremental/lookupTracker/jvm", extension = null, recursive = false)
        }
        testClass<AbstractJsLookupTrackerTest> {
            model("incremental/lookupTracker/js", extension = null, recursive = false)
        }

        testClass<AbstractIncrementalLazyCachesTest> {
            model("incremental/lazyKotlinCaches", extension = null, excludeParentDirs = true)
            model("incremental/changeIncrementalOption", extension = null, excludeParentDirs = true)
        }

        testClass<AbstractIncrementalCacheVersionChangedTest> {
            model("incremental/cacheVersionChanged", extension = null, excludeParentDirs = true)
        }

        testClass<AbstractDataContainerVersionChangedTest> {
            model("incremental/cacheVersionChanged", extension = null, excludeParentDirs = true)
        }
    }

    testGroup("jps-plugin/jps-tests/test", "jps-plugin/testData") {
        fun TestGroup.TestClass.commonProtoComparisonTests() {
            model("comparison/classSignatureChange", extension = null, excludeParentDirs = true)
            model("comparison/classPrivateOnlyChange", extension = null, excludeParentDirs = true)
            model("comparison/classMembersOnlyChanged", extension = null, excludeParentDirs = true)
            model("comparison/packageMembers", extension = null, excludeParentDirs = true)
            model("comparison/unchanged", extension = null, excludeParentDirs = true)
        }

        testClass<AbstractJvmProtoComparisonTest> {
            commonProtoComparisonTests()
            model("comparison/jvmOnly", extension = null, excludeParentDirs = true)
        }

        testClass<AbstractJsProtoComparisonTest> {
            commonProtoComparisonTests()
            model("comparison/jsOnly", extension = null, excludeParentDirs = true)
        }
    }*/
}

fun addAndroidExtensionsJpsTestGroups() {
    testGroup("plugins/android-extensions/android-extensions-jps/test", "plugins/android-extensions/android-extensions-jps/testData") {
        testClass<AbstractAndroidJpsTestCase> {
            model("android", recursive = false, extension = null)
        }
    }
}