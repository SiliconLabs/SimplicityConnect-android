package com.siliconlabs.bledemo.adapters;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.ble.BluetoothDeviceInfo;
import com.siliconlabs.bledemo.interfaces.ServicesConnectionsCallback;
import com.siliconlabs.bledemo.utils.Constants;

import java.util.List;

public class ConnectionsAdapter extends RecyclerView.Adapter<ConnectionsAdapter.ConnectionViewHolder> {
    private List<BluetoothDevice> connectionsList;
    private Context context;
    private ServicesConnectionsCallback servicesConnectionsCallback;
    private String selectedDevice;

    public class ConnectionViewHolder extends RecyclerView.ViewHolder {
        TextView nameTV;
        Button disconnectB;
        LinearLayout layout;
        ProgressBar progressBar;

        public ConnectionViewHolder(View itemView) {
            super(itemView);
            nameTV = itemView.findViewById(R.id.textview_device_name);
            disconnectB = itemView.findViewById(R.id.button_disconnect);
            layout = itemView.findViewById(R.id.connected_device_item);
            progressBar = itemView.findViewById(R.id.progress_bar_connections);
        }

    }

    public ConnectionsAdapter(List<BluetoothDevice> bluetoothDevices, Context context) {
        this.context = context;
        this.connectionsList = bluetoothDevices;
    }

    @Override
    public void onBindViewHolder(final ConnectionsAdapter.ConnectionViewHolder holder, final int position) {
        holder.progressBar.setVisibility(View.GONE);
        holder.disconnectB.setVisibility(View.VISIBLE);
        String name = connectionsList.get(position).getName();
        String address = connectionsList.get(position).getAddress();
        if (name == null || name.equals("")) {
            name = Constants.UNKNOWN;
        }
        if (getSelectedDevice() != null) {
            if (address.toLowerCase().equals(getSelectedDevice().toLowerCase())) {
                holder.nameTV.setTextColor(ContextCompat.getColor(context,R.color.silabs_blue));
            } else {
                holder.nameTV.setTextColor(ContextCompat.getColor(context,R.color.silabs_primary_text));
            }
        } else {
            holder.nameTV.setTextColor(ContextCompat.getColor(context,R.color.silabs_primary_text));
        }
        holder.nameTV.setText(name);

        holder.disconnectB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BluetoothDeviceInfo bluetoothDeviceInfo = new BluetoothDeviceInfo();
                bluetoothDeviceInfo.device = connectionsList.get(position);
                servicesConnectionsCallback.onDisconnectClicked(bluetoothDeviceInfo);
            }
        });

        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.progressBar.setVisibility(View.VISIBLE);
                holder.disconnectB.setVisibility(View.GONE);
                BluetoothDeviceInfo bluetoothDeviceInfo = new BluetoothDeviceInfo();
                bluetoothDeviceInfo.device = connectionsList.get(position);
                servicesConnectionsCallback.onDeviceClicked(bluetoothDeviceInfo);
            }
        });
    }

    @Override
    public ConnectionsAdapter.ConnectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.connection_item, parent, false);
        return new ConnectionViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return connectionsList.size();
    }

    public void setConnectionsList(List<BluetoothDevice> connectionsList) {
        this.connectionsList = connectionsList;
    }

    public List<BluetoothDevice> getConnectionsList() {
        return connectionsList;
    }

    public void setServicesConnectionsCallback(ServicesConnectionsCallback servicesConnectionsCallback) {
        this.servicesConnectionsCallback = servicesConnectionsCallback;
    }

    public String getSelectedDevice() {
        return selectedDevice;
    }

    public void setSelectedDevice(String selectedDevice) {
        this.selectedDevice = selectedDevice;
    }
}
