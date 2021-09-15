package com.siliconlabs.bledemo.gatt_configurator.import_export.utils

import org.xmlpull.v1.XmlSerializer
import java.lang.StringBuilder

class XmlPrinter(private val serializer: XmlSerializer) {

    private var indent = 0

    fun openTag(tag: String, attributes: Map<String, String>? = null, tagValue: String? = null,
                breakLine: Boolean = false, increaseIndent: Boolean = false) : XmlSerializer {
        serializer.let {
            it.text(printIndent())
            it.startTag(null, tag)
            attributes?.forEach { att ->
                it.attribute(null, att.key, att.value)
            }
            tagValue?.let { value ->
                it.text(value)
            }
            if (breakLine) it.text("\n")
            if (increaseIndent) indent++
            return it
        }
    }

    fun closeTag(tag: String, decreaseIndent: Boolean = false) {
        serializer.let {
            if (decreaseIndent) {
                indent--
                it.text(printIndent())
            }
            it.endTag(null, tag)
            it.text("\n")
        }
    }

    private fun printIndent(): String {
        val separator = StringBuilder()
        for (i in 1.. indent) {
            separator.append("\t")
        }
        return separator.toString()
    }
}