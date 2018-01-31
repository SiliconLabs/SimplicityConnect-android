package com.siliconlabs.bledemo.adapters;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.activity.BeaconListFragment;
import com.siliconlabs.bledemo.beaconutils.BleFormat;
import com.siliconlabs.bledemo.beaconutils.altbeacon.AltBeacon;
import com.siliconlabs.bledemo.beaconutils.eddystone.Beacon;
import com.siliconlabs.bledemo.beaconutils.eddystone.Constants;
import com.siliconlabs.bledemo.beaconutils.eddystone.TlmValidator;
import com.siliconlabs.bledemo.beaconutils.eddystone.UidValidator;
import com.siliconlabs.bledemo.beaconutils.eddystone.UrlValidator;
import com.siliconlabs.bledemo.beaconutils.ibeacon.IBeaconInfo;
import com.siliconlabs.bledemo.ble.BluetoothDeviceInfo;
import com.siliconlabs.bledemo.ble.Discovery;
import com.siliconlabs.bledemo.ble.ScanResultCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BeaconScreenScannedDevicesAdapter<T extends BluetoothDeviceInfo> extends RecyclerView.Adapter<DeviceInfoViewHolder>
        implements Discovery.DeviceContainer {

    // Beacon Type, Blue Gecko filter values
    private static final String BLUE_GECKO_EDDYSTONE_URL = "http://www.silabs.com/bluegecko";
    private static final int BLUE_GECKO_IBEACON_MAJOR = 34987;
    private static final int BLUE_GECKO_IBEACON_MINOR = 1025;
    private static final String BLUE_GECKO_IBEACON_UUID = "E2C56DB5-DFFB-48D2-B060-D0F5A71096E0";
    private static final String BLUE_GECKO_ALT_BEACON_MFG_ID = "0x0047";
    private static final String BLUE_GECKO_ALT_BEACON_UUID = "0x511AB500511AB500511AB500511AB500";
    private static final String BLUE_GECKO_OLD_UUID = "0xFEEDABBADEADBEEFFEEDABBADEADBEEF";
    // The Eddystone Service UUID, 0xFEAA.
    private static final ParcelUuid EDDYSTONE_SERVICE_UUID = ParcelUuid.fromString(
            "0000FEAA-0000-1000-8000-00805F9B34FB");
    private final static long MAX_AGE = 10000;
    private static final Comparator<BluetoothDeviceInfo> DEVICE_INFO_RSSI_COMPARATOR = new Comparator<BluetoothDeviceInfo>() {
        @Override
        public int compare(BluetoothDeviceInfo lhs, BluetoothDeviceInfo rhs) {
            int p1 = lhs.scanInfo.getRssi();
            int p2 = rhs.scanInfo.getRssi();
            //note: these are all negative, so it looks backwards
            if (p1 < p2) {
                return 1;
            } else if (p2 < p1) {
                return -1;
            }
            return 0;
        }
    };

    private boolean runUpdater = true;
    boolean mostRecentDevicesInfoIsDirty;
    boolean updatePending;
    private DeviceInfoViewHolder.Generator generator;
    private Dialog dialog;
    private BluetoothDeviceInfo deviceInfoForDetailsDialog;

    final Handler handler = new Handler();
    public final List<T> mostRecentDevicesInfo = new ArrayList<>();
    public final Map<T, Long> mostRecentInfoAge = new HashMap<>();
    public final List<T> devicesInfo = new ArrayList<>();
    // device mac address to eddystone beacon mapping
    public Map<String, Beacon> deviceToEddystoneBeaconMap = new HashMap<>();

    final Runnable delayedUpdater = new Runnable() {
        @Override
        public void run() {
            updateDevicesInfo();
        }
    };

    public BeaconScreenScannedDevicesAdapter(DeviceInfoViewHolder.Generator generator) {
        this.generator = generator;
    }

    @Override
    public int getItemCount() {
        return devicesInfo.size();
    }

    @Override
    public long getItemId(int position) {
        final BluetoothDeviceInfo info = devicesInfo.get(position);
        return info.device.getAddress().hashCode();
    }

    @Override
    public DeviceInfoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return generator.generate(parent);
    }

    @Override
    public void onBindViewHolder(DeviceInfoViewHolder holder, int position) {
        final T info = devicesInfo.get(position);
        holder.setData(info, position);

        int listHeaderLabelVisibility = position == 0 ? View.VISIBLE : View.GONE;
        ((BeaconListFragment.ViewHolder) holder).beaconListHeaderLabel.setVisibility(listHeaderLabelVisibility);

        int dividerVisibility = getItemCount() - 1 > position ? View.VISIBLE : View.GONE;
        ((BeaconListFragment.ViewHolder) holder).beaconListItemSeparator.setVisibility(dividerVisibility);

        // parse device to find beacon type / determine information to display
        final BluetoothDeviceInfo deviceInfo = devicesInfo.get(position);
        final BeaconListFragment.ViewHolder viewHolder = (BeaconListFragment.ViewHolder) holder;

        if (BleFormat.isAltBeacon(deviceInfo)) {
            updateAltBeaconUI(viewHolder, deviceInfo);
        } else if (BleFormat.isEddyStone(deviceInfo)) {
            updateEddystoneBeaconUI(viewHolder, deviceInfo);
        } else if (BleFormat.isIBeacon(deviceInfo)) {
            updateIBeaconUI(viewHolder, deviceInfo);
        } else if (BleFormat.isBlueBeaconOld(deviceInfo) || BleFormat.isBlueBeaconNew(deviceInfo)) {
            updateBlueGeckoUI(viewHolder, deviceInfo);
        }

        viewHolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBeaconDetailsDialog(deviceInfo, viewHolder.title.getContext());
            }
        });

        // if dialog is already showing, update dialog info (means a device has already been selected)
        if (this.deviceInfoForDetailsDialog != null &&
                this.deviceInfoForDetailsDialog.getAddress().compareTo(deviceInfo.getAddress()) == 0
                && dialog != null && dialog.isShowing()) {
            showBeaconDetailsDialog(deviceInfo, viewHolder.title.getContext());
        }
    }

    public void showBeaconDetailsDialog(BluetoothDeviceInfo detailsDialogDeviceInfo, Context context) {
        if (dialog == null) {
            dialog = new Dialog(context);
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_beacon_info_detail);
            dialog.findViewById(R.id.close_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        }

        this.deviceInfoForDetailsDialog = detailsDialogDeviceInfo;

        TextView deviceName = (TextView) dialog.findViewById(R.id.device_name);
        TextView deviceMacAddr = (TextView) dialog.findViewById(R.id.device_mac_address);
        String deviceNameString = TextUtils.isEmpty(detailsDialogDeviceInfo.getName()) ? context.getString(R.string.beacon_details_dialog_unknown_device_name) : detailsDialogDeviceInfo
                .getName();
        String deviceMacAddrString = TextUtils.isEmpty(detailsDialogDeviceInfo.getAddress()) ? context.getString(R.string.beacon_details_dialog_unknown_device_mac) : detailsDialogDeviceInfo
                .getAddress();
        deviceName.setText(deviceNameString);
        deviceMacAddr.setText(deviceMacAddrString);

        TextView beaconRssi = (TextView) dialog.findViewById(R.id.beacon_rssi);
        TextView beaconTxPower = (TextView) dialog.findViewById(R.id.beacon_tx_power);
        TextView beaconDetails = (TextView) dialog.findViewById(R.id.beacon_details);

        beaconRssi.setText(context.getString(R.string.beacon_details_dialog_rssi_label,
                                             detailsDialogDeviceInfo.getRssi()));
        // see getTxPowerLevel() of android.bluetooth.le.ScanRecord
        int txPowerLevel = detailsDialogDeviceInfo.scanInfo.getScanRecord().getTxPowerLevel();
        String txPowerString =
                txPowerLevel == Integer.MIN_VALUE ? context.getString(R.string.beacon_details_dialog_min_tx_power) :
                        "" + txPowerLevel;
        beaconTxPower.setText(context.getString(R.string.beacon_details_dialog_tx_power_label, txPowerString));

        String beaconDetailsHtml = "";
        beaconDetailsHtml += "";

        // get beacon specific information
        if (BleFormat.isEddyStone(detailsDialogDeviceInfo)) {
            deviceName.append(" " + context.getString(R.string.beacon_details_dialog_device_type_eddystone));

            Beacon beacon = deviceToEddystoneBeaconMap.get(detailsDialogDeviceInfo.getAddress());

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

            beaconDetailsHtml +=
                    "<b>" + context.getString(R.string.beacon_details_dialog_url) + ":</b> " + eddystoneUrl + "<br>";
            beaconDetailsHtml +=
                    "<b>" + context.getString(R.string.beacon_details_dialog_uid) + ":</b> " + eddystoneUidValue +
                            "<br>";
            beaconDetailsHtml += "<b>" + context.getString(R.string.beacon_details_dialog_instance) + ":</b> " +
                    eddystoneUidNameSpace + "<br><br>";

            beaconDetailsHtml +=
                    "<b>" + context.getString(R.string.beacon_details_dialog_tlm_data) + ":</b><br>" + eddystoneTlm +
                            "<br>";
        } else if (BleFormat.isIBeacon(detailsDialogDeviceInfo)) {
            deviceName.append(" " + context.getString(R.string.beacon_details_dialog_device_type_ibeacon));

            IBeaconInfo iBeaconInfo = BleFormat.getIBeaconInfo(detailsDialogDeviceInfo.scanInfo.getDevice(),
                                                               detailsDialogDeviceInfo.scanInfo.getRssi(),
                                                               detailsDialogDeviceInfo.scanInfo.getScanRecord()
                                                                       .getBytes());
            if (iBeaconInfo != null) {
                String uuid = iBeaconInfo.getUuid();
                int major = iBeaconInfo.getMajor();
                int minor = iBeaconInfo.getMinor();

                beaconDetailsHtml +=
                        "<b>" + context.getString(R.string.beacon_details_dialog_uuid) + ":</b> " + uuid + "<br><br>";
                beaconDetailsHtml +=
                        "<b>" + context.getString(R.string.beacon_details_dialog_major_number) + ":</b> " + major +
                                "<br>";
                beaconDetailsHtml +=
                        "<b>" + context.getString(R.string.beacon_details_dialog_minor_number) + ":</b> " + minor +
                                "<br>";
            }
        } else if (BleFormat.isAltBeacon(detailsDialogDeviceInfo)) {
            deviceName.append(" " + context.getString(R.string.beacon_details_dialog_device_type_alt_beacon));

            AltBeacon altBeacon = new AltBeacon(detailsDialogDeviceInfo);
            String beaconId = altBeacon.getAltBeaconId();
            String mfgId = altBeacon.getManufacturerId();
            String refRssi = "" + altBeacon.getAltBeaconReferenceRssi();

            beaconDetailsHtml +=
                    "<b>" + context.getString(R.string.beacon_details_dialog_beacon_id) + ":</b><br> " + beaconId +
                            "<br><br>";
            beaconDetailsHtml +=
                    "<b>" + context.getString(R.string.beacon_details_dialog_manufacturer_id) + ":</b> " + mfgId +
                            "<br><br>";
            beaconDetailsHtml +=
                    "<b>" + context.getString(R.string.beacon_details_dialog_reference_rssi) + ":</b> " + refRssi +
                            "&nbsp;dBm<br>";
        } else if (BleFormat.isBlueBeaconOld(detailsDialogDeviceInfo)) {
            deviceName.append(" " + context.getString(R.string.beacon_details_dialog_device_type_blue_gecko));
            beaconDetailsHtml += "<b>" + context.getString(R.string.beacon_details_dialog_uuid) + ":</b><br> " +
                    BLUE_GECKO_OLD_UUID + "<br><br>";
        } else if (BleFormat.isBlueBeaconNew(detailsDialogDeviceInfo)) {
            deviceName.append(" " + context.getString(R.string.beacon_details_dialog_device_type_blue_gecko));
            beaconDetailsHtml += "<b>" + context.getString(R.string.beacon_details_dialog_uuid) + ":</b><br> " +
                    BLUE_GECKO_ALT_BEACON_UUID + "<br><br>";
        }

        if (TextUtils.isEmpty(beaconDetailsHtml)) {
            beaconDetails.setText(context.getString(R.string.beacon_details_no_additional_info));
            beaconDetails.setGravity(Gravity.CENTER);
        } else {
            beaconDetails.setText(Html.fromHtml(beaconDetailsHtml));
            beaconDetails.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        }

        if (!dialog.isShowing()) {
            dialog.show();
        }
    }

    private void updateEddystoneBeaconUI(BeaconListFragment.ViewHolder viewHolder, BluetoothDeviceInfo deviceInfo) {
        Context context = viewHolder.container.getContext();
        Beacon beacon = deviceToEddystoneBeaconMap.get(deviceInfo.getAddress());

        String deviceName = deviceInfo.device.getName();
        deviceName = TextUtils.isEmpty(deviceName) ? context.getResources().getString(R.string.unknown) : deviceName;
        viewHolder.title.setText(deviceName);

        // Eddystone UID Advertisement Data: 16-byte Beacon ID (10-byte namespace, 6-byte instance)
        String eddystoneUid = beacon.uidStatus.uidValue;
        String eddystoneUrl = beacon.urlStatus.toString();
        if (eddystoneUrl.toLowerCase().equals(BLUE_GECKO_EDDYSTONE_URL)) {
            viewHolder.title.setText(context.getResources().getString(R.string.blue_gecko_details, deviceName));
        }

        String version = beacon.tlmStatus.version;
        String voltage = beacon.tlmStatus.voltage;
        String temperature = beacon.tlmStatus.temp;
        String advertisementCount = beacon.tlmStatus.advCnt;
        String uptimeCountInSeconds = beacon.tlmStatus.secCnt;
        String eddystoneTlm =
                context.getString(R.string.beacon_details_dialog_tlm_data) + ":\n" +
                        context.getString(R.string.beacon_details_dialog_tlm_version) + ": " + version + "\n" +
                        context.getString(R.string.beacon_details_dialog_tlm_voltage) + ": " + voltage + "\n" +
                        context.getString(R.string.beacon_details_dialog_tlm_temperature) + ": " + temperature + "\n" +
                        context.getString(R.string.beacon_details_dialog_tlm_advertisement_count) + ": " +
                        advertisementCount + "\n" +
                        context.getString(R.string.beacon_details_dialog_tlm_uptime) + ": " + uptimeCountInSeconds;

        if (TextUtils.isEmpty(eddystoneUrl)) {
            viewHolder.eddystoneUrl.setVisibility(View.GONE);
        } else {
            viewHolder.eddystoneUrl.setVisibility(View.VISIBLE);
            viewHolder.eddystoneUrl.setText(String.format("%s: %s", context.getString(R.string.beacon_details_dialog_url), eddystoneUrl));
        }

        if (TextUtils.isEmpty(eddystoneUid)) {
            viewHolder.eddystoneUid.setVisibility(View.GONE);
        } else {
            viewHolder.eddystoneUid.setVisibility(View.VISIBLE);
            viewHolder.eddystoneUid.setText(String.format("%s: %s\n%s: %s",
                                                          context.getString(R.string.beacon_details_dialog_uid),
                                                          eddystoneUid.substring(0, 20),
                                                          context.getString(R.string.beacon_details_dialog_instance),
                                                          eddystoneUid.substring(20)));
        }

        viewHolder.iBeaconInfoContainer.setVisibility(View.GONE);
        viewHolder.eddystoneInfoContainer.setVisibility(View.VISIBLE);
        viewHolder.altBeaconInfoContainer.setVisibility(View.GONE);
    }

    private void updateIBeaconUI(BeaconListFragment.ViewHolder viewHolder, BluetoothDeviceInfo deviceInfo) {
        Context context = viewHolder.container.getContext();

        String deviceName = deviceInfo.device.getName();
        deviceName = TextUtils.isEmpty(deviceName) ? context.getResources().getString(R.string.unknown) : deviceName;
        viewHolder.title.setText(deviceName);

        IBeaconInfo iBeaconInfo = BleFormat.getIBeaconInfo(deviceInfo.scanInfo.getDevice(),
                                                           deviceInfo.scanInfo.getRssi(),
                                                           deviceInfo.scanInfo.getScanRecord().getBytes());
        if (iBeaconInfo != null) {
            String uuid = iBeaconInfo.getUuid();
            int major = iBeaconInfo.getMajor();
            int minor = iBeaconInfo.getMinor();
            viewHolder.scanInfoUuid.setText(context.getString(R.string.beacon_details_dialog_uuid) + ": " + uuid);
            viewHolder.majorNumber.setText(context.getString(R.string.beacon_details_dialog_major) + ": " + major);
            viewHolder.minorNumber.setText(context.getString(R.string.beacon_details_dialog_minor) + ": " + minor);
            viewHolder.iBeaconInfoContainer.setVisibility(View.VISIBLE);
            viewHolder.eddystoneInfoContainer.setVisibility(View.GONE);
            viewHolder.altBeaconInfoContainer.setVisibility(View.GONE);

            if (major == BLUE_GECKO_IBEACON_MAJOR && minor == BLUE_GECKO_IBEACON_MINOR &&
                    uuid.toUpperCase().equals(BLUE_GECKO_IBEACON_UUID)) {
                viewHolder.title.setText(context.getResources().getString(R.string.blue_gecko_details, deviceName));
            }
        }
    }

    private void updateBlueGeckoUI(BeaconListFragment.ViewHolder viewHolder, BluetoothDeviceInfo deviceInfo) {
        Context context = viewHolder.container.getContext();

        String deviceName = deviceInfo.device.getName();
        deviceName = TextUtils.isEmpty(deviceName) ? context.getResources().getString(R.string.unknown) : deviceName;
        viewHolder.title.setText(deviceName);

        IBeaconInfo iBeaconInfo = BleFormat.getIBeaconInfo(deviceInfo.scanInfo.getDevice(),
                                                           deviceInfo.scanInfo.getRssi(),
                                                           deviceInfo.scanInfo.getScanRecord().getBytes());
        if (iBeaconInfo != null) {
            String uuid = iBeaconInfo.getUuid();
            int major = iBeaconInfo.getMajor();
            int minor = iBeaconInfo.getMinor();
        }

        if (BleFormat.isBlueBeaconOld(deviceInfo)) {
            viewHolder.scanInfoUuid.setText(
                    context.getString(R.string.beacon_details_dialog_uuid) + ": " + BLUE_GECKO_OLD_UUID);
        } else if (BleFormat.isBlueBeaconNew(deviceInfo)) {
            viewHolder.scanInfoUuid.setText(context.getString(R.string.beacon_details_dialog_uuid) + ": " +
                                                    BLUE_GECKO_ALT_BEACON_UUID.substring(0, 10) + "...");
        } else {
            viewHolder.scanInfoUuid.setText(context.getString(R.string.beacon_details_dialog_uuid) + ": Unknown");
        }
        viewHolder.majorNumber.setVisibility(View.GONE);
        viewHolder.minorNumber.setVisibility(View.GONE);
        viewHolder.iBeaconInfoContainer.setVisibility(View.VISIBLE);
        viewHolder.eddystoneInfoContainer.setVisibility(View.GONE);
        viewHolder.altBeaconInfoContainer.setVisibility(View.GONE);
    }

    private void updateAltBeaconUI(BeaconListFragment.ViewHolder viewHolder, BluetoothDeviceInfo deviceInfo) {
        Context context = viewHolder.container.getContext();

        String deviceName = deviceInfo.device.getName();
        deviceName = TextUtils.isEmpty(deviceName) ? context.getResources().getString(R.string.unknown) : deviceName;
        viewHolder.title.setText(deviceName);

        AltBeacon altBeacon = new AltBeacon(deviceInfo);

        String beaconId = altBeacon.getAltBeaconId();
        boolean isGeckoPayload = beaconId.startsWith(BLUE_GECKO_ALT_BEACON_UUID);
        if (beaconId.length() > 8 && !isTablet(context)) {
            beaconId = beaconId.substring(0, 10) + "...";
        }
        viewHolder.altBeaconId.setText(context.getResources().getString(R.string.beacon_id_details, beaconId));
        viewHolder.altBeaconManufacturerId.setText("Manufacturer Id: " + altBeacon.getManufacturerId() + ".");
        viewHolder.altBeaconReferenceRssi.setText(context.getResources().getString(R.string.beacon_id_details,
                                                                     String.valueOf(altBeacon.getAltBeaconReferenceRssi())));

        if (altBeacon.getManufacturerId().equals(BLUE_GECKO_ALT_BEACON_MFG_ID) && isGeckoPayload) {
            viewHolder.title.setText(context.getResources().getString(R.string.blue_gecko_details, deviceName));
        }

        viewHolder.iBeaconInfoContainer.setVisibility(View.GONE);
        viewHolder.eddystoneInfoContainer.setVisibility(View.GONE);
        viewHolder.altBeaconInfoContainer.setVisibility(View.VISIBLE);
    }

    public void updateWith(List<T> devicesInfo) {
        updateWith(devicesInfo, true);
    }

    public void updateWith(List<T> devicesInfo, boolean removeOld) { //TODO Filter implementation
        final Long now = System.currentTimeMillis();
        mostRecentDevicesInfoIsDirty = true;

        if ((devicesInfo != null) && !devicesInfo.isEmpty()) {
            for (T devInfo : devicesInfo) {
                T clone = (T) devInfo.clone();
                clone.isOfInterest = true;
                clone.isNotOfInterest = false;
                clone.serviceDiscoveryFailed = false;

                int index = mostRecentDevicesInfo.indexOf(clone);
                if (index >= 0) {
                    BluetoothDeviceInfo cachedInfo = mostRecentDevicesInfo.get(index);
                    long timestampDiff = clone.scanInfo.getTimestampNanos() - cachedInfo.scanInfo.getTimestampNanos();
                    if (timestampDiff != 0) {
                        Log.i("Time Diff", "Updated " + cachedInfo.scanInfo.getDisplayName(false));
                        mostRecentDevicesInfo.set(index, clone);
                        mostRecentInfoAge.put(devInfo, now);
                    }
                } else {
                    mostRecentDevicesInfo.add(devInfo);
                    mostRecentInfoAge.put(devInfo, now);
                }

                // if device is an eddystone beacon, update eddystone map of devices
                if (BleFormat.isEddyStone(devInfo)) {
                    updateEddyBeacons(devInfo);
                }
            }
        }

        if (removeOld) {
            final Iterator<Map.Entry<T, Long>> iter = mostRecentInfoAge.entrySet().iterator();
            while (iter.hasNext()) {
                final Map.Entry<T, Long> ageEntry = iter.next();
                long age = now.longValue() - ageEntry.getValue().longValue();
                if (age > MAX_AGE) {
                    Log.i("Time Diff", "Removed old " + ageEntry.getKey().scanInfo.getDisplayName(false));
                    mostRecentDevicesInfo.remove(ageEntry.getKey());
                    iter.remove();
                }
            }
        }

        if (this.devicesInfo.isEmpty()) {
            updateDevicesInfo();
        } else if (!updatePending && runUpdater) {
            updatePending = true;
            handler.postDelayed(delayedUpdater, 2000);
        }
    }

    private void updateEddyBeacons(BluetoothDeviceInfo deviceInfo) {
        ScanResultCompat scanInfo = deviceInfo.scanInfo;
        String deviceAddress = scanInfo.getDevice().getAddress();
        Beacon beacon;

        // only add new discoveries, info will be updated if already discovered
        if (!deviceToEddystoneBeaconMap.containsKey(deviceAddress)) {
            beacon = new Beacon(deviceAddress, scanInfo.getRssi());
            // we are using the map just to keep track of the eddystone beacons
            // the device has already been or will be added to the array adapter
            deviceToEddystoneBeaconMap.put(deviceAddress, beacon);
        }

        byte[] serviceData = scanInfo.getScanRecord().getServiceData().get(EDDYSTONE_SERVICE_UUID);
        validateEddyStoneServiceData(deviceAddress, serviceData);
    }

    // Checks the frame type and hands off the service data to the validation module.
    private void validateEddyStoneServiceData(String deviceAddress, byte[] serviceData) {
        Beacon beacon = deviceToEddystoneBeaconMap.get(deviceAddress);
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

        notifyDataSetChanged();
    }

    public void setRunUpdater(boolean runUpdater) {
        this.runUpdater = runUpdater;
    }

    private void updateDevicesInfo() {
        updatePending = false;
        mostRecentDevicesInfoIsDirty = false;

        if (mostRecentDevicesInfo.isEmpty()) {
            clear();
            return;
        }

        this.devicesInfo.clear();
        this.devicesInfo.addAll(mostRecentDevicesInfo);
        if (this.devicesInfo.size() > 1) {
            Collections.sort(this.devicesInfo, DEVICE_INFO_RSSI_COMPARATOR);
        }
        notifyDataSetChanged();
    }

    public void clear() {
        mostRecentDevicesInfo.clear();
        mostRecentInfoAge.clear();
        deviceToEddystoneBeaconMap.clear();

        if (!devicesInfo.isEmpty()) {
            int size = devicesInfo.size();
            devicesInfo.clear();
            notifyItemRangeRemoved(0, size);
        }
    }

    @Override
    public void flushContainer() {
        clear();
    }

    @Override
    public void updateWithDevices(List devices) {
        updateWith(devices);
    }

    public List<T> getDevicesInfo() {
        return devicesInfo;
    }

    public boolean isTablet(Context context) {
        boolean xlarge = (
                (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == 4);
        boolean large = (
                (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) ==
                        Configuration.SCREENLAYOUT_SIZE_LARGE);
        return (xlarge || large);
    }
}
