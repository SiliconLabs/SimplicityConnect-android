package com.siliconlabs.bledemo.bluetooth.ble

data class ManufacturerDataFilter(
        val id: Int,
        val data: ByteArray,
        val mask: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ManufacturerDataFilter

        if (id != other.id) return false
        if (!data.contentEquals(other.data)) return false
        if (mask != null) {
            if (other.mask == null) return false
            if (!mask.contentEquals(other.mask)) return false
        } else if (other.mask != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + data.contentHashCode()
        result = 31 * result + (mask?.contentHashCode() ?: 0)
        return result
    }
}
