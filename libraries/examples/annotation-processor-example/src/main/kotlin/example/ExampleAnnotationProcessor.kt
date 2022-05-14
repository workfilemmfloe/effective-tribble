package example

import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

public class ExampleAnnotationProcessor : AbstractProcessor() {

    private companion object {
        val ANNOTATION_FQ_NAME = ExampleAnnotation::class.java.canonicalName
        val SUFFIX_OPTION = "suffix"
        val GENERATE_KOTLIN_CODE_OPTION = "generate.kotlin.code"
        val KAPT_KOTLIN_GENERATED_OPTION = "kapt.kotlin.generated"
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        val elements = roundEnv.getElementsAnnotatedWith(ExampleAnnotation::class.java)

        val elementUtils = processingEnv.elementUtils
        val filer = processingEnv.filer

        val options = processingEnv.options
        val generatedFileSuffix = options[SUFFIX_OPTION] ?: "Generated"
        val generateKotlinCode = "true" == options[GENERATE_KOTLIN_CODE_OPTION]
        val kotlinGenerated = options[KAPT_KOTLIN_GENERATED_OPTION]

        for (element in elements) {
            val packageName = elementUtils.getPackageOf(element).qualifiedName.toString()
            val simpleName = element.simpleName
            val className = simpleName.toString().capitalize() + generatedFileSuffix

            filer.createSourceFile(className).openWriter().use { with(it) {
                appendln("package $packageName;")
                appendln("public final class $className {}")
            }}

            if (generateKotlinCode && kotlinGenerated != null && element.kind == ElementKind.CLASS) {
                File(kotlinGenerated, "$simpleName.kt").writer().buffered().use {
                    it.appendln("package $packageName")
                    it.appendln("fun $simpleName.customToString() = \"$simpleName: \" + toString()")
                }
            }
        }

        return true;
    }

    override fun getSupportedSourceVersion() = SourceVersion.RELEASE_6

    override fun getSupportedAnnotationTypes() = setOf(ANNOTATION_FQ_NAME)

    override fun getSupportedOptions() = setOf(SUFFIX_OPTION)
}