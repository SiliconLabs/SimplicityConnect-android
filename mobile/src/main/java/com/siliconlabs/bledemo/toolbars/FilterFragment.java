package com.siliconlabs.bledemo.toolbars;


import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.siliconlabs.bledemo.other.BeaconType;
import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.other.SavedSearch;
import com.siliconlabs.bledemo.adapters.BeaconTypeAdapter;
import com.siliconlabs.bledemo.adapters.SavedSearchesAdapter;
import com.siliconlabs.bledemo.beaconutils.BleFormat;
import com.siliconlabs.bledemo.utils.FilterDeviceParams;
import com.siliconlabs.bledemo.utils.SharedPrefUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 */
public class FilterFragment extends Fragment {

    private SeekBar rssiSeekBar;
    private TextView rssiValueText;
    private ImageView beaconArrowIV;
    private ImageView savedArrowIV;
    private ImageView closeBtn;
    private Button resetBtn;
    private Button saveBtn;
    private Button searchBtn;
    private AppCompatCheckBox onlyFavourites;
    private AppCompatCheckBox onlyConnectable;
    private RecyclerView beaconTypeRV;
    private RecyclerView savedSearchesRV;
    private BeaconTypeAdapter beaconTypeadapter;
    private SavedSearchesAdapter savedSearchesAdapter;
    private SharedPrefUtils sharedPrefUtils;
    private boolean rssiFlag;

    // UI elements for search boxes
    private EditText deviceNameET;
    private ImageView clearPacketNameSearchIV;
    private EditText packetContentET;
    private ImageView clearPacketContentSearchIV;

    public FilterFragment() {
        // Required empty public constructor
    }

    private ToolbarCallback toolbarCallback;

    public FilterFragment setCallback(ToolbarCallback toolbarCallback) {
        this.toolbarCallback = toolbarCallback;
        return this;
    }

    private void init(View view) {
        closeBtn = view.findViewById(R.id.imageview_close);
        saveBtn = view.findViewById(R.id.button_save);
        searchBtn = view.findViewById(R.id.button_search);
        resetBtn = view.findViewById(R.id.button_reset);
        rssiSeekBar = view.findViewById(R.id.seekbar_rssi);
        rssiValueText = view.findViewById(R.id.textview_rssi_value);
        beaconArrowIV = view.findViewById(R.id.imageview_beacon_arrow);
        savedArrowIV = view.findViewById(R.id.imageview_save_arrow);
        onlyFavourites = view.findViewById(R.id.only_favourites);
        onlyConnectable = view.findViewById(R.id.only_connectable);
        beaconTypeRV = view.findViewById(R.id.recyclerview_beacon_type);
        savedSearchesRV = view.findViewById(R.id.recyclerview_saved_searches);

        deviceNameET = view.findViewById(R.id.search_device_name_edit_text);
        packetContentET = view.findViewById(R.id.search_packet_content_edit_text);
        clearPacketNameSearchIV = view.findViewById(R.id.search_clear_packet_name);
        clearPacketContentSearchIV = view.findViewById(R.id.search_clear_packet_content);

        setSearchBox(deviceNameET, clearPacketNameSearchIV);
        setSearchBox(packetContentET, clearPacketContentSearchIV);
        packetContentET.setFilters(new InputFilter[]{new InputFilter.AllCaps()});

        ArrayList<BeaconType> list = new ArrayList<>();
        final List<BleFormat> bleFormats = new ArrayList<>(Arrays.asList(BleFormat.values()));
        for (BleFormat bf : bleFormats) {
            list.add(new BeaconType(getResources().getString(bf.getNameResId()), false, bf));
        }
        beaconTypeadapter = new BeaconTypeAdapter(list, getActivity());
        LinearLayoutManager beaconLinearLayoutManager = new LinearLayoutManager(getActivity());
        beaconTypeRV.setLayoutManager(beaconLinearLayoutManager);
        beaconTypeRV.addItemDecoration(new DividerItemDecoration(beaconTypeRV.getContext(), beaconLinearLayoutManager.getOrientation()));
        beaconTypeRV.setAdapter(beaconTypeadapter);

        ArrayList<SavedSearch> searchlist = new ArrayList<>();
        for (Map.Entry mapElement : sharedPrefUtils.getMapFilter().entrySet()) {
            searchlist.add(new SavedSearch((String) mapElement.getKey()));
        }
        savedSearchesAdapter = new SavedSearchesAdapter(searchlist, getContext(), new SavedSearchesAdapter.SavedSearchesCallback() {
            @Override
            public void onClick(String name) {
                FilterDeviceParams f = sharedPrefUtils.getMapFilter().get(name);
                sharedPrefUtils.setLastFilter(f);
                fillFields(f);
                savedSearchesAdapter.notifyDataSetChanged();

            }
        });
        LinearLayoutManager searchLinearLayoutManager = new LinearLayoutManager(getActivity());
        savedSearchesRV.setLayoutManager(searchLinearLayoutManager);
        savedSearchesRV.addItemDecoration(new DividerItemDecoration(savedSearchesRV.getContext(), searchLinearLayoutManager.getOrientation()));
        savedSearchesRV.setAdapter(savedSearchesAdapter);
    }


    private void setSearchBox(final EditText searchEditText, final ImageView clearImageView) {

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count <= 0) clearImageView.setVisibility(View.GONE);
                else clearImageView.setVisibility(View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        clearImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchEditText.getText().clear();
            }
        });

    }

    private void ui() {
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toolbarCallback.close();
            }
        });
        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetUi();
                submit(true);
            }
        });
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String key = prepareFilterName();
                if (key.equals("")) return;
                if (!sharedPrefUtils.getMapFilter().containsKey(key)) {
                    FilterDeviceParams filterDeviceParams = prepareFilterDeviceParam();
                    sharedPrefUtils.addToMapFilterAndSave(key, filterDeviceParams);
                    sharedPrefUtils.setLastFilter(filterDeviceParams);
                    savedSearchesAdapter.addItem(new SavedSearch(key));
                    savedSearchesAdapter.notifyDataSetChanged();
                }
            }
        });
        rssiSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                rssiFlag = true;
                int rssi = getRssiValue(progress);
                rssiValueText.setText(getResources().getString(R.string.n_dBm, rssi));

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submit(true);
            }
        });
        beaconArrowIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //If after clicking arrow is up then...
                if (beaconArrowIV.getTag().equals("ARROW_UP")) {
                    beaconArrowIV.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_arrow_down_on));
                    beaconTypeRV.setVisibility(View.GONE);
                    beaconArrowIV.setTag("ARROW_DOWN");
                } else {
                    beaconArrowIV.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_arrow_up_on));
                    beaconTypeRV.setVisibility(View.VISIBLE);
                    beaconArrowIV.setTag("ARROW_UP");
                }
            }
        });
        savedArrowIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //If after clicking arrow is up then...
                if (savedArrowIV.getTag().equals("ARROW_UP")) {
                    savedArrowIV.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_arrow_down_on));
                    savedSearchesRV.setVisibility(View.GONE);
                    savedArrowIV.setTag("ARROW_DOWN");
                } else {
                    savedArrowIV.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_arrow_up_on));
                    savedSearchesRV.setVisibility(View.VISIBLE);
                    savedArrowIV.setTag("ARROW_UP");
                }
            }
        });
    }

    private String prepareFilterName() {
        StringBuilder name = new StringBuilder();
        String seperator = " + ";
        if (rssiFlag) {
            name.append("RSSI > ").append(rssiValueText.getText()).append(seperator);
        }
        List<BleFormat> selectedBeacons = getSelectedBeacons();
        if (!selectedBeacons.isEmpty()) {
            name.append("Beacon type ");
            for (int i = 0; i < selectedBeacons.size(); i++) {
                name.append("\"").append(getResources().getString(selectedBeacons.get(i).getNameResId())).append("\"");
                if (i != selectedBeacons.size() - 1) {
                    name.append(", ");
                }
            }
            name.append(seperator);
        }
        if (!deviceNameET.getText().toString().equals("")) {
            name.append("Name \"").append(deviceNameET.getText().toString()).append("\"").append(seperator);
        }
        if (!packetContentET.getText().toString().equals("")) {
            name.append("Packet content \"").append(packetContentET.getText().toString()).append("\"").append(seperator);
        }
        if (onlyFavourites.isChecked()) {
            name.append("Only favourites").append(seperator);
        }
        if (onlyConnectable.isChecked()) {
            name.append("Only connectable");
        }
        if (name.toString().endsWith(seperator)) {
            name = new StringBuilder(name.substring(0, name.length() - 3));
        }
        return name.toString();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_filter, container, false);
        sharedPrefUtils = new SharedPrefUtils(getContext());
        init(view);
        ui();

        if (sharedPrefUtils.getLastFilter() != null) {
            fillFields(sharedPrefUtils.getLastFilter());
        } else {
            resetUi();
        }

        return view;
    }

    private int getRssiValue(int progress) {
        return progress - 100;
    }

    private void resetUi() {
        deviceNameET.setText("");
        packetContentET.setText("");
        rssiSeekBar.setProgress(0);
        rssiValueText.setText(R.string.dialog_device_filter_rssi_not_set);
        rssiFlag = false;
        onlyFavourites.setChecked(false);
        onlyConnectable.setChecked(false);
        beaconTypeadapter.selectBeacons(Collections.<BleFormat>emptyList());
    }

    private void fillFields(FilterDeviceParams filterDeviceParams) {
        deviceNameET.setText(filterDeviceParams.getName());
        packetContentET.setText(filterDeviceParams.getAdvertising());
        rssiSeekBar.setProgress(filterDeviceParams.getRssiValue() + 100);
        rssiSeekBar.refreshDrawableState();
        int rssi = filterDeviceParams.getRssiValue();
        rssiFlag = filterDeviceParams.isRssiFlag();
        rssiValueText.setText(rssiFlag ? getResources().getString(R.string.n_dBm, rssi) : getContext().getString(R.string.dialog_device_filter_rssi_not_set));
        List<BleFormat> selectedBleFormats = filterDeviceParams.getBleFormats();
        beaconTypeadapter.selectBeacons(selectedBleFormats);
        onlyFavourites.setChecked(filterDeviceParams.isOnlyFavourite());
        onlyConnectable.setChecked(filterDeviceParams.isOnlyConnectable());

    }

    private List<BleFormat> getSelectedBeacons() {
        List<BleFormat> selectedBeacons = new ArrayList<>();
        for (BeaconType b : beaconTypeadapter.getBeaconTypeList()) {
            if (b.isChecked()) selectedBeacons.add(b.getBleFormat());
        }
        return selectedBeacons;
    }

    private FilterDeviceParams prepareFilterDeviceParam() {
        String name = deviceNameET.getText().toString();
        String advertising = packetContentET.getText().toString();
        int rssi = getRssiValue(rssiSeekBar.getProgress());
        return new FilterDeviceParams(prepareFilterName(), name, advertising, rssi, rssiFlag, getSelectedBeacons(), onlyFavourites.isChecked(), onlyConnectable.isChecked());
    }

    private void submit(boolean closeToolbar) {
        FilterDeviceParams filterDeviceParams = prepareFilterDeviceParam();
        sharedPrefUtils.setLastFilter(filterDeviceParams);
        toolbarCallback.submit(filterDeviceParams, closeToolbar);
    }

}
