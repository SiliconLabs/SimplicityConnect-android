package com.siliconlabs.bledemo.rssi_graph.dialog_fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.rssi_graph.viewmodels.RssiGraphViewModel
import kotlinx.android.synthetic.main.fragment_dialog_rssi_filter.view.*

class FilterDialogFragment : DialogFragment() {

    private lateinit var chosenFilters: MutableMap<FilterType, String>

    private lateinit var rootView: View
    private lateinit var activityViewModel: RssiGraphViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activityViewModel = ViewModelProvider(requireActivity()).get(RssiGraphViewModel::class.java)
        chosenFilters = activityViewModel.activeFilters.value ?: mutableMapOf()

        context?.let {
            return AlertDialog.Builder(it).apply {
                rootView = LayoutInflater.from(it).inflate(R.layout.fragment_dialog_rssi_filter, null)

                setView(rootView)
                setTitle(getString(R.string.filter_dialog_fragment_title))

                setCancelable(true)
            }.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        dialog?.setCanceledOnTouchOutside(false)

        setupUiListeners()
        initUiValues()
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private fun setupUiListeners() {
        rootView.apply {
            rssi_filter_button_cancel.setOnClickListener { dismiss() }
            rssi_filter_button_reset.setOnClickListener {
                activityViewModel.applyFilters(mutableMapOf())
                dismiss()
            }
            rssi_filter_button_apply.setOnClickListener {
                activityViewModel.applyFilters(chosenFilters)
                dismiss()
            }
            rssi_filter_et_search_device_name.addTextChangedListener(searchDeviceNameListener)
            rssi_filter_iv_clear_device_name.setOnClickListener { rssi_filter_et_search_device_name.text.clear() }
            rssi_filter_seekbar.setOnSeekBarChangeListener(rssiSeekbarListener)
        }
    }

    private fun initUiValues() {
        rootView.apply {
            rssi_filter_et_search_device_name.setText(chosenFilters[FilterType.NAME_OR_ADDRESS] ?: "")
            rssi_filter_seekbar.progress =
                    chosenFilters[FilterType.RSSI]?.toInt()?.plus(SEEKBAR_OFFSET) ?: 0
        }
    }

    private val searchDeviceNameListener = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) { }
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            if (s.count() <= 0) {
                rootView.rssi_filter_iv_clear_device_name.visibility = View.GONE
                chosenFilters.remove(FilterType.NAME_OR_ADDRESS)
            } else {
                rootView.rssi_filter_iv_clear_device_name.visibility = View.VISIBLE
                chosenFilters[FilterType.NAME_OR_ADDRESS] = s.toString()
            }
        }
        override fun afterTextChanged(s: Editable) { }
    }

    private val rssiSeekbarListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (progress == 0) {
                rootView.rssi_filter_tv_rssi_value.text = getString(R.string.filter_rssi_not_set)
                chosenFilters.remove(FilterType.RSSI)
            } else {
                val rssi = progress - SEEKBAR_OFFSET
                rootView.rssi_filter_tv_rssi_value.text = getString(R.string.n_dBm, rssi)
                chosenFilters[FilterType.RSSI] = rssi.toString()
            }
        }
        override fun onStartTrackingTouch(seekBar: SeekBar) {}
        override fun onStopTrackingTouch(seekBar: SeekBar) {}
    }

    enum class FilterType {
        NAME_OR_ADDRESS,
        RSSI
    }

    companion object {
        private const val SEEKBAR_OFFSET = 130
    }
}