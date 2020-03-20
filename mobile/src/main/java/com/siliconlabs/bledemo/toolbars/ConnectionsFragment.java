package com.siliconlabs.bledemo.toolbars;


import android.bluetooth.BluetoothDevice;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.activity.DeviceServicesActivity;
import com.siliconlabs.bledemo.adapters.ConnectionsAdapter;
import com.siliconlabs.bledemo.ble.BlueToothService;


/**
 * A simple {@link Fragment} subclass.
 */
public class ConnectionsFragment extends Fragment {

    public ConnectionsFragment() {
        // Required empty public constructor
    }

    private ToolbarCallback toolbarCallback;
    private ConnectionsAdapter adapter;
    private BlueToothService.Binding bluetoothBinding;


    public ConnectionsFragment setCallback(ToolbarCallback toolbarCallback) {
        this.toolbarCallback = toolbarCallback;
        return this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_connections, container, false);
        // Inflate the layout for this fragment
        ImageView closeBtn = view.findViewById(R.id.imageview_close);
        Button disconnectAll = view.findViewById(R.id.button_disconnect_all);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toolbarCallback.close();
            }
        });


        RecyclerView connectionsRV = view.findViewById(R.id.recyclerview_connections);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        connectionsRV.setLayoutManager(new LinearLayoutManager(getActivity()));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(connectionsRV.getContext(), linearLayoutManager.getOrientation());
        connectionsRV.addItemDecoration(dividerItemDecoration);
        connectionsRV.setAdapter(adapter);

        disconnectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                bluetoothBinding = new BlueToothService.Binding(getContext()) {
                    @Override
                    protected void onBound(BlueToothService service) {
                        for (BluetoothDevice device : adapter.getConnectionsList()) {
                            boolean successDisconnected = service.disconnectGatt(device.getAddress());
                            if (!successDisconnected) {
                                Toast.makeText(getContext(), R.string.device_not_from_EFR, Toast.LENGTH_LONG).show();
                            }
                        }
                        if (getActivity() != null && getActivity() instanceof DeviceServicesActivity) {
                            getActivity().finish();
                        } else {
                            toolbarCallback.close();
                        }
                    }
                };

                BlueToothService.bind(bluetoothBinding);
            }
        });
        return view;
    }

    public ConnectionsAdapter getAdapter() {
        return adapter;
    }

    public void setAdapter(ConnectionsAdapter adapter) {
        this.adapter = adapter;
    }
}
