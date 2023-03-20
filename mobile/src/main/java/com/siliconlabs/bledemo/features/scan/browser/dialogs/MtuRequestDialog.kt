package com.siliconlabs.bledemo.features.scan.browser.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.siliconlabs.bledemo.base.fragments.BaseDialogFragment
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.DialogRequestMtuBinding

class MtuRequestDialog(
        private val currentMTU: Int,
        private val callback: Callback
) : BaseDialogFragment(
        hasCustomWidth = true,
        isCanceledOnTouchOutside = true
) {

    private lateinit var _binding: DialogRequestMtuBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = DialogRequestMtuBinding.inflate(inflater)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUiListeners()
        initSeekbarValues()
    }

    private fun initSeekbarValues() {
        _binding.seekBarMtu.apply {
            textviewSeekbarMin.text = resources.getInteger(R.integer.mtu_min_value).toString()
            textviewSeekbarMax.text = resources.getInteger(R.integer.mtu_max_value).toString()
            seekControlBar.max = resources.getInteger(R.integer.mtu_max_value) - resources.getInteger(R.integer.mtu_min_value)
            seekControlBar.progress = getProgressBarValue(currentMTU)
        }
    }

    private fun setupUiListeners() {        _binding.apply {

            request.setOnClickListener {
                callback.onMtuRequested(getMtuValue(seekBarMtu.seekControlBar.progress))
                dismiss()
            }
            cancelRequest.setOnClickListener { dismiss() }

            seekBarMtu.seekControlBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    seekBarMtu.seekControlText.text = getMtuValue(progress).toString()
                }
            })
        }
    }

    private fun getProgressBarValue(realMtu: Int) = realMtu - resources.getInteger(R.integer.mtu_min_value)
    private fun getMtuValue(progressBarValue: Int) = progressBarValue + resources.getInteger(R.integer.mtu_min_value)

    interface Callback {
        fun onMtuRequested(requestedMtu: Int)
    }
}