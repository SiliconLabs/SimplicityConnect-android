package com.siliconlabs.bledemo.iop_test.models

enum class IopBoard(val icName: IcName, val ota1FileName: String, val ota2FileName: String) {
    UNKNOWN(IcName.INFORMATION_IC_NAME_UNKNOWN, "", ""),
    BRD_4104A(IcName.INFORMATION_IC_NAME_BG13, "iop-4104a-update.gbl", "iop-4104a.gbl"),
    BRD_4181A(IcName.INFORMATION_IC_NAME_BG21, "iop-4181a-update.gbl", "iop-4181a.gbl"),
    BRD_4181B(IcName.INFORMATION_IC_NAME_BG21, "iop-4181b-update.gbl", "iop-4181b.gbl"),
    BRD_4182A(IcName.INFORMATION_IC_NAME_BG22, "iop-4182a-update.gbl", "iop-4182a.gbl");

    enum class IcName(val text: String) {
        INFORMATION_IC_NAME_UNKNOWN("Unknown"),
        INFORMATION_IC_NAME_BG13("BG13"),
        INFORMATION_IC_NAME_BG21("xG21"),
        INFORMATION_IC_NAME_BG22("xG22");
    }
}