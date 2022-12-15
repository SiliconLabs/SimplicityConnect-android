package com.siliconlabs.bledemo.features.scan.browser.views

import android.content.Context
import com.siliconlabs.bledemo.bluetooth.data_types.Field

abstract class FieldView(
        context: Context?,
        protected val field: Field,
        fieldValue: ByteArray
) : ValueView(context, fieldValue)