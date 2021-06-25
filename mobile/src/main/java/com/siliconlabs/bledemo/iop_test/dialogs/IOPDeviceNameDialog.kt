package com.siliconlabs.bledemo.iop_test.dialogs

import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.base.BaseDialogFragment
import com.siliconlabs.bledemo.iop_test.activities.IOPTestActivity
import com.siliconlabs.bledemo.iop_test.models.IOPTest
import kotlinx.android.synthetic.main.dialog_iop_device_name.*

class IOPDeviceNameDialog : BaseDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_iop_device_name, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleClickEvents()
    }

    private fun handleClickEvents() {
        btn_ok.setOnClickListener {
            handleOkClick()
        }
        btn_cancel.setOnClickListener {
            dismiss()
        }
        btn_interoperability_test.setOnClickListener {
            handleClickOnInteroperabilityTest()
        }
    }

    private fun isInputCorrect(): Boolean {
        return !TextUtils.isEmpty(getFwName())
    }

    private fun getFwName(): String {
        return et_device_name.text.toString()
    }

    private fun handleOkClick() {
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
            Toast.makeText(requireContext(), R.string.toast_bluetooth_not_enabled, Toast.LENGTH_SHORT).show()
        } else if (isInputCorrect()) {
            IOPTest.createDataTest(getFwName())
            IOPTestActivity.startActivity(requireContext())
            dismiss()
        } else {
            Toast.makeText(requireContext(), requireContext().getString(R.string.iop_test_toast_enter_valid_device_name), Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleClickOnInteroperabilityTest() {
        Toast.makeText(requireContext(),"TODO",Toast.LENGTH_SHORT).show()
        //val uriUrl = Uri.parse("https://" + "TODO")
        //val launchBrowser = Intent(Intent.ACTION_VIEW, uriUrl)
        //startActivity(launchBrowser)
    }
}