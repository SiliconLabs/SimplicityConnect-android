package com.siliconlabs.bledemo.features.iop_test.models

enum class IopBoard(val icName: IcName, val ota1FileName: String, val ota2FileName: String) {
    UNKNOWN(IcName.INFORMATION_IC_NAME_UNKNOWN, "", ""),
    BRD_4104A(IcName.INFORMATION_IC_NAME_BG13, "iop-4104a-update.gbl", "iop-4104a.gbl"),
    BRD_4181A(IcName.INFORMATION_IC_NAME_BG21, "iop-4181a-update.gbl", "iop-4181a.gbl"),
    BRD_4181B(IcName.INFORMATION_IC_NAME_BG21, "iop-4181b-update.gbl", "iop-4181b.gbl"),
    BRD_4182A(IcName.INFORMATION_IC_NAME_BG22, "iop-4182a-update.gbl", "iop-4182a.gbl"),
    BRD_4186B(IcName.INFORMATION_IC_NAME_BG24, "iop-4186b-update.gbl", "iop-4186b.gbl");

    enum class IcName(val text: String) {
        INFORMATION_IC_NAME_UNKNOWN("Unknown"),
        INFORMATION_IC_NAME_BG13("BG13"),
        INFORMATION_IC_NAME_BG21("xG21"),
        INFORMATION_IC_NAME_BG22("xG22"),
        INFORMATION_IC_NAME_BG24("xG24");
    }

    companion object {

        @JvmStatic
        fun fromBoardCode(code: Byte): IopBoard {
            return when (code) {
                1.toByte() -> BRD_4104A
                2.toByte() -> BRD_4181A
                3.toByte() -> BRD_4181B
                4.toByte() -> BRD_4182A
                5.toByte() -> BRD_4186B
                else -> UNKNOWN
            }
        }

        @JvmStatic
        fun fromBoardString(model: String): IopBoard {
            return when (model) {
                "BRD4104A" -> BRD_4104A
                "BRD4181A" -> BRD_4181A
                "BRD4181B" -> BRD_4181B
                "BRD4182A" -> BRD_4182A
                "BRD4186B" -> BRD_4186B
                else -> UNKNOWN
            }
        }
    }
}