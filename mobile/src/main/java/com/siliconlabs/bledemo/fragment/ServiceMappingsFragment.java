package com.siliconlabs.bledemo.fragment;


import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.siliconlabs.bledemo.mappings.Mapping;
import com.siliconlabs.bledemo.mappings.MappingType;
import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.adapters.MappingAdapter;
import com.siliconlabs.bledemo.utils.SharedPrefUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class ServiceMappingsFragment extends Fragment {

    private HashMap<String, Mapping> map;
    private List<Mapping> list;
    private SharedPrefUtils sharedPrefUtils;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPrefUtils = new SharedPrefUtils(getActivity());
        map = sharedPrefUtils.getServiceNamesMap();
        list = new ArrayList<>(map.values());

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_service_mappings, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
        MappingAdapter adapter = new MappingAdapter(list, getActivity(), MappingType.SERVICE);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();

        map.clear();

        for (Mapping mapping : list) {
            map.put(mapping.getUuid(), mapping);
        }

        sharedPrefUtils.saveServiceNamesMap(map);
    }
}
