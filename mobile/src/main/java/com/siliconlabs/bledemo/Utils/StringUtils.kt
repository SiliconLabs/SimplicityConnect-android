package com.siliconlabs.bledemo.Utils

object StringUtils {

    const val HEX_VALUES = "0123456789abcdefABCDEF"

    fun getStringWithoutWhitespaces(text: String): String {
        return text.replace(" ".toRegex(), "")
    }

    fun getStringWithoutColons(text: String?): String {
        if (text == null || text == "") return ""
        val result = StringBuilder()
        val splittedStrings = text.split(":".toRegex()).toTypedArray()
        for (str in splittedStrings) {
            result.append(str)
        }
        return result.toString()
    }

    fun removeWhitespaceAndCommaIfNeeded(sb: StringBuilder) {
        val len = sb.length

        if (len >= 2 && sb[len - 1] == ' ' && sb[len - 2] == ',') {
            sb.setLength(len - 2)
        }
    }
}