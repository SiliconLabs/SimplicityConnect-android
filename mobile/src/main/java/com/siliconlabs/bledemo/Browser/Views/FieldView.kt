package com.siliconlabs.bledemo.Browser.Views

import android.content.Context
import com.siliconlabs.bledemo.Bluetooth.DataTypes.Field

abstract class FieldView(
        context: Context?,
        protected val field: Field,
        fieldValue: ByteArray
) : ValueView(context, fieldValue)