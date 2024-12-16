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
import com.siliconlabs.bledemo.databinding.FragmentCharacteristicMappingsBinding
import com.siliconlabs.bledemo.utils.SharedPrefUtils

import java.util.*
import kotlin.collections.ArrayList

class CharacteristicMappingsFragment : Fragment() {
    private lateinit var map: HashMap<String, Mapping>
    private lateinit var list: ArrayList<Mapping>
    private lateinit var sharedPrefUtils: SharedPrefUtils
    private lateinit var binding: FragmentCharacteristicMappingsBinding  //fragment_characteristic_mappings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPrefUtils = SharedPrefUtils(requireContext())
        map = sharedPrefUtils.characteristicNamesMap
        list = ArrayList(map.values)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCharacteristicMappingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvCharacteristics.apply {
            layoutManager = LinearLayoutManager(activity)
            addItemDecoration(CardViewListDecoration())
            adapter =
                DictionaryEntryAdapter(list, requireContext(), Mapping.Type.CHARACTERISTIC)
        }

    }

    override fun onPause() {
        super.onPause()
        map.clear()
        for (mapping in list) map[mapping.uuid] = mapping
        sharedPrefUtils.saveCharacteristicNamesMap(map)
    }
}
