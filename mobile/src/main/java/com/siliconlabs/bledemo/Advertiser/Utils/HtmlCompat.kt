package com.siliconlabs.bledemo.advertiser.utils

import android.os.Build
import android.text.Html
import android.text.SpannableString
import android.text.Spanned

class HtmlCompat {
    companion object {
        @SuppressWarnings("deprecation")
        fun fromHtml(html: String?): Spanned? {
            return when {
                html == null -> SpannableString("")
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
                else -> Html.fromHtml(html)

            }
        }
    }
}