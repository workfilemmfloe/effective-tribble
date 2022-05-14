/*
 * Generated file
 * DO NOT EDIT
 * 
 * See libraries/tools/idl2k for details
 */

package org.w3c.fetch

import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.css.*
import org.w3c.dom.events.*
import org.w3c.dom.parsing.*
import org.w3c.dom.svg.*
import org.w3c.dom.url.*
import org.w3c.files.*
import org.w3c.notifications.*
import org.w3c.performance.*
import org.w3c.workers.*
import org.w3c.xhr.*

@native public open class Headers(init: dynamic = noImpl) {
    fun append(name: String, value: String): Unit = noImpl
    fun delete(name: String): Unit = noImpl
    fun get(name: String): String? = noImpl
    fun getAll(name: String): Array<String> = noImpl
    fun has(name: String): Boolean = noImpl
    fun set(name: String, value: String): Unit = noImpl
}

@native public open class Request(input: dynamic, init: RequestInit = noImpl) {
    open val method: String
        get() = noImpl
    open val url: String
        get() = noImpl
    open val headers: Headers
        get() = noImpl
    open val context: String
        get() = noImpl
    open val referrer: String
        get() = noImpl
    open val mode: String
        get() = noImpl
    open val credentials: String
        get() = noImpl
    open val cache: String
        get() = noImpl
    open val redirect: String
        get() = noImpl
    open val bodyUsed: Boolean
        get() = noImpl
    fun clone(): Request = noImpl
    fun arrayBuffer(): dynamic = noImpl
    fun blob(): dynamic = noImpl
    fun formData(): dynamic = noImpl
    fun json(): dynamic = noImpl
    fun text(): dynamic = noImpl
}

@native public interface RequestInit {
    var method: String
    var headers: dynamic
    var body: dynamic
    var mode: String
    var credentials: String
    var cache: String
    var redirect: String
}

@Suppress("NOTHING_TO_INLINE")
public inline fun RequestInit(method: String, headers: dynamic, body: dynamic, mode: String, credentials: String, cache: String, redirect: String): RequestInit {
    val o = js("({})")

    o["method"] = method
    o["headers"] = headers
    o["body"] = body
    o["mode"] = mode
    o["credentials"] = credentials
    o["cache"] = cache
    o["redirect"] = redirect

    return o
}

@native public open class Response(body: dynamic = noImpl, init: ResponseInit = noImpl) {
    open val type: String
        get() = noImpl
    open val url: String
        get() = noImpl
    open val status: Short
        get() = noImpl
    open val ok: Boolean
        get() = noImpl
    open val statusText: String
        get() = noImpl
    open val headers: Headers
        get() = noImpl
    open val bodyUsed: Boolean
        get() = noImpl
    fun clone(): Response = noImpl
    fun arrayBuffer(): dynamic = noImpl
    fun blob(): dynamic = noImpl
    fun formData(): dynamic = noImpl
    fun json(): dynamic = noImpl
    fun text(): dynamic = noImpl

    companion object {
        fun error(): Response = noImpl
        fun redirect(url: String, status: Short = 302): Response = noImpl
    }
}

@native public interface ResponseInit {
    var status: Short
    var statusText: String
    var headers: dynamic
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ResponseInit(status: Short = 200, statusText: String = "OK", headers: dynamic): ResponseInit {
    val o = js("({})")

    o["status"] = status
    o["statusText"] = statusText
    o["headers"] = headers

    return o
}

