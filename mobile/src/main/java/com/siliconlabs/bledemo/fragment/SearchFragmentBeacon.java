package com.siliconlabs.bledemo.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Switch;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.activity.BeaconListFragment;
import com.siliconlabs.bledemo.activity.BeaconScanActivity;
import com.siliconlabs.bledemo.activity.MainActivityDebugMode;
/**
 * Created by jcstange on 1/5/2017.
 */

public class SearchFragmentBeacon extends Fragment implements View.OnClickListener {
        EditText tosearch;
        Switch Bonded;
        Switch RSSI;
        @Override
public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.search_fragment, container, false);
        RSSI = (Switch) view.findViewById(R.id.rssi_radio);
        RSSI.setOnClickListener(this);
        Bonded = (Switch) view.findViewById(R.id.bondedradio);
        Bonded.setOnClickListener(this);
        Button search = (Button) view.findViewById(R.id.search_filter);
        search.setOnClickListener(this);
        tosearch = (EditText) view.findViewById(R.id.search);
        tosearch.setOnClickListener(this);
        // Inflate the layout for this fragment
        return view;
        }

@Override
public void onClick(View v) {

        switch (v.getId()){
        case R.id.rssi_radio:
        //...
        ((BeaconScanActivity) getActivity()).orderbyRSSI();
        //Log.i("SearchFragment","rssi_radio pressed");
        break;
        case R.id.bondedradio:
        //...
        //Log.i("SearchFragment","Bonded_filter pressed");
        break;
        case R.id.search_filter:
        //...
        String search = tosearch.getText().toString();
        //Log.i("SearchFragment","Search_filter pressed");
        break;
        case R.id.search:
        if(tosearch.getText().toString().equals("Filter by Name")) {
        tosearch.setText("");
        }
        break;
default:
        break;
        }}}

