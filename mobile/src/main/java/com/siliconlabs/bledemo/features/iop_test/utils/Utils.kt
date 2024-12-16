package com.siliconlabs.bledemo.features.iop_test.utils

import android.os.Build
import android.text.TextUtils

class Utils {
    companion object {
        private fun capitalize(str: String): String {
            if (TextUtils.isEmpty(str)) {
                return str
            }
            val arr = str.toCharArray()
            var capitalizeNext = true
            var phrase = ""
            for (c in arr) {
                if (capitalizeNext && Character.isLetter(c)) {
                    phrase += Character.toUpperCase(c)
                    capitalizeNext = false
                    continue
                } else if (Character.isWhitespace(c)) {
                    capitalizeNext = true
                }
                phrase += c
            }
            return phrase
        }

        fun getAndroidVersion(): String {
            val release = Build.VERSION.RELEASE
            val sdkVersion = Build.VERSION.SDK_INT
            return "Android_SDK:_" + release + "_" + sdkVersion
        }

        fun getDeviceName(): String {
            val manufacturer = Build.MANUFACTURER
            val model = Build.MODEL
            if (model.startsWith(manufacturer)) {
                return capitalize(model)
            }
            return (capitalize(manufacturer) + "_" + model).replace(" ", "_")
        }
    }

}