package com.siliconlabs.bledemo.iop_test.models

import android.bluetooth.BluetoothGattCharacteristic
import com.siliconlabs.bledemo.iop_test.utils.ErrorCodes
import com.siliconlabs.bledemo.utils.Converters
import java.util.*

class ChildrenItemTestInfo(val id: Int, val typeTestCase: String, val nameTest: String, val properties: String) {
    var characteristic: BluetoothGattCharacteristic? = null
    private var valueErrorLog: StringBuilder? = null

    private val lstValueItemTest: MutableList<String> = ArrayList()
    private var statusWriteString = "Status write: "
    private var acceptableTime = "Acceptable time: "
    private var testingTime = "Testing time: "
    var isWriteCharacteristic = false
    var isReadCharacteristic = false
    var statusChildrenTest = false
    var statusRunTest = -1
    var statusWrite = -1
    var statusRead = -1
    var valueMtu = -1
    var startTimeTest: Long = 0
    var endTimeTest: Long = 0

    fun getLstValueItemTest(): List<String> {
        return lstValueItemTest
    }

    fun getValueErrorLog(): String {
        if (valueErrorLog == null) {
            valueErrorLog = StringBuilder()
            valueErrorLog?.append("N/A")
        }
        return valueErrorLog.toString()
    }

    fun setValueNALog(): String {
        valueErrorLog = StringBuilder()
        valueErrorLog?.append("N/A")
        return valueErrorLog.toString()
    }

    fun setDataAndCompareResult(characteristic: BluetoothGattCharacteristic) {
        valueErrorLog = StringBuilder()
        val uuids = CommonUUID.Characteristic.values()
        val cUuid = characteristic.uuid.toString()
        var uuid = -1
        for (i in uuids.indices) {
            if (cUuid.equals(uuids[i].showUUID(), ignoreCase = true)) {
                uuid = uuids[i].id
                break
            }
        }
        when (uuid) {
            CommonUUID.ID_READ_ONLY_LENGTH_1,
            CommonUUID.ID_READ_ONLY_LENGTH_255 -> {
                if (statusRead == 0) {
                    statusChildrenTest = true
                } else if (statusRead != -1) {
                    valueErrorLog?.append(ErrorCodes.getErrorName(statusRead))
                }
            }
            CommonUUID.ID_WRITE_ONLY_LENGTH_1,
            CommonUUID.ID_WRITE_WITHOUT_RESPONSE_LENGTH_1,
            CommonUUID.ID_WRITE_ONLY_LENGTH_255,
            CommonUUID.ID_WRITE_WITHOUT_RESPONSE_LENGTH_255 -> {
                if (statusWrite == 0) {
                    statusChildrenTest = true
                } else if (statusRead != -1) {
                    valueErrorLog?.append(ErrorCodes.getErrorName(statusRead))
                }
            }
            CommonUUID.ID_NOTIFICATION_LENGTH_1,
            CommonUUID.ID_INDICATE_LENGTH_1 -> {
                if (CommonResultTest.RESULT_TEST_55_LENGTH_1 == Converters.getHexValue(characteristic.value)
                        && endTimeTest - startTimeTest <= Common.TIME_OUT_NOTIFICATION_INDICATE_TEST) {
                    statusChildrenTest = true
                }
                valueErrorLog?.append("(")?.append(testingTime + (endTimeTest - startTimeTest))?.append("ms;")?.append(acceptableTime)?.append(Common.TIME_OUT_NOTIFICATION_INDICATE_TEST)?.append("ms")?.append(")")
            }
            CommonUUID.ID_NOTIFICATION_LENGTH_MTU_3,
            CommonUUID.ID_INDICATE_LENGTH_MTU_3 -> {
                lstValueItemTest.add(0, Converters.getHexValue(characteristic.value))
                if (compareValueCharacteristic(lstValueItemTest[0], Converters.getHexValue(Converters.decToByteArray(createDataTestCaseLengthByte255(valueMtu - 3))))
                        && endTimeTest - startTimeTest <= Common.TIME_OUT_NOTIFICATION_INDICATE_TEST) {
                    statusChildrenTest = true
                    valueErrorLog?.append("(")?.append(testingTime + (endTimeTest - startTimeTest))?.append("ms;")?.append(acceptableTime)?.append(Common.TIME_OUT_NOTIFICATION_INDICATE_TEST)?.append("ms")?.append(")")
                } else {
                    valueErrorLog?.append("(")?.append(testingTime + (endTimeTest - startTimeTest))?.append("ms;")?.append(acceptableTime)?.append(Common.TIME_OUT_NOTIFICATION_INDICATE_TEST)?.append("ms")?.append(")")
                    valueErrorLog?.append(statusWriteString)?.append(";Value: ")?.append(Converters.getHexValue(characteristic.value))
                }
            }
            CommonUUID.ID_IOP_TEST_LENGTH_1 -> {
                if (statusWrite == 0 && Converters.getHexValue(characteristic.value) == CommonResultTest.RESULT_TEST_55_LENGTH_1) {
                    statusChildrenTest = true
                } else {
                    valueErrorLog?.append(if (statusWrite != 0) ErrorCodes.getErrorName(statusWrite) + ";" else "")?.append(Converters.getHexValue(characteristic.value))
                }
            }
            CommonUUID.ID_IOP_TEST_LENGTH_255 -> {
                if (statusWrite == 0 && compareValueCharacteristic(Converters.getHexValue(characteristic.value), Converters.getHexValue(Converters.decToByteArray(createDataTestCaseLengthByte255(255))))) {
                    statusChildrenTest = true
                } else {
                    valueErrorLog?.append(if (statusWrite != 0) ErrorCodes.getErrorName(statusWrite) + ";" else "")?.append(Converters.getHexValue(characteristic.value))
                }
            }
            CommonUUID.ID_IOP_TEST_LENGTH_VARIABLE_4 -> {
                if (statusRead == 0) {
                    lstValueItemTest.add(Converters.getHexValue(characteristic.value))
                }
                if (lstValueItemTest.size > 1) {
                    if (lstValueItemTest[0] == CommonResultTest.RESULT_TEST_55_LENGTH_1
                            && lstValueItemTest[1] == CommonResultTest.RESULT_TEST_66_LENGTH_4
                            || lstValueItemTest[1] == CommonResultTest.RESULT_TEST_55_LENGTH_1
                            && lstValueItemTest[0] == CommonResultTest.RESULT_TEST_66_LENGTH_4) {
                        statusChildrenTest = true
                    } else {
                        valueErrorLog?.append("Value length 1: ")?.append(lstValueItemTest[0])?.append(";")?.append("Value length 4: ")?.append(lstValueItemTest[1])
                    }
                }
            }
            CommonUUID.ID_IOP_TEST_CONST_LENGTH_1 -> {
                if (statusRead == 0) {
                    lstValueItemTest.add(Converters.getHexValue(characteristic.value))
                }
                if (statusWrite == 0x0003) {
                    if (lstValueItemTest[0] == CommonResultTest.RESULT_TEST_55_LENGTH_1) {
                        statusChildrenTest = true
                    } else {
                        valueErrorLog?.append("Value length 1: ")?.append(lstValueItemTest[0])?.append(";")?.append("Status: ")?.append(statusWrite)
                    }
                }
            }
            CommonUUID.ID_IOP_TEST_CONST_LENGTH_255 -> {
                if (statusRead == 0) {
                    lstValueItemTest.add(0, Converters.getHexValue(characteristic.value))
                }
                if (statusWrite == 0x0003) {
                    if (compareValueCharacteristic(lstValueItemTest[0], Converters.getHexValue(Converters.decToByteArray(createDataTestCaseLengthByte255(255)))) && statusWrite == 0x0003) {
                        statusChildrenTest = true
                    } else {
                        valueErrorLog?.append("Value length 255: ")?.append(lstValueItemTest[0])?.append(";")?.append("Status: ")?.append(statusWrite)
                    }
                }
            }
            CommonUUID.ID_IOP_TEST_USER_LEN_1 -> {
                if (statusRead == 0) {
                    if (statusWrite == 0 && CommonResultTest.RESULT_TEST_55_LENGTH_1 == Converters.getHexValue(characteristic.value)) {
                        statusChildrenTest = true
                    } else {
                        valueErrorLog?.append(statusWriteString)?.append(statusWrite)?.append("; Value: ")?.append(Converters.getHexValue(characteristic.value))
                    }
                }
            }
            CommonUUID.ID_IOP_TEST_USER_LEN_255 -> {
                if (statusRead == 0) {
                    if (statusWrite == 0 && compareValueCharacteristic(Converters.getHexValue(Converters.decToByteArray(createDataTestCaseLengthByte255(255))), Converters.getHexValue(characteristic.value))) {
                        statusChildrenTest = true
                    } else {
                        valueErrorLog?.append(statusWriteString)?.append(";Value: ")?.append(Converters.getHexValue(characteristic.value))
                    }
                }
            }
            CommonUUID.ID_IOP_TEST_USER_LEN_VARIABLE_4 -> {
                if (statusRead == 0) {
                    lstValueItemTest.add(Converters.getHexValue(characteristic.value))
                }
                if (lstValueItemTest.size > 1) {
                    if (lstValueItemTest[0] == CommonResultTest.RESULT_TEST_55_LENGTH_1
                            && lstValueItemTest[1] == CommonResultTest.RESULT_TEST_66_LENGTH_4
                            || lstValueItemTest[1] == CommonResultTest.RESULT_TEST_55_LENGTH_1
                            && lstValueItemTest[0] == CommonResultTest.RESULT_TEST_66_LENGTH_4) {
                        statusChildrenTest = true
                    } else {
                        valueErrorLog?.append("Test 1: ")?.append(lstValueItemTest[0])?.append(";Test 2: ")?.append(lstValueItemTest[0])
                    }
                }
            }
            CommonUUID.ID_IOP_TEST_PHASE3_SECURITY_PAIRING,
            CommonUUID.ID_IOP_TEST_PHASE3_SECURITY_AUTHENTICATION,
            CommonUUID.ID_IOP_TEST_PHASE3_SECURITY_BONDING,
            CommonUUID.ID_IOP_TEST_PHASE3_GATT_CATCHING,
            CommonUUID.ID_IOP_TEST_PHASE3_SERVICE_CHANGED_INDICATION -> {
                if (statusRead == 0 && CommonResultTest.RESULT_TEST_55_LENGTH_1 == Converters.getHexValue(characteristic.value)) {
                    statusChildrenTest = true
                } else if (statusRead != -1) {
                    valueErrorLog?.append(ErrorCodes.getErrorName(statusRead))
                }
            }
            else -> {
            }
        }
    }

    fun compareValueCharacteristic(oldValue: String, newValue: String): Boolean {
        val arOld = oldValue.trim().split(" ")
        val arNew = newValue.trim().split(" ")
        return arOld.size == arNew.size && arOld[0] == arNew[0] && arNew[arNew.size - 1] == arOld[arOld.size - 1]
    }

    private fun createDataTestCaseLengthByte255(len: Int): String {
        return StringBuilder().apply {
            for (i in 0 until len) {
                append(i).append(" ")
            }
        }.toString()
    }
}