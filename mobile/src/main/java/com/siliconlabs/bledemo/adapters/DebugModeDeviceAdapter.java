package com.siliconlabs.bledemo.adapters;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.ble.BluetoothDeviceInfo;
import com.siliconlabs.bledemo.interfaces.DebugModeCallback;
import com.siliconlabs.bledemo.views.FlowLayout;

import java.lang.reflect.Method;
import java.util.Set;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class DebugModeDeviceAdapter extends ScannedDevicesAdapter<BluetoothDeviceInfo> {
    private Context context;
    DebugModeCallback debugModeCallback;

    public DebugModeDeviceAdapter(Context context, DebugModeCallback debugModeCallback, DeviceInfoViewHolder.Generator generator) {
        super(generator);
        this.context = context;
        this.debugModeCallback = debugModeCallback;
    }

    public static class ViewHolder extends DeviceInfoViewHolder<BluetoothDeviceInfo> {
        @InjectView(R.id.devices_header_label)
        TextView headerLabel;
        @InjectView(R.id.card_view)
        CardView cardView;
        @InjectView(R.id.device_info_container)
        LinearLayout deviceInfoContainer;
        @InjectView(R.id.device_name)
        TextView deviceName;
        @InjectView(R.id.mac_address)
        TextView deviceMacAddress;
        @InjectView(R.id.rssi)
        TextView deviceRssi;
        @InjectView(R.id.expanded_info_container)
        FlowLayout deviceExtraInfoContainer;
        @InjectView(R.id.submenu_spinner)
        Spinner submenuSpinner;
        @InjectView(R.id.connecting_spinner)
        ProgressBar connectingSpinner;
        @InjectView(R.id.button_extrainfo)
        Button button_extrainfo;

        DebugModeCallback debugModeCallback;
        private Context context;
        public SubmenuSpinnerAdapter submenuAdapter;
        BluetoothDeviceInfo device;
        BluetoothDevice mDevice;

        public void startConnectingSpinnerAnim() {
            if (connectingSpinner != null) {
                connectingSpinner.setVisibility(View.VISIBLE);
                connectingSpinner.postInvalidate();
            }
        }

        public void stopConnectingSpinnerAnim() {
            if (connectingSpinner != null) {
                connectingSpinner.setVisibility(View.GONE);
                connectingSpinner.postInvalidate();
            }
        }

        public ViewHolder(final Context context, View view, final DebugModeCallback debugModeCallback) {
            super(view);
            ButterKnife.inject(this, itemView);
            this.context = context;
            this.debugModeCallback = debugModeCallback;
            submenuAdapter = new SubmenuSpinnerAdapter();
            deviceInfoContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewHolder.this.debugModeCallback.connectToDevice(device);
                }
            });
            button_extrainfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(deviceExtraInfoContainer.getVisibility()== View.VISIBLE) deviceExtraInfoContainer.setVisibility(View.GONE);
                    else deviceExtraInfoContainer.setVisibility(View.VISIBLE);
                }
            });
        }

        public void createSpinner(boolean unbond){

            String[] connectedActions = context.getResources().getStringArray(R.array.connected_debug_actions);
            String[] disconnectedActions = context.getResources().getStringArray(R.array.disconnected_debug_actions);
            if (unbond) disconnectedActions[2] = "Bond";
            submenuAdapter.setActionsArrays(connectedActions, disconnectedActions);
            submenuSpinner.setAdapter(submenuAdapter);
            submenuSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (device.isConnected()) {
                        switch (position) {
                            case 1:
                                // View option selected
                                ViewHolder.this.debugModeCallback.callbackSelected(device);
                                break;
                            case 2:
                                // Disconnect option selected
                                ViewHolder.this.debugModeCallback.disconnectFromDevice(device);
                                break;
                            case 3:
                                // Ad Details option selected
                                ViewHolder.this.debugModeCallback.showAdvertisementDialog(device);
                                break;
                        }
                    } else {
                        switch (position) {
                            case 1:
                                // connect clicked
                                ViewHolder.this.debugModeCallback.connectToDevice(device);
                                break;
                            case 2:
                                // Ad Details option selected
                                ViewHolder.this.debugModeCallback.showAdvertisementDialog(device);
                                break;
                            case 3:
                                if(isBonded()) {
                                    ViewHolder.this.unbond();
                                } else {
                                    ViewHolder.this.bond();
                                }
                                break;

                        }
                    }

                    submenuSpinner.setSelection(0);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    submenuSpinner.setSelection(0);
                }
            });
        }

        @Override
        public void setData(BluetoothDeviceInfo info, int position) {
            // reset dirty content

            cardView.setSelected(false);
            cardView.setCardBackgroundColor(Color.WHITE);
            headerLabel.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
            deviceExtraInfoContainer.removeAllViews();

            // set data for the list item
            device = info;
            mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(device.getAddress());
            submenuAdapter.notifyDataSetChanged();
            if (!device.getAddress().equals(mDevice.getAddress())) Log.e("Error","Different Address" + mDevice.getAddress());
            if (device.getName() != null && !device.getName().equals(mDevice.getName())) Log.e("Error","Different Name" + mDevice.getName());
            // build string for information about device, set device info for ui
            String deviceNameText = device.getName() == null || device.getName().isEmpty() ? "Unknown" : device.getName();
            String deviceIsConnectedText = device.isConnected() ? "  <small><font color=\"#0BB5FF\">(connected)</font></small>" : "";
            String deviceHeaderText = "<b>" + deviceNameText + deviceIsConnectedText + "</b>";
            deviceName.setText(Html.fromHtml(deviceHeaderText));
            String deviceMacAddressText = device.getAddress() == null || device.getAddress().isEmpty() ? "Unknown" : device.getAddress();
            deviceMacAddress.setText(deviceMacAddressText);
            int rssi = device.getRssi();
            String deviceRssiText = Integer.toString(rssi);
            deviceRssi.setText(deviceRssiText + " dBm");
            if(isBonded()) {//Bonded -----------------------------------------
                deviceName.append(" (BONDED)");
                createSpinner(false);
            } else {
                createSpinner(true);
            }

            submenuAdapter.notifyDataSetChanged();

            // Extra info about device
            for (int i = 0; i < device.getAdvertData().size(); i++) {
                String data = device.getAdvertData().get(i);
                String[] advertiseData = data.split(":");
                String dataLabel = advertiseData[0];
                String dataValue = "";
                if (advertiseData.length > 1) {
                    dataValue = advertiseData[1];
                }
                TextView extraInfoView = new TextView(context);
                String extraInfo = "<font color=\"#FF4B4B4B\">" + dataValue + "</font>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<br><small><font color=\"#FF969696\">" +
                        dataLabel + "</font><small>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
                extraInfoView.setText(Html.fromHtml(extraInfo));
                extraInfoView.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.getResources().getDimension(R.dimen.device_list_item_ad_data_value_text_size));
                deviceExtraInfoContainer.addView(extraInfoView);

                if (i == BluetoothDeviceInfo.MAX_EXTRA_DATA - 1) {
                    break;
                }
            }
        }

        //Bonded --------------------------------------------------------------------
        private boolean isBonded(){
            boolean isBonded = false;
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (pairedDevices.size()>0){
                for(BluetoothDevice devices: pairedDevices){
                    if(device.getAddress().equals(devices.getAddress())){
                        isBonded = true;
                    }

                }
            }

            return isBonded;
        }
        //--------------------------------------------------------------------------

        public void unbond(){
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (pairedDevices.size()>0){
                for(BluetoothDevice devices: pairedDevices){
                    if(device.getAddress().equals(devices.getAddress())){
                        boolean change_name;
                        try {
                            Method m = mDevice.getClass().getMethod("removeBond", (Class[]) null);
                            m.invoke(mDevice, (Object[]) null);
                            change_name = true;
                        } catch (Exception e){
                            Log.e("Unbond","err: " + e);
                            change_name = false;
                        }
                        if (change_name){
                            deviceName.setText(Html.fromHtml("<b>" +device.getName() + "</b>"));
                        }
                    }
                }
            }
        }

        public void bond(){

            if(device.getAddress()!=null){
                boolean change_name;
                try {
                    Method m = mDevice.getClass().getMethod("createBond", (Class[]) null);
                    m.invoke(mDevice, (Object[]) null);
                    change_name = true;
                } catch (Exception e){
                    Log.e("Bond","err: " + e);
                    change_name = false;
                }
                if(change_name){
                    deviceName.setText(Html.fromHtml("<b>" + device.getName() + "</b>" + " (BONDED)"));
                }
            }
        }


        // note that an empty dummy view is used for index 0 to avoid first item selected state
        private class SubmenuSpinnerAdapter extends BaseAdapter {
            private String actionsConnected[] = {""};
            private String actionsDisconnected[] = {""};

            @Override
            public int getCount() {
                if (device == null) {
                    return 0;
                }
                return device.isConnected() ? (actionsConnected.length + 1) : (actionsDisconnected.length + 1);
            }

            @Override
            public Object getItem(int position) {
                if (device == null) {
                    return "";
                }
                return device.isConnected() ? actionsConnected[position - 1] : actionsDisconnected[position - 1];
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return LayoutInflater.from(context).inflate(R.layout.debug_mode_device_submenu_index_btn, parent, false);
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                if (position > 0) {
                    TextView actionView = (TextView) LayoutInflater.from(context).inflate(R.layout.debug_mode_device_submenu_item, parent, false);
                    actionView.setText(device.isConnected() ? actionsConnected[position - 1] : actionsDisconnected[position - 1]);
                    return actionView;
                } else {
                    View view = new View(context);
                    view.setVisibility(View.GONE);
                    return view;
                }
            }

            public void setActionsArrays(String[] connectedActions, String[] disconnectedActions) {
                this.actionsConnected = connectedActions;
                this.actionsDisconnected = disconnectedActions;
            }
        }
    }
}
