/*
 * Bluegiga’s Bluetooth Smart Android SW for Bluegiga BLE modules
 * Contact: support@bluegiga.com.
 *
 * This is free software distributed under the terms of the MIT license reproduced below.
 *
 * Copyright (c) 2013, Bluegiga Technologies
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files ("Software")
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF
 * ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A  PARTICULAR PURPOSE.
 */
package com.siliconlabs.bledemo.iop_test.models

import android.util.Log
import java.util.*

class ItemTestCaseInfo(var idTest: Int, var titlesTest: String, var describe: String, var listChildrenItem: ArrayList<ChildrenItemTestInfo>?, private var statusTest: Int) {
    private var throughputBytePerSec: Int = 0
    private var throughputAcceptable: Int = 0
    private var timeEnd: Long = 0
    var timeStart: Long = 0

    fun setThroughputBytePerSec(throughput: Int, acceptable: Int) {
        throughputBytePerSec = throughput
        throughputAcceptable = acceptable

        statusTest = if (throughput > acceptable) {
            Common.IOP3_TC_STATUS_PASS //pass
        } else {
            Common.IOP3_TC_STATUS_FAILED //fail
        }
    }

    fun getThroughputPassedTestcase(): String {
        return StringBuilder().apply {
            append("(Throughput: ")
            append(throughputBytePerSec)
            append(" Bytes/s;")
            append("Acceptable Throughput: ")
            append(throughputAcceptable)
            append(" Bytes/s)")
        }.toString()
    }

    fun getStatusTest(): Int {
        return statusTest
    }

    fun setStatusTest(statusTest: Int) {
        this.statusTest = statusTest
        listChildrenItem?.let { list ->
            if (statusTest == 0 && list.size > 0) {
                for (itemTestInfo in list) {
                    itemTestInfo.statusChildrenTest = false
                }
            }
        }
    }

    fun getTimeEnd(): Long {
        return timeEnd
    }

    fun setTimeEnd(timeEnd: Long) {
        if (idTest == 3) {
            statusTest = if (timeEnd - timeStart <= Common.TIME_OUT_DISCOVER) {
                Common.IOP3_TC_STATUS_PASS
            } else {
                Common.IOP3_TC_STATUS_FAILED
            }
        } else if (idTest < 5) {
            Log.d(TAG, "idTest < 5, timeEnd - timeStart = ${timeEnd - timeStart}")
            statusTest = if (timeEnd - timeStart <= Common.TIME_OUT_TEST) {
                Common.IOP3_TC_STATUS_PASS
            } else {
                Common.IOP3_TC_STATUS_FAILED
            }
        }
    }

    fun getValueStatusTest(): String {
        return when (statusTest) {
            Common.IOP3_TC_STATUS_FAILED -> "Fail"
            Common.IOP3_TC_STATUS_PASS -> "Pass"
            Common.IOP3_TC_STATUS_PROCESSING -> "Running"
            Common.IOP3_TC_STATUS_NOT_RUN -> "N/A"
            Common.IOP3_TC_STATUS_WAITING -> "Waiting"
            else -> "Waiting"
        }
    }

    fun checkStatusItemService() {
        if (listChildrenItem != null && listChildrenItem?.size!! > 0) {
            for (item in listChildrenItem!!) {
                if (item.statusRunTest != Common.IOP3_TC_STATUS_PASS || !item.statusChildrenTest) { //1 is finished
                    statusTest = Common.IOP3_TC_STATUS_FAILED
                    return
                } else {
                    statusTest = Common.IOP3_TC_STATUS_PASS
                }
            }
        } else {
            if (getStatusTest() != Common.IOP3_TC_STATUS_PASS) {
                statusTest = Common.IOP3_TC_STATUS_FAILED
            }
        }
    }

    fun getTimePassedTestCase(): String {
        return StringBuilder().apply {
            append("(Testing time: ")
            append(timeEnd - timeStart)
            append("ms")
            append(";")
            append("Acceptable time: ")
            append(if (idTest == 3) Common.TIME_OUT_DISCOVER else Common.TIME_OUT_TEST)
            append("ms)")
        }.toString()
    }

    companion object {
        private const val TAG = "IOPTest"
    }
}