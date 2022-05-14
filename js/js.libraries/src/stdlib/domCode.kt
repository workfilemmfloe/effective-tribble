package kotlin.dom

import org.w3c.dom.Document
import org.w3c.dom.Node

public fun createDocument(): Document {
    return js.dom.html.document.implementation.createDocument(null, null, null)
}

native public val Node.outerHTML: String = js.noImpl

/** Converts the node to an XML String */
public fun Node.toXmlString(): String = this.outerHTML

/** Converts the node to an XML String */
public fun Node.toXmlString(xmlDeclaration: Boolean): String = this.outerHTML
