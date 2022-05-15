package custom.scriptDefinition

import java.io.File
import kotlin.script.dependencies.*
import kotlin.script.experimental.dependencies.*
import kotlin.script.experimental.location.*
import kotlin.script.templates.ScriptTemplateDefinition
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.annotations.KotlinScriptEvaluator

@KotlinScript(displayName = "Kotlin Script with custom extension", fileExtension = "mykts")
open class Template