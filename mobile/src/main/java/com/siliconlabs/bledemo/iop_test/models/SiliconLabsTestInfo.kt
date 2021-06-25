package com.siliconlabs.bledemo.iop_test.models

import android.util.Log
import com.siliconlabs.bledemo.iop_test.utils.Utils
import java.text.SimpleDateFormat
import java.util.*

class SiliconLabsTestInfo(var fwName: String, val listItemTest: ArrayList<ItemTestCaseInfo>) {
    private var mLstValuesParameters: ArrayList<String>? = null
    private var mLstValuesPlatform: ArrayList<String>? = null
    private var phoneOs: String = Utils.getAndroidVersion()
    var phoneName: String = Utils.getDeviceName()
    var totalTestCase: Int

    init {
        this.totalTestCase = getTotalTestCase(listItemTest)
    }

    fun setLstValuesPlatform(mLstValuesPlatform: ArrayList<String>?) {
        this.mLstValuesPlatform = mLstValuesPlatform
        val connection_parameters_tag = "connection_parameters"
        Log.d(connection_parameters_tag, "mtu_size " + getValuesParameters(0))
        Log.d(connection_parameters_tag, "pdu_size " + getValuesParameters(1))
        Log.d(connection_parameters_tag, "interval " + getValuesParameters(2))
        Log.d(connection_parameters_tag, "latency " + getValuesParameters(3))
        Log.d(connection_parameters_tag, "supervision_timeout " + getValuesParameters(4))
    }

    fun setLstValuesParameters(mLstValuesParameters: ArrayList<String>) {
        this.mLstValuesParameters = mLstValuesParameters
    }

    private fun getTotalTestCase(listItemTest: ArrayList<ItemTestCaseInfo>): Int {
        var count = 0
        for (item in listItemTest) {
            item.listChildrenItem?.let {
                count += it.size
            }
        }
        return count + 6
    }

    private fun getDate(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    private fun logDataTest(): String {
        return StringBuilder().apply {
            for (itemTest in listItemTest) {
                if (itemTest.listChildrenItem != null && itemTest.listChildrenItem?.size!! > 0) {
                    for (children in itemTest.listChildrenItem!!) {
                        append("\tTest case ")

                        when {
                            children.id >= 11 -> append(itemTest.idTest + 1)
                            itemTest.idTest == 8 -> append(7)
                            itemTest.idTest == 9 -> append(7)
                            else -> append(itemTest.idTest)
                        }
                        append(".")

                        when {
                            children.id >= 11 -> append(children.id - 10)
                            itemTest.idTest < 6 -> append(children.id)
                            itemTest.idTest == 8 -> append(children.id + 1)
                            else -> append(children.id + 4)
                        }
                        append(",")

                        when {
                            children.statusChildrenTest -> append("Pass")
                            children.getValueErrorLog() == "N/A" -> append("")
                            else -> append(FAILED)
                        }

                        if (itemTest.idTest == 4) {
                            when {
                                children.id in 7..10 -> append("," + children.getValueErrorLog() + ".\n")
                                children.statusChildrenTest -> append(".\n")
                                else -> append(children.getValueErrorLog() + ".\n")
                            }
                        } else {
                            if (children.statusChildrenTest) {
                                append(".\n")
                            } else {
                                append(children.getValueErrorLog() + ".\n")
                            }
                        }

                    }
                } else {
                    append("\tTest case ")

                    when {
                        itemTest.idTest < 5 -> append(itemTest.idTest)
                        itemTest.idTest == 7 -> append("7.1")
                        itemTest.idTest == 5 -> append("6.1")
                        itemTest.idTest == 6 -> append("6.2")
                        else -> append(itemTest.idTest + 1)
                    }
                    append(",")

                    if (itemTest.idTest == 7) {
                        append("N/A,")
                    } else {
                        append(itemTest.getValueStatusTest())
                    }

                    if (itemTest.idTest <= 3 && itemTest.getValueStatusTest() == FAILED) {
                        append(",")
                    }

                    if (itemTest.idTest == 7) {
                        append(itemTest.getThroughputPassedTestcase())
                    } else {
                        if (itemTest.idTest <= 3 && itemTest.getValueStatusTest() == FAILED) {
                            append(itemTest.getTimePassedTestCase())
                        }
                    }
                    append(".\n")
                }
            }
        }.toString()
    }

    /**
     * Get values parameters by index
     * 0: MTU
     * 1: PDU
     * 2: interval
     * 3: latency
     * 4: supervision_timeout
     */
    private fun getValuesParameters(index: Int): String {
        mLstValuesParameters?.let {
            if (index < it.size) {
                return it[index].trim()
            }
        }
        return ""
    }

    /**
     * Get values platform by index
     * 0: Minor version(firmware stack version)
     * 1: ic name
     *
     * @param index
     * @return
     */
    fun getValuesPlatform(index: Int): String {
        mLstValuesPlatform?.let {
            if (index < it.size) {
                return it[index].trim()
            }
        }
        return ""
    }

    fun getIopBoard(name: String): IopBoard {
        return when (name) {
            "0" -> IopBoard.UNKNOWN
            "1" -> IopBoard.BRD_4104A
            "2" -> IopBoard.BRD_4181A
            "3" -> IopBoard.BRD_4181B
            "4" -> IopBoard.BRD_4182A
            else -> IopBoard.UNKNOWN
        }
    }

    override fun toString(): String {
        val fInterval: Float
        val sInterval = getValuesParameters(2)
        fInterval = sInterval.toFloat() * 1.25f
        return StringBuilder().apply {
            append("<timestamp>").append(getDate()).append("</timestamp>").append("\n")
            append("<phone_informations>").append("\n")
            append("\t").append("<phone_name>").append(phoneName).append("</phone_name>").append("\n")
            append("\t").append("<phone_os_version>").append(phoneOs).append("</phone_os_version>").append("\n")
            append("</phone_informations>").append("\n")
            append("<firmware_informations>").append("\n")
            append("\t").append("<firmware_version>").append(getValuesPlatform(0)).append("</firmware_version>").append("\n")
            append("\t").append("<firmware_ic_name>").append(getIopBoard(getValuesPlatform(1)).icName.text).append("</firmware_ic_name>").append("\n")
            append("\t").append("<firmware_name>").append(fwName).append("</firmware_name>").append("\n")
            append("</firmware_informations>").append("\n")
            append("<connection_parameters>").append("\n")
            append("\t").append("<mtu_size>").append(getValuesParameters(0)).append("</mtu_size>").append("\n")
            append("\t").append("<pdu_size>").append(getValuesParameters(1)).append("</pdu_size>").append("\n")
            append("\t").append("<interval>").append(fInterval).append("</interval>").append("\n")
            append("\t").append("<latency>").append(getValuesParameters(3)).append("</latency>").append("\n")
            append("\t").append("<supervision_timeout>").append(getValuesParameters(4)).append("</supervision_timeout>").append("\n")
            append("</connection_parameters>").append("\n")
            append("<test_results>").append("\n")
            append(logDataTest())
            append("</test_results>").append("\n")
        }.toString()
    }

    companion object {
        private const val FAILED = "Failed"
    }
}