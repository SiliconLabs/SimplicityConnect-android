package com.siliconlabs.bledemo.Browser.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.Browser.Adapters.MappingAdapter
import com.siliconlabs.bledemo.Browser.Model.Mapping
import com.siliconlabs.bledemo.Browser.Model.MappingType
import com.siliconlabs.bledemo.Utils.SharedPrefUtils
import kotlinx.android.synthetic.main.fragment_characteristic_mappings.view.*
import java.util.*

class CharacteristicMappingsFragment : Fragment() {
    private var map: HashMap<String, Mapping>? = null
    private var list: List<Mapping>? = null
    private var sharedPrefUtils: SharedPrefUtils? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPrefUtils = SharedPrefUtils(activity)
        map = sharedPrefUtils?.characteristicNamesMap
        list = ArrayList(map!!.values)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_characteristic_mappings, container, false)
        val adapter = MappingAdapter(list, activity, MappingType.CHARACTERISTIC)
        view.rv_characteristics.layoutManager = LinearLayoutManager(activity)
        view.rv_characteristics.adapter = adapter

        return view
    }

    override fun onPause() {
        super.onPause()
        map?.clear()
        for (mapping in list!!) {
            map!![mapping.uuid] = mapping
        }
        sharedPrefUtils?.saveCharacteristicNamesMap(map)
    }
}