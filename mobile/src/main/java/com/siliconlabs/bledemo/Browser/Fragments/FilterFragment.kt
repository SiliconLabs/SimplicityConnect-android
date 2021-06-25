package com.siliconlabs.bledemo.Browser.Fragments

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.siliconlabs.bledemo.BeaconUtils.BleFormat
import com.siliconlabs.bledemo.Browser.Adapters.BeaconTypeAdapter
import com.siliconlabs.bledemo.Browser.Adapters.SavedSearchesAdapter
import com.siliconlabs.bledemo.Browser.Adapters.SavedSearchesAdapter.SavedSearchesCallback
import com.siliconlabs.bledemo.Browser.Models.BeaconType
import com.siliconlabs.bledemo.Browser.Models.SavedSearch
import com.siliconlabs.bledemo.Browser.ToolbarCallback
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.Utils.FilterDeviceParams
import com.siliconlabs.bledemo.Utils.SharedPrefUtils
import kotlinx.android.synthetic.main.fragment_filter.*
import java.util.*

class FilterFragment : Fragment() {
    private var toolbarCallback: ToolbarCallback? = null
    private lateinit var beaconTypeadapter: BeaconTypeAdapter
    private lateinit var savedSearchesAdapter: SavedSearchesAdapter
    private lateinit var sharedPrefUtils: SharedPrefUtils
    private var rssiFlag = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_filter, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPrefUtils = SharedPrefUtils(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setSearchBox(et_search_device_name, iv_clear_device_name)
        setSearchBox(et_search_packet_content, iv_clear_packet_content)
        et_search_packet_content.filters = arrayOf<InputFilter>(InputFilter.AllCaps())

        initBeaconRecyclerView()
        initSavedSearchesRecyclerView()

        setListeners()
        resetUi()
    }

    private fun initBeaconRecyclerView() {
        val list = ArrayList<BeaconType>()
        val bleFormats: List<BleFormat> = ArrayList(listOf(*BleFormat.values()))
        for (bf in bleFormats) {
            list.add(BeaconType(resources.getString(bf.nameResId), false, bf))
        }

        beaconTypeadapter = BeaconTypeAdapter(list, requireContext())
        rv_beacon_type.layoutManager = LinearLayoutManager(activity)
        rv_beacon_type.addItemDecoration(DividerItemDecoration(rv_beacon_type.context, LinearLayoutManager.VERTICAL))
        rv_beacon_type.adapter = beaconTypeadapter
    }

    private fun initSavedSearchesRecyclerView() {
        val searchList = ArrayList<SavedSearch>()
        for ((key) in sharedPrefUtils.mapFilter) {
            searchList.add(SavedSearch((key as String)))
        }
        savedSearchesAdapter = SavedSearchesAdapter(searchList, requireContext(), object : SavedSearchesCallback {
            override fun onClick(name: String?) {
                val f = sharedPrefUtils.mapFilter[name]
                sharedPrefUtils.lastFilter = f
                f?.let { fillFields(f) }
                savedSearchesAdapter.notifyDataSetChanged()
            }

        })

        rv_saved_searches.layoutManager = LinearLayoutManager(activity)
        rv_saved_searches.addItemDecoration(DividerItemDecoration(rv_saved_searches.context, LinearLayoutManager.VERTICAL))
        rv_saved_searches.adapter = savedSearchesAdapter
    }


    fun setCallback(toolbarCallback: ToolbarCallback?): FilterFragment {
        this.toolbarCallback = toolbarCallback
        return this
    }

    private fun setSearchBox(etSearch: EditText, ivClear: ImageView) {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                ivClear.visibility = if (count <= 0) View.GONE else View.VISIBLE
            }

            override fun afterTextChanged(s: Editable) {}
        })
        ivClear.setOnClickListener { etSearch.text.clear() }
    }

    private fun setListeners() {
        iv_close.setOnClickListener { toolbarCallback?.close() }
        btn_reset.setOnClickListener { resetUi(); submit(true) }
        btn_save.setOnClickListener(View.OnClickListener {
            val key = prepareFilterName()
            if (key == "") return@OnClickListener
            if (!sharedPrefUtils.mapFilter.containsKey(key)) {
                val filterDeviceParams = prepareFilterDeviceParam()
                sharedPrefUtils.addToMapFilterAndSave(key, filterDeviceParams)
                sharedPrefUtils.lastFilter = filterDeviceParams
                savedSearchesAdapter.addItem(SavedSearch(key))
                savedSearchesAdapter.notifyDataSetChanged()
            }
        })

        sb_rssi.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                rssiFlag = true
                val rssi = getRssiValue(progress)
                tv_rssi_value.text = resources.getString(R.string.n_dBm, rssi)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        btn_search.setOnClickListener { submit(true) }

        iv_beacon_arrow.setOnClickListener { //If after clicking arrow is up then...
            if (iv_beacon_arrow.tag == "ARROW_UP") {
                iv_beacon_arrow.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_down_on))
                rv_beacon_type.visibility = View.GONE
                iv_beacon_arrow.tag = "ARROW_DOWN"
            } else {
                iv_beacon_arrow.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_up_on))
                rv_beacon_type.visibility = View.VISIBLE
                iv_beacon_arrow.tag = "ARROW_UP"
            }
        }

        iv_save_arrow.setOnClickListener {
            if (iv_save_arrow.tag == "ARROW_UP") {
                iv_save_arrow.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_down_on))
                rv_saved_searches.visibility = View.GONE
                iv_save_arrow.tag = "ARROW_DOWN"
            } else {
                iv_save_arrow.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_up_on))
                rv_saved_searches.visibility = View.VISIBLE
                iv_save_arrow.tag = "ARROW_UP"
            }
        }
    }

    private fun prepareFilterName(): String {
        return StringBuilder().apply {
            val separator = " + "

            if (rssiFlag) append("RSSI > ").append(tv_rssi_value.text).append(separator)

            val selectedBeacons = selectedBeacons
            if (selectedBeacons.isNotEmpty()) {
                append("Beacon type ")
                for (i in selectedBeacons.indices) {
                    append("\"").append(resources.getString(selectedBeacons[i].nameResId)).append("\"")
                    if (i != selectedBeacons.size - 1) append(", ")
                }
                append(separator)
            }

            if (et_search_device_name.text.toString() != "") append("Name \"").append(et_search_device_name.text.toString()).append("\"").append(separator)
            if (et_search_packet_content.text.toString() != "") append("Packet content \"").append(et_search_packet_content.text.toString()).append("\"").append(separator)
            if (cb_only_favourites.isChecked) append("Only favourites").append(separator)
            if (cb_only_connectable.isChecked) append("Only connectable").append(separator)
            if (cb_only_bonded.isChecked) append("Only bonded")
            if (toString().endsWith(separator)) return substring(0, length - 3)

        }.toString()
    }

    private fun getRssiValue(progress: Int): Int {
        return progress - 100
    }

    private fun resetUi() {
        et_search_device_name.setText("")
        et_search_packet_content.setText("")
        sb_rssi.progress = 0
        tv_rssi_value.setText(R.string.filter_rssi_not_set)
        rssiFlag = false
        cb_only_favourites.isChecked = false
        cb_only_connectable.isChecked = false
        cb_only_bonded.isChecked = false
        beaconTypeadapter.selectBeacons(emptyList())
    }

    private fun fillFields(filterDeviceParams: FilterDeviceParams) {
        et_search_device_name.setText(filterDeviceParams.name)
        et_search_packet_content.setText(filterDeviceParams.advertising)
        sb_rssi.progress = filterDeviceParams.rssiValue + 100
        sb_rssi.refreshDrawableState()
        val rssi = filterDeviceParams.rssiValue
        rssiFlag = filterDeviceParams.isRssiFlag
        tv_rssi_value.text = if (rssiFlag) resources.getString(R.string.n_dBm, rssi) else context?.getString(R.string.filter_rssi_not_set)
        val selectedBleFormats = filterDeviceParams.bleFormats
        beaconTypeadapter.selectBeacons(selectedBleFormats)
        cb_only_favourites.isChecked = filterDeviceParams.isOnlyFavourite
        cb_only_connectable.isChecked = filterDeviceParams.isOnlyConnectable
        cb_only_bonded.isChecked = filterDeviceParams.isOnlyBonded
    }

    private val selectedBeacons: List<BleFormat>
        get() {
            val selectedBeacons: MutableList<BleFormat> = ArrayList()
            for (b in beaconTypeadapter.beaconTypeList) {
                if (b.isChecked) selectedBeacons.add(b.bleFormat)
            }
            return selectedBeacons
        }

    private fun prepareFilterDeviceParam(): FilterDeviceParams {
        val name = et_search_device_name.text.toString()
        val advertising = et_search_packet_content.text.toString()
        val rssi = getRssiValue(sb_rssi.progress)
        return FilterDeviceParams(prepareFilterName(), name, advertising, rssi, rssiFlag, selectedBeacons, cb_only_favourites.isChecked, cb_only_connectable.isChecked, cb_only_bonded.isChecked)
    }

    private fun submit(closeToolbar: Boolean) {
        val filterDeviceParams = prepareFilterDeviceParam()
        sharedPrefUtils.lastFilter = filterDeviceParams
        toolbarCallback?.submit(filterDeviceParams, closeToolbar)
    }
}