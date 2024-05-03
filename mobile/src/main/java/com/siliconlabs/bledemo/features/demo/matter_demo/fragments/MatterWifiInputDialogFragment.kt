package com.siliconlabs.bledemo.features.demo.matter_demo.fragments

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.DialogConfigureWifiMatterBinding
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterOTBRInputDialogFragment.Companion.WINDOW_SIZE
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.FragmentUtils

class MatterWifiInputDialogFragment : DialogFragment() {

    private lateinit var binding: DialogConfigureWifiMatterBinding
    private var flag: Boolean = false

    companion object {
        fun newInstance(): MatterWifiInputDialogFragment = MatterWifiInputDialogFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (!flag) {
            flag = true
            binding = DialogConfigureWifiMatterBinding.inflate(inflater, container, false)
        }
        if (dialog != null && dialog!!.window != null) {
            dialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog!!.window!!.requestFeature(Window.FEATURE_NO_TITLE)
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        val dialog: Dialog? = dialog
        if (dialog != null) {
            dialog.window!!
                .setLayout(
                    (getScreenWidth(requireActivity()) * WINDOW_SIZE).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners(view)
    }

    private fun setupClickListeners(view: View) {
        binding.positiveBtn.setOnClickListener {
            val wifiSSID = binding.editNetworkSSID.text.toString().trim()
            val wifiPassword = binding.editWiFiPassword.text.toString().trim()
            if (wifiSSID.isNullOrBlank() || wifiPassword.isNullOrBlank()) {
                Toast.makeText(requireContext(),
                    getString(R.string.ssid_and_password_required), Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            if (!FragmentUtils.isPasswordValid(wifiPassword)) {
                Toast.makeText(requireContext(),
                    getString(R.string.password_must_be_min_8_characters), Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            val intent = Intent()
            intent.putExtra(
                MatterScannerFragment.WIFI_INPUT_SSID,
                wifiSSID
            )
            intent.putExtra(
                MatterScannerFragment.WIFI_INPUT_PASSWORD,
                wifiPassword
            )
            targetFragment?.onActivityResult(
                targetRequestCode, MatterScannerFragment.INPUT_WIFI_REQ_CODE,
                intent
            )
            dismiss()
        }

        binding.negativeBtn.setOnClickListener {
            dismiss()
            if (requireActivity().supportFragmentManager.backStackEntryCount > 0) {
                requireActivity().supportFragmentManager.popBackStack();
            } else {
                FragmentUtils.getHost(
                    this@MatterWifiInputDialogFragment, CallBackHandler::class.java
                ).onBackHandler()
            }
        }
    }
    interface CallBackHandler {
        fun onBackHandler()
    }
    private fun getScreenWidth(activity: Activity): Int {
        val size = Point()
        activity.windowManager.defaultDisplay.getSize(size)
        return size.x
    }

    fun stopDisplay() {
        dismiss()
    }

    override fun onDestroy() {
        super.onDestroy()
        flag = false
    }
}