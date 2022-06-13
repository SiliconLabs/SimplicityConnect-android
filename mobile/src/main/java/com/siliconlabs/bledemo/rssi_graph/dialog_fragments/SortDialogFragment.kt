package com.siliconlabs.bledemo.rssi_graph.dialog_fragments

import android.app.Dialog
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.rssi_graph.viewmodels.RssiGraphViewModel
import kotlinx.android.synthetic.main.fragment_dialog_rssi_sort.view.*
import kotlinx.android.synthetic.main.name_with_checkmark.view.*

class SortDialogFragment : DialogFragment() {

    private lateinit var chosenSortMode: SortMode

    private lateinit var rootView: View
    private lateinit var activityViewModel: RssiGraphViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activityViewModel = ViewModelProvider(requireActivity()).get(RssiGraphViewModel::class.java)
        chosenSortMode = activityViewModel.activeSortMode.value ?: SortMode.NONE

        context?.let {
            return AlertDialog.Builder(it).apply {
                rootView = LayoutInflater.from(it).inflate(R.layout.fragment_dialog_rssi_sort, null)

                setView(rootView)
                setTitle(getString(R.string.sort_dialog_fragment_title))

                setCancelable(true)
            }.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        dialog?.setCanceledOnTouchOutside(false)

        setupUiListeners()
        rootView.apply {
            initSortModeView(sort_rssi_asc, SortMode.ASCENDING, getString(R.string.sort_label_ascending))
            initSortModeView(sort_rssi_desc, SortMode.DESCENDING, getString(R.string.sort_label_descending))
            initSortModeView(sort_name_a_to_z, SortMode.A_TO_Z, getString(R.string.sort_label_A_to_Z))
            initSortModeView(sort_name_z_to_a, SortMode.Z_TO_A, getString(R.string.sort_label_Z_to_A))
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private fun setupUiListeners() {
        rootView.apply {
            rssi_sort_button_apply.setOnClickListener {
                activityViewModel.setSortMode(chosenSortMode)
                dismiss()
            }
            rssi_sort_button_cancel.setOnClickListener { dismiss() }
        }
    }

    private fun initSortModeView(layout: View, sortMode: SortMode, name: String) {
        layout.apply {
            tv_name.text = name
            iv_checkmark.visibility = View.GONE
            if (sortMode == chosenSortMode) setSelected(layout)

            setOnClickListener {
                if (!isSelected(layout)) {
                    deselectAllCheckboxes()
                    setSelected(layout)
                    chosenSortMode = sortMode
                } else {
                    setDeselected(layout)
                    chosenSortMode = SortMode.NONE
                }
            }
        }
    }

    private fun setSelected(layout: View) {
        layout.tv_name.setTextColor(ContextCompat.getColor(requireContext(), R.color.silabs_blue))
        layout.tv_name.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        layout.iv_checkmark.visibility = View.VISIBLE
    }

    private fun setDeselected(layout: View) {
        layout.tv_name.setTextColor(ContextCompat.getColor(requireContext(), R.color.silabs_subtle_text))
        layout.tv_name.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        layout.iv_checkmark.visibility = View.GONE
    }

    private fun isSelected(layout: View) = layout.iv_checkmark.visibility == View.VISIBLE

    private fun deselectAllCheckboxes() {
        rootView.apply {
            setDeselected(sort_rssi_asc)
            setDeselected(sort_rssi_desc)
            setDeselected(sort_name_a_to_z)
            setDeselected(sort_name_z_to_a)
        }
    }

    enum class SortMode {
        A_TO_Z,
        Z_TO_A,
        ASCENDING,
        DESCENDING,
        NONE
    }
}