/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.wasm.ast.*
import org.jetbrains.kotlin.backend.wasm.lower.WasmSignature
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable

class WasmCompiledModuleFragment {
    val functions = ReferencableAndDefinable<IrFunctionSymbol, WasmFunction>()
    val globals = ReferencableAndDefinable<IrFieldSymbol, WasmGlobal>()
    val functionTypes = ReferencableAndDefinable<IrFunctionSymbol, WasmFunctionType>()
    val structTypes = ReferencableAndDefinable<IrClassSymbol, WasmStructType>()

    val classIds = ReferencableElements<IrClassSymbol, Int>()
    val interfaceId = ReferencableElements<IrClassSymbol, Int>()
    val virtualFunctionId = ReferencableElements<IrFunctionSymbol, Int>()
    val signatureId = ReferencableElements<WasmSignature, Int>()
    val stringLiteralId = ReferencableElements<String, Int>()

    val classes = mutableListOf<IrClassSymbol>()
    val interfaces = mutableListOf<IrClassSymbol>()
    val virtualFunctions = mutableListOf<IrSimpleFunctionSymbol>()
    val signatures = LinkedHashSet<WasmSignature>()
    val stringLiterals = mutableListOf<String>()

    val typeInfo = ReferencableAndDefinable<IrClassSymbol, ConstantDataElement>()
    val exports = mutableListOf<WasmExport>()

    var startFunction: WasmFunction? = null

    open class ReferencableElements<Ir, Wasm : Any> {
        val unbound = mutableMapOf<Ir, WasmSymbol<Wasm>>()
        fun reference(ir: Ir): WasmSymbol<Wasm> =
            unbound.getOrPut(ir) { WasmSymbol.unbound() }
    }

    class ReferencableAndDefinable<Ir, Wasm: Any> : ReferencableElements<Ir, Wasm>() {
        fun define(ir: Ir, wasm: Wasm) {
            if (ir in defined)
                error("Trying to redefine element: IR: $ir Wasm: $wasm")

            elements += wasm
            defined[ir] = wasm
            wasmToIr[wasm] = ir
        }

        val defined = LinkedHashMap<Ir, Wasm>()
        val elements = mutableListOf<Wasm>()

        val wasmToIr = mutableMapOf<Wasm, Ir>()
    }

    fun linkWasmCompiledFragments(): WasmModule {
        bind(functions.unbound, functions.defined)
        bind(globals.unbound, globals.defined)
        bind(functionTypes.unbound, functionTypes.defined)
        bind(structTypes.unbound, structTypes.defined)

        val klassIds = mutableMapOf<IrClassSymbol, Int>()
        var classId = 0
        for (typeInfoElement in typeInfo.elements) {
            val ir = typeInfo.wasmToIr.getValue(typeInfoElement)
            klassIds[ir] = classId
            classId += typeInfoElement.sizeInBytes
        }

        bind(classIds.unbound, klassIds)
        bindIndices(virtualFunctionId.unbound, virtualFunctions)
        bindIndices(signatureId.unbound, signatures.toList())
        bindIndices(interfaceId.unbound, interfaces)
        bindIndices(stringLiteralId.unbound, stringLiterals)

        val data = typeInfo.elements.map {
            val ir = typeInfo.wasmToIr.getValue(it)
            val id = klassIds.getValue(ir)
            WasmData(id, it.toBytes())
        }

        val logTypeInfo = false
        if (logTypeInfo) {
            println("Signatures: ")
            for ((index, signature: WasmSignature) in signatures.withIndex()) {
                println("  -- $index $signature")
            }

            println("Interfaces: ")
            for ((index, iface: IrClassSymbol) in interfaces.withIndex()) {
                println("  -- $index ${iface.owner.fqNameWhenAvailable}")
            }

            println("Virtual functions: ")
            for ((index, vf: IrSimpleFunctionSymbol) in virtualFunctions.withIndex()) {
                println("  -- $index ${vf.owner.fqNameWhenAvailable}")
            }

            println(ConstantDataStruct("typeInfo", typeInfo.elements).dump("", 0))
        }

        val table = WasmTable(virtualFunctions.map { functions.defined.getValue(it) })
        val typeInfoSize = classId
        val memorySizeInPages = (typeInfoSize / 65_536) + 1
        val memory = WasmMemory(memorySizeInPages, memorySizeInPages)

        val module = WasmModule(
            functionTypes = functionTypes.elements,
            structTypes = structTypes.elements,
            importedFunctions = functions.elements.filterIsInstance<WasmImportedFunction>(),
            definedFunctions = functions.elements.filterIsInstance<WasmDefinedFunction>(),
            table = table,
            memory = memory,
            globals = globals.elements,
            exports = exports,
            start = WasmStart(startFunction!!),
            data = data
        )
        module.calculateIds()
        return module
    }
}

fun <IrSymbolType, WasmDeclarationType, WasmSymbolType : WasmSymbol<WasmDeclarationType>> bind(
    unbound: Map<IrSymbolType, WasmSymbolType>,
    defined: Map<IrSymbolType, WasmDeclarationType>
) {
    unbound.forEach { (irSymbol, wasmSymbol) ->
        if (irSymbol !in defined)
            error("Can't link symbol ${irSymbolDebugDump(irSymbol)}")
        wasmSymbol.bind(defined.getValue(irSymbol))
    }
}

private fun irSymbolDebugDump(symbol: Any?): String =
    when (symbol) {
        is IrFunctionSymbol -> "function ${symbol.owner.fqNameWhenAvailable}"
        is IrClassSymbol -> "class ${symbol.owner.fqNameWhenAvailable}"
        else -> symbol.toString()
    }

fun <IrSymbolType> bindIndices(
    unbound: Map<IrSymbolType, WasmSymbol<Int>>,
    ordered: List<IrSymbolType>
) {
    unbound.forEach { (irSymbol, wasmSymbol) ->
        val index = ordered.indexOf(irSymbol)
        if (index == -1)
            error("Can't link symbol with indices $irSymbol")
        wasmSymbol.bind(index)
    }
}