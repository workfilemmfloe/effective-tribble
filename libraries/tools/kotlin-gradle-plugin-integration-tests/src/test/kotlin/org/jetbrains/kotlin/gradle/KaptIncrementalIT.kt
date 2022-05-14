package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.allKotlinFiles
import org.jetbrains.kotlin.gradle.util.findFileByName
import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test

class KaptIncrementalIT: BaseGradleIT() {
    companion object {
        private const val GRADLE_VERSION = "2.10"
        private val EXAMPLE_ANNOTATION_REGEX = "@(field:)?example.ExampleAnnotation".toRegex()
    }

    private fun getProject() = Project("kaptIncrementalCompilationProject", GRADLE_VERSION)

    private val annotatedElements =
            arrayOf("A", "funA", "valA", "funUtil", "valUtil", "B", "funB", "valB", "useB")

    override fun defaultBuildOptions(): BuildOptions =
            super.defaultBuildOptions().copy(incremental = true)

    @Test
    fun testBasic() {
        val project = getProject()

        project.build("build") {
            assertSuccessful()
            checkGenerated(*annotatedElements)
            checkNotGenerated("notAnnotatedFun")
            assertContains("foo.ATest PASSED")
        }

        project.build("build") {
            assertSuccessful()
            assertContains(":compileKotlin UP-TO-DATE",
                           ":compileJava UP-TO-DATE",
                           ":kaptKotlin UP-TO-DATE",
                           ":kaptGenerateStubsKotlin UP-TO-DATE")
        }
    }

    @Test
    fun testChangeFunctionBodyWithoutChangingSignature() {
        val project = getProject()

        project.build("build") {
            assertSuccessful()
            checkGenerated(*annotatedElements)
            checkNotGenerated("notAnnotatedFun")
            assertContains("foo.ATest PASSED")
        }

        val utilKt = project.projectDir.getFileByName("util.kt")
        utilKt.modify { oldContent ->
            assert(oldContent.contains("2 * 2 == 4"))
            oldContent.replace("2 * 2 == 4", "2 * 2 == 5")
        }

        project.build("build") {
            assertSuccessful()
            assertNotContains(":kaptGenerateStubsKotlin UP-TO-DATE")
            assertContains(":kaptKotlin UP-TO-DATE")
        }
    }

    @Test
    fun testAddAnnotatedElement() {
        val project = getProject()
        project.build("build") {
            assertSuccessful()
        }

        val utilKt = project.projectDir.getFileByName("util.kt")
        utilKt.modify { oldContent ->
            """$oldContent

            @example.ExampleAnnotation
            fun newUtilFun() {}"""
        }

        project.build("build") {
            assertSuccessful()
            assertKapt3FullyExecuted()

            // todo: for kapt with stubs check compileKotlin and compileKotlinAfterJava separately
            assertCompiledKotlinSourcesHandleKapt3(project.relativize(utilKt))
            checkGenerated(*(annotatedElements + arrayOf("newUtilFun")))
        }
    }

    @Test
    fun testAddAnnotation() {
        val project = getProject()
        project.build("build") {
            assertSuccessful()
        }

        val utilKt = project.projectDir.getFileByName("util.kt")
        utilKt.modify { it.replace("fun notAnnotatedFun", "@example.ExampleAnnotation fun notAnnotatedFun") }

        project.build("build") {
            assertSuccessful()
            assertKapt3FullyExecuted()
            assertCompiledKotlinSources(project.relativize(utilKt))
            checkGenerated(*(annotatedElements + arrayOf("notAnnotatedFun")))
        }
    }

    @Test
    fun testRemoveSourceFile() {
        val project = getProject()
        val kapt3IncDataPath = "build/tmp/kapt3/incrementalData/main"
        val kapt3StubsPath = "build/tmp/kapt3/stubs/main"

        project.build("build") {
            assertSuccessful()
            assertKapt3FullyExecuted()
            assertFileExists("$kapt3IncDataPath/bar/B.class")
            assertFileExists("$kapt3IncDataPath/bar/UseBKt.class")
            assertFileExists("$kapt3StubsPath/bar/B.java")
            assertFileExists("$kapt3StubsPath/bar/UseBKt.java")
        }

        with (project.projectDir) {
            getFileByName("B.kt").delete()
            getFileByName("useB.kt").delete()
        }

        project.build("build") {
            assertFailed()
            assertNoSuchFile("$kapt3IncDataPath/bar/B.class")
            assertNoSuchFile("$kapt3IncDataPath/bar/UseBKt.class")
            assertNoSuchFile("$kapt3StubsPath/bar/B.java")
            assertNoSuchFile("$kapt3StubsPath/bar/UseBKt.java")
        }

        project.projectDir.getFileByName("JavaClass.java").delete()

        project.build("build") {
            assertSuccessful()
            assertKapt3FullyExecuted()
            assertCompiledKotlinSourcesHandleKapt3(project.relativize(project.projectDir.allKotlinFiles()))
            val affectedElements = arrayOf("B", "funB", "valB", "useB")
            checkGenerated(*(annotatedElements.toSet() - affectedElements).toTypedArray())
            checkNotGenerated(*affectedElements)
        }
    }

    @Test
    fun testRemoveAnnotations() {
        val project = getProject()
        project.build("build") {
            assertSuccessful()
        }

        val bKt = project.projectDir.getFileByName("B.kt")
        bKt.modify { it.replace(EXAMPLE_ANNOTATION_REGEX, "") }
        val affectedElements = arrayOf("B", "funB", "valB")

        project.build("build") {
            assertSuccessful()

            assertKapt3FullyExecuted()

            val useBKt = project.projectDir.getFileByName("useB.kt")
            assertCompiledKotlinSources(project.relativize(bKt, useBKt), tasks = listOf("kaptGenerateStubsKotlin"))

            // java removal is detected
            assertCompiledKotlinSources(project.relativize(project.projectDir.allKotlinFiles()),
                    tasks = listOf("compileKotlin"))

            checkGenerated(*(annotatedElements.toSet() - affectedElements).toTypedArray())
            checkNotGenerated(*affectedElements)
        }
    }

    @Test
    fun testChangeAnnotatedPropertyType() {
        val project = getProject()
        project.build("build") {
            assertSuccessful()
        }

        val bKt = project.projectDir.getFileByName("B.kt")
        val useBKt = project.projectDir.getFileByName("useB.kt")
        bKt.modify { it.replace("val valB = \"text\"", "val valB = 4") }

        project.build("build") {
            assertSuccessful()
            assertKapt3FullyExecuted()
            assertCompiledKotlinSourcesHandleKapt3(project.relativize(bKt, useBKt))
            checkGenerated(*annotatedElements)
        }
    }

    private fun CompiledProject.assertCompiledKotlinSourcesHandleKapt3(
            sources: Iterable<String>,
            weakTesting: Boolean = false
    ) {
        assertCompiledKotlinSources(sources, weakTesting, tasks = listOf("compileKotlin", "kaptGenerateStubsKotlin"))
    }

    private fun CompiledProject.assertKapt3FullyExecuted() {
            assertNotContains(":kaptKotlin UP-TO-DATE", ":kaptGenerateStubsKotlin UP-TO-DATE")
    }

    private fun CompiledProject.checkGenerated(vararg annotatedElementNames: String) {
        getGeneratedFileNames(*annotatedElementNames).forEach {
            val file = project.projectDir.getFileByName(it)
            assert(file.isFile) { "$file must exist" }
        }
    }

    private fun CompiledProject.checkNotGenerated(vararg annotatedElementNames: String) {
        getGeneratedFileNames(*annotatedElementNames).forEach {
            val file = project.projectDir.findFileByName(it)
            assert(file == null) { "$file must not exist" }
        }
    }

    private fun getGeneratedFileNames(vararg annotatedElementNames: String): Iterable<String> {
        val names = annotatedElementNames.map { it.capitalize() + "Generated" }
        return names.map { it + ".java" }
    }
}