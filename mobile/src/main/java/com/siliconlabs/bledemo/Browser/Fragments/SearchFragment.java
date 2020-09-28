package com.siliconlabs.bledemo.Browser.Fragments;


import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.Browser.Activities.BrowserActivity;

/**
 * Created by jcstange on 12/5/2016.
 */

public class SearchFragment extends Fragment implements View.OnClickListener {
    private EditText tosearch;
    private Switch Bonded;
    private Switch RSSI;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        RSSI = view.findViewById(R.id.rssi_radio);
        RSSI.setOnClickListener(this);
        RSSI.setSelected(false);
        Bonded = view.findViewById(R.id.bondedradio);
        Bonded.setOnClickListener(this);
        Bonded.setSelected(false);
        Button search = view.findViewById(R.id.search_filter);
        search.setOnClickListener(this);
        tosearch = view.findViewById(R.id.search);
        tosearch.setImeActionLabel("", EditorInfo.IME_ACTION_SEARCH);
        tosearch.setOnClickListener(this);
        tosearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String search = v.getText().toString();
                    ((BrowserActivity) getActivity()).performSearch(search);
                    //Log.i("SearchFragment","Search_filter pressed");
                    return true;
                }
                return false;
            }
        });
        return view;
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.rssi_radio:
                //...
                if (!Bonded.isChecked()) {
                    ((BrowserActivity) getActivity()).sortlist(4);
                    //Log.d("SearchFragment","rssi_radio depressed");
                } else {
                    ((BrowserActivity) getActivity()).sortlist(0);
                    //Log.d("SearchFragment","rssi_radio pressed");
                }
                //Log.d("SearchFragment","rssi_radio pressed");
                break;
            case R.id.bondedradio:
                //...
                if (!Bonded.isChecked()) {
                    ((BrowserActivity) getActivity()).onRefresh();
                } else {
                    ((BrowserActivity) getActivity()).filterbond();
                }
                //Log.d("SearchFragment","Bonded_filter pressed");
                break;
            case R.id.search_filter:
                //...
                String search = tosearch.getText().toString();
                ((BrowserActivity) getActivity()).performSearch(search);
                //Log.i("SearchFragment","Search_filter pressed");
                break;
            case R.id.search:
                if (!tosearch.getText().toString().equals("")) tosearch.setText("");

                break;
            default:
                break;
        }
    }


}

