package com.siliconlabs.bledemo.features.scan.browser.dialogs

import android.bluetooth.BluetoothGatt
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Toast
import com.siliconlabs.bledemo.base.fragments.BaseDialogFragment
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.DialogRequestPriorityBinding

class ConnectionRequestDialog(
        private val currentPriority: Int,
        private val callback: Callback
) : BaseDialogFragment(
        hasCustomWidth = true,
        isCanceledOnTouchOutside = true
) {

    private lateinit var _binding: DialogRequestPriorityBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = DialogRequestPriorityBinding.inflate(inflater)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setupUiListeners()
    }

    private fun initViews() {
        _binding.apply { when (currentPriority) {
          BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER -> lowPriority.isChecked = true
          BluetoothGatt.CONNECTION_PRIORITY_BALANCED -> balancedPriority.isChecked = true
          BluetoothGatt.CONNECTION_PRIORITY_HIGH -> highPriority.isChecked = true
        } }
    }

    private fun setupUiListeners() {
        _binding.apply {
            lowPriority.setOnClickListener {
                if ((it as CheckBox).isChecked) {
                    clearCheckBoxes()
                    it.isChecked = true
                }
            }
            balancedPriority.setOnClickListener {
                if ((it as CheckBox).isChecked) {
                    clearCheckBoxes()
                    it.isChecked = true
                }
            }
            highPriority.setOnClickListener {
                if ((it as CheckBox).isChecked) {
                    clearCheckBoxes()
                    it.isChecked = true
                }
            }
            request.setOnClickListener {
                getPriority()?.let { priority ->
                    callback.onConnectionPriorityRequested(priority)
                    dismiss()
                } ?: Toast.makeText(requireContext(), getString(
                        R.string.no_priority_chosen), Toast.LENGTH_SHORT).show()
            }
            cancelRequest.setOnClickListener { dismiss() }
        }
    }

    private fun clearCheckBoxes() {
        _binding.apply {
            lowPriority.isChecked = false
            balancedPriority.isChecked = false
            highPriority.isChecked = false
        }
    }

    private fun getPriority() : Int? {
        return _binding.let {
            when {
                it.lowPriority.isChecked -> BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER
                it.balancedPriority.isChecked -> BluetoothGatt.CONNECTION_PRIORITY_BALANCED
                it.highPriority.isChecked -> BluetoothGatt.CONNECTION_PRIORITY_HIGH
                else -> null
            }
        }
    }

    interface Callback {
        fun onConnectionPriorityRequested(priority: Int)
    }

}