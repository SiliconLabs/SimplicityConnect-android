package com.siliconlabs.bledemo.features.demo.wifi_ota_update

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import com.siliconlabs.bledemo.R
import java.util.*

class WiFiOtaFileManager(private val context: Context) {

    var otaFilename: String? = null
    var otaFile: ByteArray? = null

    fun hasCorrectFileExtensionRPS(): Boolean {
        return otaFilename?.toUpperCase(Locale.ROOT)?.contains(".RPS")!!
    }

    fun readFilename(uri: Uri) {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    result = c.getString(c.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
                c.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        otaFilename = result
    }

    fun readFile(uri: Uri) {
        context.contentResolver.openInputStream(uri)?.let {
            otaFile = ByteArray(it.available())
            it.read(otaFile)
        } ?: Toast.makeText(context, context.getString(R.string.problem_while_preparing_the_file),
                Toast.LENGTH_LONG).show()
    }

    enum class UploadMode {
        AUTO,
        USER
    }

}