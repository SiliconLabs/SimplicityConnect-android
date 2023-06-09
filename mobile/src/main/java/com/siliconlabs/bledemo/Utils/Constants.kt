package com.siliconlabs.bledemo.utils

import com.siliconlabs.bledemo.features.scan.browser.models.logs.Log
import java.util.*

object Constants {
    /* TODO: pass a list of logs to service through intent parcelable instead of a constant from
        object. Requires further revising Log hierarchy class to be able to make them parcelable
        and pass to intent. */
    var LOGS: List<Log> = LinkedList()

    const val ATT_HEADER_SIZE = 3
    const val MIN_ALLOWED_MTU = 23
    const val MAX_ALLOWED_MTU = 512
}
