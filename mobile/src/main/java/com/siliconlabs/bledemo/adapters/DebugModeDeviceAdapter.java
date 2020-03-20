package com.siliconlabs.bledemo.adapters;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import androidx.cardview.widget.CardView;

import android.os.ParcelUuid;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.activity.BrowserActivity;
import com.siliconlabs.bledemo.beaconutils.BleFormat;
import com.siliconlabs.bledemo.beaconutils.altbeacon.AltBeacon;
import com.siliconlabs.bledemo.beaconutils.eddystone.Beacon;
import com.siliconlabs.bledemo.beaconutils.eddystone.Constants;
import com.siliconlabs.bledemo.beaconutils.eddystone.TlmValidator;
import com.siliconlabs.bledemo.beaconutils.eddystone.UidValidator;
import com.siliconlabs.bledemo.beaconutils.eddystone.UrlValidator;
import com.siliconlabs.bledemo.beaconutils.ibeacon.IBeaconInfo;
import com.siliconlabs.bledemo.ble.BlueToothService;
import com.siliconlabs.bledemo.ble.BluetoothDeviceInfo;
import com.siliconlabs.bledemo.ble.ScanResultCompat;
import com.siliconlabs.bledemo.interfaces.DebugModeCallback;
import com.siliconlabs.bledemo.utils.SharedPrefUtils;
import com.siliconlabs.bledemo.views.ServiceItemContainerRe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import butterknife.InjectView;

import static com.siliconlabs.bledemo.bluetoothdatamodel.parsing.ScanRecordParser.SPLIT;


public class DebugModeDeviceAdapter extends ScannedDevicesAdapter<BluetoothDeviceInfo> {
    private DebugModeCallback debugModeCallback;
    private static SharedPrefUtils sharedPrefUtils;
    private static Context mContext;
    private static Map<String, AdvertismentData> currentAdvertismentDataMap = new HashMap<>();

    public static final ParcelUuid EDDYSTONE_SERVICE_UUID = ParcelUuid.fromString(
            "0000FEAA-0000-1000-8000-00805F9B34FB");


    public DebugModeDeviceAdapter(Context context, DebugModeCallback debugModeCallback, DeviceInfoViewHolder.Generator generator) {
        super(generator, context);
        mContext = context;
        this.debugModeCallback = debugModeCallback;
        sharedPrefUtils = new SharedPrefUtils(context);
    }

    public static class ViewHolder extends DeviceInfoViewHolder<BluetoothDeviceInfo> implements View.OnClickListener {
        @InjectView(R.id.card_view)
        CardView cardView;
        @InjectView(R.id.device_name)
        TextView deviceName;
        @InjectView(R.id.device_address)
        TextView deviceAddress;
        @InjectView(R.id.rssi)
        TextView deviceRssi;
        @InjectView(R.id.device_type)
        TextView deviceType;
        @InjectView(R.id.connect_btn)
        Button connectBtn;
        @InjectView(R.id.disconnect_btn)
        Button disconnectBtn;
        @InjectView(R.id.favorite_btn)
        CheckBox favoriteBtn;
        @InjectView(R.id.advertisement_container)
        LinearLayout advertisementContainer;
        @InjectView(R.id.text_view_is_connectable)
        TextView isConnectableTV;
        @InjectView(R.id.text_view_interval)
        TextView intervalTV;
        @InjectView(R.id.text_view_device_address)
        TextView addressTV;

        DebugModeCallback debugModeCallback;
        private Context context;
        private BluetoothDeviceInfo device;
        private BleFormat bleFormat;
        private BlueToothService.Binding bluetoothBinding;

        public ViewHolder(final Context context, View view, final DebugModeCallback debugModeCallback) {
            super(view);
            ButterKnife.inject(this, itemView);
            this.context = context;
            this.debugModeCallback = debugModeCallback;
        }

        private void refreshDeviceRowOnUiThread(final boolean isConnected) {
            if (mContext instanceof BrowserActivity) {
                ((BrowserActivity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refreshButtonsConnected(isConnected);
                        debugModeCallback.updateCountOfConnectedDevices();
                    }
                });
            }
        }

        private void refreshButtonsConnected(boolean isConnected) {
            if (isConnected) {
                connectBtn.setVisibility(View.GONE);
                disconnectBtn.setVisibility(View.VISIBLE);
            } else {
                connectBtn.setVisibility(View.VISIBLE);
                disconnectBtn.setVisibility(View.GONE);
            }
        }

        private List<BluetoothDevice> getConnectedDevices() {
            BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            return bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
        }

        private boolean isConnected(String address) {
            for (BluetoothDevice d : getConnectedDevices()) {
                if (d.getAddress().equals(address)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void setData(BluetoothDeviceInfo bluetoothDeviceInfo, int position) {
            // set data for the list item
            device = bluetoothDeviceInfo;
            advertisementContainer.removeAllViews();
            refreshButtonsConnected(isConnected(device.getAddress()));

            if (currentAdvertismentDataMap.containsKey(device.getAddress())) {
                addAdvertsToContainer(currentAdvertismentDataMap.get(device.getAddress()));
            }

            if (device.isConnectable) {
                connectBtn.setVisibility(View.VISIBLE);
                isConnectableTV.setText(context.getResources().getString(R.string.Connectable));
            } else {
                connectBtn.setVisibility(View.GONE);
                isConnectableTV.setText(context.getResources().getString(R.string.Non_Connectable));
            }
            intervalTV.setText(String.valueOf(device.intervalNanos / 1000000));
            addressTV.setText(device.getAddress());
            String deviceNameText = device.getName() == null || device.getName().isEmpty() ? "Unknown" : device.getName();
            deviceName.setText(deviceNameText);
            deviceRssi.setText(context.getResources().getString(R.string.n_dBm, device.getRssi()));
            deviceAddress.setText(device.getAddress());
            bleFormat = device.getBleFormat();
            deviceType.setText(context.getResources().getString(bleFormat.getNameResId()));
            final boolean isFavorite = sharedPrefUtils.isFavorite(device.getAddress());
            favoriteBtn.setChecked(isFavorite);
            favoriteBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isFavorite) {
                        ViewHolder.this.debugModeCallback.removeFromFavorite(device.getAddress());
                    } else {
                        ViewHolder.this.debugModeCallback.addToFavorite(device.getAddress());
                    }
                }
            });
            cardView.setOnClickListener(this);
            disconnectBtn.setOnClickListener(this);
            connectBtn.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {

                case R.id.card_view:
                    generateAdvertData();
                    break;

                case R.id.disconnect_btn:
                    disconnect();
                    break;

                case R.id.connect_btn:
                    connect();
                    break;

                default:
                    break;
            }
        }

        private void connect() {
            debugModeCallback.connectToDevice(device);
        }

        private void disconnect() {
            bluetoothBinding = new BlueToothService.Binding(context) {
                @Override
                protected void onBound(BlueToothService service) {
                    boolean successDisconnected = service.disconnectGatt(device.getAddress());
                    if (!successDisconnected) {
                        Toast.makeText(mContext, R.string.device_not_from_EFR, Toast.LENGTH_LONG).show();
                    }
                    refreshDeviceRowOnUiThread(!successDisconnected);

                }
            };
            BlueToothService.bind(bluetoothBinding);
        }

        private void generateAdvertData() {
            if (currentAdvertismentDataMap.containsKey(device.getAddress())) {
                currentAdvertismentDataMap.remove(device.getAddress());
                advertisementContainer.removeAllViews();
            } else {
                AdvertismentData advertismentData = new AdvertismentData();
                List<AdvertismentRow> rows = new ArrayList<>();

                for (int i = 0; i < device.getAdvertData().size(); i++) {
                    String data = device.getAdvertData().get(i);
                    String[] advertiseData = data.split(SPLIT);
                    String dataLabel = advertiseData[0];
                    String dataValue = "";
                    if (advertiseData.length > 1) {
                        dataValue = advertiseData[1];
                    }

                    rows.add(new AdvertismentRow(dataLabel, dataValue));

                }
                String dataValue = "";
                switch (bleFormat) {
                    case I_BEACON:
                        iBeaconAdv(rows, dataValue);
                        break;
                    case EDDYSTONE:
                        eddystoneAdv(rows, dataValue);
                        break;
                    case ALT_BEACON:
                        altBeaconAdv(rows, dataValue);
                        break;
                    default:
                        break;

                }

                advertismentData.setRows(rows);
                currentAdvertismentDataMap.put(device.getAddress(), advertismentData);

                addAdvertsToContainer(advertismentData);
            }
        }

        //copy from BeaconScreenScannedDevicesAdapter
        private void validateEddyStoneServiceData(Beacon beacon, String deviceAddress, byte[] serviceData) {
            if (serviceData == null) {
                String err = "Null Eddystone service data";
                beacon.frameStatus.nullServiceData = err;
                return;
            }
            switch (serviceData[0]) {
                case Constants.UID_FRAME_TYPE:
                    UidValidator.validate(deviceAddress, serviceData, beacon);
                    break;
                case Constants.TLM_FRAME_TYPE:
                    TlmValidator.validate(deviceAddress, serviceData, beacon);
                    break;
                case Constants.URL_FRAME_TYPE:
                    UrlValidator.validate(deviceAddress, serviceData, beacon);
                    break;
                default:
                    String err = String.format("Invalid frame type byte %02X", serviceData[0]);
                    beacon.frameStatus.invalidFrameType = err;
                    break;
            }

        }

        //copy from BeaconScreenScannedDevicesAdapter
        public void addAdvertsToContainer(AdvertismentData advertismentData) {
            advertisementContainer.removeAllViews();
            for (AdvertismentRow row : advertismentData.getRows()) {
                final ServiceItemContainerRe serviceItemContainer = new ServiceItemContainerRe(context);
                serviceItemContainer.serviceTitleTextView.setText(row.getTitle());
                serviceItemContainer.serviceUuidTextView.setText(row.getBody());
                advertisementContainer.addView(serviceItemContainer, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            }
        }

        //copy from BeaconScreenScannedDevicesAdapter
        private void iBeaconAdv(List<AdvertismentRow> rows, String dataValue) {
            IBeaconInfo iBeaconInfo = BleFormat.getIBeaconInfo(device.device,
                    device.getRssi(),
                    device.scanInfo.getScanRecord().getBytes());

            if (iBeaconInfo != null) {
                String uuid = iBeaconInfo.getUuid();
                int major = iBeaconInfo.getMajor();
                int minor = iBeaconInfo.getMinor();
                int rssiAt1m = iBeaconInfo.getPower();

                String details = "Minor: " + minor + "<br><br>" +
                        "Major: " + major + "<br><br>" +
                        "UUID: " + uuid + "<br><br>" +
                        "RSSI at 1m: " + rssiAt1m;

                rows.add(new AdvertismentRow("iBeacon data", Html.fromHtml(details).toString()));

            }
        }

        //copy from BeaconScreenScannedDevicesAdapter
        private void eddystoneAdv(List<AdvertismentRow> rows, String dataValue) {

            ScanResultCompat scanInfo = device.scanInfo;
            String deviceAddress = scanInfo.getDevice().getAddress();
            Beacon beacon = new Beacon(deviceAddress, scanInfo.getRssi());
            byte[] serviceData = scanInfo.getScanRecord().getServiceData().get(EDDYSTONE_SERVICE_UUID);
            validateEddyStoneServiceData(beacon, deviceAddress, serviceData);

            // get url string
            String eddystoneUrl = beacon.urlStatus.toString();
            eddystoneUrl = TextUtils.isEmpty(eddystoneUrl) ? context.getString(R.string.beacon_details_dialog_unknown_value) : eddystoneUrl;

            // get uid string
            // Eddystone UID Advertisement Data: 16-byte Beacon ID (10-byte namespace, 6-byte instance)
            String eddystoneUid = beacon.uidStatus.uidValue;
            String eddystoneUidValue = TextUtils.isEmpty(eddystoneUid) ? context.getString(R.string.beacon_details_dialog_unknown_value) : eddystoneUid
                    .substring(0, 20);
            String eddystoneUidNameSpace = TextUtils.isEmpty(eddystoneUid) ? context.getString(R.string.beacon_details_dialog_unknown_value) :
                    "" + eddystoneUid.substring(20);

            // get tlm data
            String version = beacon.tlmStatus.version;
            //beacon.tlmstatus.voltage returns value in millivolts
            String voltage = beacon.tlmStatus.voltage;
            double voltageValue = 0;
            if (voltage != null) {
                voltageValue = Double.parseDouble(voltage);
                voltage = (voltageValue / 1000) + " " + context.getString(R.string.beacon_details_dialog_unit_volts);
            }

            String temperature = beacon.tlmStatus.temp + " " +
                    context.getString(R.string.beacon_details_dialog_unit_degrees_celsius);
            String advertisementCount = beacon.tlmStatus.advCnt;

            double uptimeCountInSeconds = beacon.tlmStatus.deciSecondsCntVal / 10;
            uptimeCountInSeconds = Math.round(uptimeCountInSeconds * 10) / 10.0;
            String secondsLabel = context.getString(R.string.beacon_details_dialog_unit_seconds_abbreviated);
            String daysLabel = context.getString(R.string.beacon_details_dialog_unit_days);
            String uptimeCount = String.format("%d " + secondsLabel + " (%d " + daysLabel + ")",
                    (int) uptimeCountInSeconds,
                    TimeUnit.SECONDS.toDays((int) (uptimeCountInSeconds)));

            String eddystoneTlm = "";
            eddystoneTlm += (context.getString(R.string.beacon_details_dialog_tlm_version) + ": " + version + "<br>");
            eddystoneTlm += (context.getString(R.string.beacon_details_dialog_tlm_voltage) + ": " + voltage + "<br>");
            eddystoneTlm += (context.getString(R.string.beacon_details_dialog_tlm_temperature) + ": " + temperature +
                    "<br>");
            eddystoneTlm += (context.getString(R.string.beacon_details_dialog_tlm_advertisement_count) + ": " +
                    advertisementCount + "<br>");
            eddystoneTlm += (context.getString(R.string.beacon_details_dialog_tlm_uptime) + ": " + uptimeCount);
            eddystoneTlm = TextUtils.isEmpty(eddystoneTlm) ? context.getString(R.string.beacon_details_dialog_unknown_value) : eddystoneTlm;
            dataValue +=
                    "<b>" + context.getString(R.string.beacon_details_dialog_url) + ":</b> " + eddystoneUrl + "<br>";
            dataValue +=
                    "<b>" + context.getString(R.string.beacon_details_dialog_uid) + ":</b> " + eddystoneUidValue +
                            "<br>";
            dataValue += "<b>" + context.getString(R.string.beacon_details_dialog_instance) + ":</b> " +
                    eddystoneUidNameSpace + "<br><br>";

            dataValue +=
                    "<b>" + context.getString(R.string.beacon_details_dialog_tlm_data) + ":</b><br>" + eddystoneTlm +
                            "<br>";

            rows.add(new AdvertismentRow("Eddystone data", Html.fromHtml(dataValue).toString()));
        }

        //copy from BeaconScreenScannedDevicesAdapter
        private void altBeaconAdv(List<AdvertismentRow> rows, String dataValue) {
            AltBeacon altBeacon = new AltBeacon(device);
            String beaconId = altBeacon.getAltBeaconId();
            String mfgId = altBeacon.getManufacturerId();
            String refRssi = "" + altBeacon.getAltBeaconReferenceRssi();
            dataValue +=
                    "<b>" + context.getString(R.string.beacon_details_dialog_beacon_id) + ":</b><br> " + beaconId +
                            "<br><br>";
            dataValue +=
                    "<b>" + context.getString(R.string.beacon_details_dialog_manufacturer_id) + ":</b> " + mfgId +
                            "<br><br>";
            dataValue +=
                    "<b>" + context.getString(R.string.beacon_details_dialog_reference_rssi) + ":</b> " + refRssi +
                            "&nbsp;dBm<br>";
            rows.add(new AdvertismentRow("AltBeacon data", Html.fromHtml(dataValue).toString()));
        }

    }

    public static class AdvertismentData {
        private List<AdvertismentRow> rows = new ArrayList<>();

        public List<AdvertismentRow> getRows() {
            return rows;
        }

        public void setRows(List<AdvertismentRow> rows) {
            this.rows = rows;
        }
    }

    public static class AdvertismentRow {
        private String title;
        private String body;

        public AdvertismentRow(String title, String body) {
            this.title = title;
            this.body = body;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }
    }
}
