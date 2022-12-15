package com.siliconlabs.bledemo.features.scan.browser.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.siliconlabs.bledemo.features.scan.browser.adapters.DictionaryEntryAdapter
import com.siliconlabs.bledemo.features.scan.browser.models.Mapping
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.common.other.CardViewListDecoration
import com.siliconlabs.bledemo.utils.SharedPrefUtils
import kotlinx.android.synthetic.main.fragment_characteristic_mappings.*
import java.util.*
import kotlin.collections.ArrayList

class CharacteristicMappingsFragment : Fragment() {
    private lateinit var map: HashMap<String, Mapping>
    private lateinit var list: ArrayList<Mapping>
    private lateinit var sharedPrefUtils: SharedPrefUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPrefUtils = SharedPrefUtils(requireContext())
        map = sharedPrefUtils.characteristicNamesMap
        list = ArrayList(map.values)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_characteristic_mappings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rv_characteristics.apply {
            layoutManager = LinearLayoutManager(activity)
            addItemDecoration(CardViewListDecoration())
            adapter = DictionaryEntryAdapter(list, requireContext(), Mapping.Type.CHARACTERISTIC)
        }

    }

    override fun onPause() {
        super.onPause()
        map.clear()
        for (mapping in list) map[mapping.uuid] = mapping
        sharedPrefUtils.saveCharacteristicNamesMap(map)
    }
}
