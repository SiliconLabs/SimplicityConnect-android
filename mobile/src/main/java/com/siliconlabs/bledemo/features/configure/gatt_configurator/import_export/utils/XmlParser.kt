package com.siliconlabs.bledemo.features.configure.gatt_configurator.import_export.utils

import com.siliconlabs.bledemo.features.configure.gatt_configurator.import_export.ImportException
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParser.TEXT
import org.xmlpull.v1.XmlPullParserException
import com.siliconlabs.bledemo.features.configure.gatt_configurator.import_export.ImportException.ErrorType.*

class XmlParser(private val parser: XmlPullParser) {

    fun parseTagAttributes(allowedNames: Set<String>,
                           restrictedAttributes: Map<String, Set<String>>? = null) : Map<String, String> {
        val parsedAttributes = mutableMapOf<String, String>()
        val allowedNamesLeft = allowedNames.toMutableSet()

        for (i in 0 until parser.attributeCount) {
            parser.getAttributeName(i).let {
                if (allowedNames.contains(it)) {
                    if (allowedNamesLeft.contains(it)) {
                        restrictedAttributes?.let { map ->
                            map[it]?.let { allowedValues ->
                                parseAttributeValue(parser.getAttributeValue(i), allowedValues)
                            }
                        }
                        parsedAttributes[it] = parser.getAttributeValue(i)
                        allowedNamesLeft.remove(it)
                    }
                    else throw ImportException(ATTRIBUTE_NAME_DUPLICATED, parser.getAttributeName(i))
                }
                else throw ImportException(WRONG_ATTRIBUTE_NAME, parser.getAttributeName(i), allowedNames)
            }
        }
        return parsedAttributes.toMap()
    }

    fun parseTagValue() : String {
        return try {
            parser.nextText()
        } catch (err: XmlPullParserException) {
            throw ImportException(PARSING_ERROR)
        }
    }

    fun parseTagValue(allowedValues: Set<String>) : String {
        return try {
            val retrievedValue = parser.nextText()
            if (allowedValues.contains(retrievedValue)) retrievedValue
            else throw ImportException(WRONG_CAPABILITY_LISTED, retrievedValue, allowedValues)
        } catch (err: XmlPullParserException) {
            throw ImportException(PARSING_ERROR)
        }
    }

    fun parseTagValue(regex: Regex) : String {
        return try {
            val retrievedValue = parser.nextText()
            if (regex.matches(retrievedValue)) retrievedValue
            else throw ImportException(WRONG_TAG_VALUE, retrievedValue, setOf(regex.toString()))
        } catch (err: XmlPullParserException) {
            throw ImportException(PARSING_ERROR)
        }
    }

    fun parseInside(tagName: String, inside: () -> Unit) {
        try {
            while (parser.nextTag() != XmlPullParser.END_TAG && parser.name != tagName) {
                inside()
            }
        } catch (err: XmlPullParserException) {
            if (parser.eventType == TEXT) throw ImportException(NESTED_TAG_EXPECTED)
            else throw ImportException(PARSING_ERROR)
        }

    }

    fun parseRootTagOpening() {
        if (parser.nextTag() != XmlPullParser.START_TAG) {
            throw ImportException(NESTED_TAG_EXPECTED)
        } else {
            if (parser.name != XmlConst.gatt && parser.name != XmlConst.project) {
                throw ImportException(WRONG_TAG_NAME, parser.name, XmlConst.rootElements)
            }
        }
    }

    private fun parseAttributeValue(passedValue: String, allowedValues: Set<String>) {
        if (!allowedValues.contains(passedValue)) {
            throw ImportException(WRONG_ATTRIBUTE_VALUE, passedValue, allowedValues)
        }
    }
}