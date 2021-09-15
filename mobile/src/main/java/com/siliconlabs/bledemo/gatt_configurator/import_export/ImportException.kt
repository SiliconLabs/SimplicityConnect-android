package com.siliconlabs.bledemo.gatt_configurator.import_export

class ImportException(
        val errorType: ErrorType,
        val provided: String? = null,
        val expected: Set<String>? = null
) : Exception(errorType.toString()) {
    init {
        this.printStackTrace()
    }

    enum class ErrorType {
        PARSING_ERROR,
        NESTED_TAG_EXPECTED,

        WRONG_TAG_NAME,
        WRONG_TAG_VALUE,
        TAG_MAXIMUM_OCCURRENCE_EXCEEDED,

        ATTRIBUTE_NAME_DUPLICATED,
        WRONG_ATTRIBUTE_NAME,
        WRONG_ATTRIBUTE_VALUE,
        MANDATORY_ATTRIBUTE_MISSING,

        NO_CAPABILITIES_DECLARED,
        TOO_MANY_CAPABILITIES_DECLARED,
        WRONG_CAPABILITY_LISTED,

        NO_PROPERTIES_DECLARED,
        PROPERTY_NOT_SUPPORTED_BY_DESCRIPTOR,
        WRONG_INCLUDE_ID_DECLARED
    }
}