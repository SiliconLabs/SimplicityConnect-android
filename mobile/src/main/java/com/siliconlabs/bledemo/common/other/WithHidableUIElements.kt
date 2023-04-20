package com.siliconlabs.bledemo.common.other

import android.content.Context

interface WithHidableUIElements {
    fun getLayoutManagerWithHidingUIElements(context: Context?): LinearLayoutManagerWithHidingUIElements
}