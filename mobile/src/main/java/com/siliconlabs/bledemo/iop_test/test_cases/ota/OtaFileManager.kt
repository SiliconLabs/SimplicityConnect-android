package com.siliconlabs.bledemo.iop_test.test_cases.ota

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.iop_test.models.IopBoard
import java.util.*

class OtaFileManager(private val context: Context) {

    var otaFilename: String? = null
    var otaFile: ByteArray? = null
    var uploadMode: UploadMode? = null

    fun findGblFile(firmwareVersion: String, boardType: IopBoard, isTestAck: Boolean) {
        otaFilename = if (isTestAck) boardType.ota1FileName else boardType.ota2FileName

        val inputStream = context.assets.open("iop/$firmwareVersion/$otaFilename")
        otaFile = ByteArray(inputStream.available())
        inputStream.read(otaFile)
    }

    fun hasCorrectFileExtension(): Boolean {
        return otaFilename?.toUpperCase(Locale.ROOT)?.contains(".GBL")!!
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