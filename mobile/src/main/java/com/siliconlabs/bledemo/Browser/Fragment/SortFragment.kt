package com.siliconlabs.bledemo.Browser.Fragment

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.siliconlabs.bledemo.R
import kotlinx.android.synthetic.main.fragment_sort.*

class SortFragment : Fragment() {
    private var sortCallback: SortCallback? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sort, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        handleSortSelection(sort_rssi_asc, SortMode.RSSI_ASC, getString(R.string.sort_label_ascending))
        handleSortSelection(sort_rssi_desc, SortMode.RSSI_DESC, getString(R.string.sort_label_descending))
        handleSortSelection(sort_name_a_to_z, SortMode.NAME_A_TO_Z, getString(R.string.sort_label_A_to_Z))
        handleSortSelection(sort_name_z_to_a, SortMode.NAME_Z_TO_A, getString(R.string.sort_label_Z_to_A))
    }

    private fun handleSortSelection(layout: View, sortMode: SortMode, name: String) {
        val tvName = layout.findViewById(R.id.tv_name) as TextView
        val ivCheckmark = layout.findViewById(R.id.iv_checkmark) as ImageView
        tvName.text = name
        ivCheckmark.visibility = View.GONE

        layout.setOnClickListener {
            click(layout)

            if (isSelected(layout)) {
                deselectAllCheckboxes()
                setSelected(layout)
                sortCallback?.setSortMode(sortMode)
            } else {
                setDeselected(layout)
                sortCallback?.setSortMode(SortMode.NONE)
            }
        }
    }

    private fun click(layout: View) {
        if (isSelected(layout)) setDeselected(layout)
        else setSelected(layout)
    }

    private fun setSelected(layout: View) {
        val tvName = layout.findViewById(R.id.tv_name) as TextView
        val ivCheckmark = layout.findViewById(R.id.iv_checkmark) as ImageView

        tvName.setTextColor(ContextCompat.getColor(requireContext(), R.color.silabs_blue))
        tvName.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        ivCheckmark.visibility = View.VISIBLE
    }

    private fun setDeselected(layout: View) {
        val tvName = layout.findViewById(R.id.tv_name) as TextView
        val ivCheckmark = layout.findViewById(R.id.iv_checkmark) as ImageView

        tvName.setTextColor(ContextCompat.getColor(requireContext(), R.color.silabs_subtle_text))
        tvName.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        ivCheckmark.visibility = View.GONE
    }

    private fun isSelected(layout: View): Boolean {
        return layout.findViewById<ImageView>(R.id.iv_checkmark).visibility == View.VISIBLE
    }

    private fun deselectAllCheckboxes() {
        setDeselected(sort_rssi_asc)
        setDeselected(sort_rssi_desc)
        setDeselected(sort_name_a_to_z)
        setDeselected(sort_name_z_to_a)
    }

    fun setCallback(sortCallback: SortCallback?): SortFragment {
        this.sortCallback = sortCallback
        return this
    }

}

enum class SortMode(val resId: Int) {
    RSSI_ASC(R.drawable.ic_sort_asc),
    RSSI_DESC(R.drawable.ic_sort_desc),
    NAME_A_TO_Z(R.drawable.ic_sort_a_z),
    NAME_Z_TO_A(R.drawable.ic_sort_z_a),
    NONE(R.drawable.ic_sort)
}

interface SortCallback {
    fun setSortMode(mode: SortMode)
}
