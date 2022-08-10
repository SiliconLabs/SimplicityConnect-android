package com.siliconlabs.bledemo.Browser.Adapters

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.ParcelUuid
import android.text.Html
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.siliconlabs.bledemo.Adapters.DeviceInfoViewHolder
import com.siliconlabs.bledemo.Adapters.ScannedDevicesAdapter
import com.siliconlabs.bledemo.BeaconUtils.BleFormat
import com.siliconlabs.bledemo.BeaconUtils.altbeacon.AltBeacon
import com.siliconlabs.bledemo.BeaconUtils.eddystone.*
import com.siliconlabs.bledemo.BeaconUtils.ibeacon.IBeaconInfo
import com.siliconlabs.bledemo.Bluetooth.BLE.BluetoothDeviceInfo
import com.siliconlabs.bledemo.Bluetooth.BLE.ScanResultCompat
import com.siliconlabs.bledemo.Bluetooth.ConnectedGatts
import com.siliconlabs.bledemo.Bluetooth.Parsing.ScanRecordParser
import com.siliconlabs.bledemo.Bluetooth.Services.BluetoothService
import com.siliconlabs.bledemo.Browser.Activities.BrowserActivity
import com.siliconlabs.bledemo.Browser.DebugModeCallback
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.Views.DetailsRow
import com.siliconlabs.bledemo.utils.SharedPrefUtils
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

class DebugModeDeviceAdapter(mContext: Context, generator: DeviceInfoViewHolder.Generator) : ScannedDevicesAdapter(generator, mContext) {

    class ViewHolder(private val context: Context, view: View?, private var debugModeCallback: DebugModeCallback, private val sharedPrefUtils: SharedPrefUtils) : DeviceInfoViewHolder(view), View.OnClickListener {
        private var cardView = itemView.findViewById(R.id.card_view) as CardView
        private var deviceName = itemView.findViewById(R.id.device_name) as TextView
        private var deviceRssi = itemView.findViewById(R.id.rssi) as TextView
        private var deviceType = itemView.findViewById(R.id.device_type) as TextView
        private var connectBtn = itemView.findViewById(R.id.connect_btn) as Button
        private var disconnectBtn = itemView.findViewById(R.id.disconnect_btn) as Button
        private var favoriteBtn = itemView.findViewById(R.id.favorite_btn) as CheckBox
        private var advertisementContainer = itemView.findViewById(R.id.advertisement_container) as LinearLayout
        private var isConnectableTV = itemView.findViewById(R.id.tv_is_connectable) as TextView
        private var intervalTV = itemView.findViewById(R.id.tv_interval) as TextView
        private var addressTV = itemView.findViewById(R.id.tv_device_address) as TextView

        private var device: BluetoothDeviceInfo? = null
        private var bleFormat: BleFormat? = null
        private var bluetoothBinding: BluetoothService.Binding? = null

        private fun refreshDeviceRowOnUiThread(isConnected: Boolean) {
            if (context is BrowserActivity) {
                context.runOnUiThread {
                    refreshButtonsConnected(isConnected)
                    debugModeCallback.updateCountOfConnectedDevices()
                }
            }
        }

        private fun refreshButtonsConnected(isConnected: Boolean) {
            if (isConnected) {
                showDisconnectButton()
            } else {
                showConnectButton()
            }
        }

        private fun showDisconnectButton() {
            connectBtn.visibility = View.GONE
            disconnectBtn.visibility = View.VISIBLE
        }

        private fun showConnectButton() {
            connectBtn.visibility = View.VISIBLE
            disconnectBtn.visibility = View.GONE
        }


        private fun isConnected(address: String): Boolean {
            return ConnectedGatts.isGattWithAddressConnected(address)
        }

        override fun setData(info: BluetoothDeviceInfo, position: Int, size: Int) {
            // set data for the list item
            device = info
            refreshButtonsConnected(isConnected(device?.address!!))
            val margin16Dp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, context.resources.displayMetrics).toInt()
            val margin10Dp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, context.resources.displayMetrics).toInt()
            val layoutParams = cardView.layoutParams as ViewGroup.MarginLayoutParams

            when (position) {
                0 -> layoutParams.setMargins(margin16Dp, margin16Dp, margin16Dp, margin10Dp)
                size - 1 -> layoutParams.setMargins(margin16Dp, margin10Dp, margin16Dp, margin16Dp)
                else -> layoutParams.setMargins(margin16Dp, margin10Dp, margin16Dp, margin10Dp)
            }

            cardView.requestLayout()

            if (device?.isConnectable!!) {
                connectBtn.visibility = View.VISIBLE
                isConnectableTV.text = context.resources.getString(R.string.Connectable)
            } else {
                connectBtn.visibility = View.GONE
                isConnectableTV.text = context.resources.getString(R.string.Non_Connectable)
            }

            intervalTV.text = (device?.intervalNanos!! / 1000000).toString()

            device?.let {
                val isBonded = it.device.bondState == BluetoothDevice.BOND_BONDED
                addressTV.text = context.resources.getString(R.string.device_address_with_bond_state, it.address, if (isBonded) "BONDED" else "NOT BONDED")
            }

            val deviceNameText = if (device?.name == null || device?.name?.isEmpty()!!) context.resources.getString(R.string.not_advertising_shortcut) else device?.name
            deviceName.text = deviceNameText

            deviceRssi.text = context.resources.getString(R.string.n_dBm, device?.rssi!!)
            bleFormat = device?.getBleFormat()
            deviceType.text = context.resources.getString(bleFormat?.nameResId!!)

            val isFavorite = sharedPrefUtils.isFavorite(device?.address) || sharedPrefUtils.isTemporaryFavorite(device?.address)
            favoriteBtn.isChecked = isFavorite
            if (sharedPrefUtils.isFavorite(device?.address)) {
                debugModeCallback.removeFromFavorite(device?.address!!)
                debugModeCallback.addToTemporaryFavorites(device?.address!!)
            }

            advertisementContainer.removeAllViews()
            if (CURRENT_ADVERTISEMENT_DATA_MAP.containsKey(device?.address!!)) {
                generateAdvertData()
                addAdvertsToContainer(CURRENT_ADVERTISEMENT_DATA_MAP[device?.address!!])
            }

            favoriteBtn.setOnClickListener {
                val isFavorite = sharedPrefUtils.isFavorite(device?.address) || sharedPrefUtils.isTemporaryFavorite(device?.address)
                if (isFavorite) {
                    debugModeCallback.removeFromFavorite(device?.address!!) //Remove device from temporaryDevices and devices
                } else {
                    debugModeCallback.addToTemporaryFavorites(device?.address!!) //Ad device to temporaryDevices
                }
            }

            cardView.setOnClickListener(this)
            disconnectBtn.setOnClickListener(this)
            connectBtn.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            when (v.id) {
                R.id.card_view -> toggleAdvertData()
                R.id.disconnect_btn -> disconnect()
                R.id.connect_btn -> connect()
                else -> {
                }
            }
        }

        private fun connect() {
            device?.apply {
                debugModeCallback.connectToDevice(device)
            }
        }

        private fun disconnect() {
            bluetoothBinding = object : BluetoothService.Binding(context) {
                override fun onBound(service: BluetoothService?) {
                    service?.disconnectGatt(device?.address!!)
                    refreshDeviceRowOnUiThread(false)
                }
            }
            bluetoothBinding?.bind()
        }

        private fun toggleAdvertData() {
            if (CURRENT_ADVERTISEMENT_DATA_MAP.containsKey(device?.address)) {
                CURRENT_ADVERTISEMENT_DATA_MAP.remove(device?.address)
                advertisementContainer.removeAllViews()
            } else {
                generateAdvertData()
                addAdvertsToContainer(CURRENT_ADVERTISEMENT_DATA_MAP[device?.address!!])
            }
        }

        private fun generateAdvertData() {
            val rows: MutableList<Pair<String, String>> = ArrayList()

            // If not legacy, prepare extra Advertising Extension to show
            if (!device?.scanInfo?.isLegacy!!) {
                rows.add(Pair(context.resources.getString(R.string.Bluetooth_5_Advertising_Extension),
                        prepareBluetooth5AdvertExtensionData(device?.scanInfo!!)))
            }
            device?.advertData?.forEach {
                val data = it?.split(ScanRecordParser.SPLIT.toRegex())?.toTypedArray()!!
                val dataLabel = data[0]
                val dataValue =
                        if (data.size > 1) data[1]
                        else ""
                rows.add(Pair(dataLabel, dataValue))
            }

            when (bleFormat) {
                BleFormat.I_BEACON -> iBeaconAdv(rows)
                BleFormat.EDDYSTONE -> eddystoneAdv(rows)
                BleFormat.ALT_BEACON -> altBeaconAdv(rows)
                else -> { }
            }

            CURRENT_ADVERTISEMENT_DATA_MAP[device?.address!!] = AdvertisementData(rows)
        }

        private fun prepareBluetooth5AdvertExtensionData(scanResult: ScanResultCompat): String {
            val builder = StringBuilder()

            //Data status
            builder.append(context.getString(R.string.Data_Status_colon))
                    .append(" ")
            if (scanResult.dataStatus == 0) builder.append(context.getString(R.string.advertising_extension_status_complete)) else builder.append(context.getString(R.string.advertising_extension_status_truncated))
            builder.append("<br/>")

            //Primary PHY
            builder.append(context.getString(R.string.Primary_PHY_colon))
                    .append(" ")
            if (scanResult.primaryPhy == 1) builder.append(context.getString(R.string.advertising_extension_phy_le_1m)) else builder.append(context.getString(R.string.advertising_extension_phy_le_coded))
            builder.append("<br/>")

            //Secondary PHY
            builder.append(context.getString(R.string.Secondary_PHY_colon))
                    .append(" ")
            if (scanResult.secondaryPhy == 1) builder.append(context.getString(R.string.advertising_extension_phy_le_1m)) else if (scanResult.secondaryPhy == 2) builder.append(context.getString(R.string.advertising_extension_phy_le_2m)) else if (scanResult.secondaryPhy == 3) builder.append(context.getString(R.string.advertising_extension_phy_le_coded)) else builder.append(context.getString(R.string.advertising_extension_phy_unused))
            builder.append("<br/>")

            //Advertising Set ID
            builder.append(context.getString(R.string.Advertising_Set_ID))
                    .append(" ")
            if (scanResult.advertisingSetID == 255) builder.append(context.getString(R.string.advertising_extension_not_present)) else builder.append(scanResult.advertisingSetID)
            builder.append("<br/>")

            //Tx Power
            builder.append(context.getString(R.string.Tx_Power))
                    .append(" ")
            if (scanResult.txPower == 127) builder.append(context.getString(R.string.advertising_extension_not_present)) else builder.append(scanResult.txPower).append("dBm")
            builder.append("<br/>")

            //Periodic Advertising Interval
            builder.append(context.getString(R.string.Periodic_Advertising_Interval_colon))
                    .append(" ")
            if (scanResult.periodicAdvertisingInterval in 6..65536) {
                val ms = scanResult.periodicAdvertisingInterval * 1.25
                builder.append(ms)
                        .append("ms")
            } else {
                builder.append(context.getString(R.string.advertising_extension_not_present))
            }
            return builder.toString()
        }

        private fun validateEddyStoneServiceData(beacon: Beacon, deviceAddress: String, serviceData: ByteArray?) {
            if (serviceData == null) {
                val err = "Null Eddystone service data"
                beacon.frameStatus.nullServiceData = err
                return
            }
            when (serviceData[0]) {
                Constants.UID_FRAME_TYPE -> UidValidator.validate(deviceAddress, serviceData, beacon)
                Constants.TLM_FRAME_TYPE -> TlmValidator.validate(deviceAddress, serviceData, beacon)
                Constants.URL_FRAME_TYPE -> UrlValidator.validate(deviceAddress, serviceData, beacon)
                else -> {
                    val err = String.format("Invalid frame type byte %02X", serviceData[0])
                    beacon.frameStatus.invalidFrameType = err
                }
            }
        }

        private fun addAdvertsToContainer(advertisementData: AdvertisementData?) {
            advertisementData?.rows?.forEach {
                val title = it.first
                val text = Html.fromHtml(it.second).toString()
                val serviceItemContainer = DetailsRow(context, title, text)
                advertisementContainer.addView(serviceItemContainer, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
        }

        private fun iBeaconAdv(rows: MutableList<Pair<String, String>>) {
            IBeaconInfo.getIBeaconInfo(device?.scanInfo?.scanRecord?.bytes!!)?.let { beaconInfo ->
                val info = StringBuilder().apply {
                    append("Minor: ").append(beaconInfo.minor).append("<br>")
                    append("Major: ").append(beaconInfo.major).append("<br>")
                    append("UUID: ").append(beaconInfo.uuid).append("<br>")
                    append("RSSI at 1m: ").append(beaconInfo.power)
                }.toString()
                rows.add(Pair("iBeacon data", info))
            }
        }

        private fun eddystoneAdv(rows: MutableList<Pair<String, String>>) {
            var dataValue = ""
            val scanInfo = device?.scanInfo
            val deviceAddress = scanInfo?.device?.address
            val beacon = Beacon(deviceAddress!!, scanInfo.rssi)
            val serviceData = scanInfo.scanRecord?.serviceData!![EDDYSTONE_SERVICE_UUID]
            validateEddyStoneServiceData(beacon, deviceAddress, serviceData)

            // get url string
            var eddystoneUrl = beacon.urlStatus.toString()
            eddystoneUrl = if (TextUtils.isEmpty(eddystoneUrl)) context.getString(R.string.beacon_details_dialog_unknown_value) else eddystoneUrl

            // get uid string
            // Eddystone UID Advertisement Data: 16-byte Beacon ID (10-byte namespace, 6-byte instance)
            val eddystoneUid = beacon.uidStatus.uidValue
            val eddystoneUidValue = if (TextUtils.isEmpty(eddystoneUid)) context.getString(R.string.beacon_details_dialog_unknown_value) else eddystoneUid?.substring(0, 20)
            val eddystoneUidNameSpace = if (TextUtils.isEmpty(eddystoneUid)) context.getString(R.string.beacon_details_dialog_unknown_value) else "" + eddystoneUid?.substring(20)

            // get tlm data
            val version = beacon.tlmStatus.version
            //beacon.tlmstatus.voltage returns value in millivolts
            var voltage = beacon.tlmStatus.voltage
            var voltageValue = 0.0
            if (voltage != null) {
                voltageValue = voltage.toDouble()
                voltage = (voltageValue / 1000).toString() + " " + context.getString(R.string.beacon_details_dialog_unit_volts)
            }
            val temperature = beacon.tlmStatus.temp + " " +
                    context.getString(R.string.beacon_details_dialog_unit_degrees_celsius)
            val advertisementCount = beacon.tlmStatus.advCnt
            var uptimeCountInSeconds = beacon.tlmStatus.deciSecondsCntVal / 10
            uptimeCountInSeconds = Math.round(uptimeCountInSeconds * 10) / 10.0
            val secondsLabel = context.getString(R.string.beacon_details_dialog_unit_seconds_abbreviated)
            val daysLabel = context.getString(R.string.beacon_details_dialog_unit_days)
            val uptimeCount = String.format("%d $secondsLabel (%d $daysLabel)",
                    uptimeCountInSeconds.toInt(),
                    TimeUnit.SECONDS.toDays(uptimeCountInSeconds.roundToLong()))
            var eddystoneTlm = ""
            eddystoneTlm += context.getString(R.string.beacon_details_dialog_tlm_version) + ": " + version + "<br>"
            eddystoneTlm += context.getString(R.string.beacon_details_dialog_tlm_voltage) + ": " + voltage + "<br>"
            eddystoneTlm += context.getString(R.string.beacon_details_dialog_tlm_temperature) + ": " + temperature +
                    "<br>"
            eddystoneTlm += context.getString(R.string.beacon_details_dialog_tlm_advertisement_count) + ": " +
                    advertisementCount + "<br>"
            eddystoneTlm += context.getString(R.string.beacon_details_dialog_tlm_uptime) + ": " + uptimeCount
            eddystoneTlm = if (TextUtils.isEmpty(eddystoneTlm)) context.getString(R.string.beacon_details_dialog_unknown_value) else eddystoneTlm
            dataValue += context.getString(R.string.beacon_details_dialog_url) + ": " + eddystoneUrl + "<br>"
            dataValue += context.getString(R.string.beacon_details_dialog_uid) + ": " + eddystoneUidValue +
                    "<br>"
            dataValue += context.getString(R.string.beacon_details_dialog_instance) + ": " +
                    eddystoneUidNameSpace + "<br><br>"
            dataValue += context.getString(R.string.beacon_details_dialog_tlm_data) + ":<br>" + eddystoneTlm +
                    "<br>"
            rows.add(Pair("Eddystone data", dataValue))
        }

        private fun altBeaconAdv(rows: MutableList<Pair<String, String>>) {
            var dataValue = ""

            val altBeacon = AltBeacon(device!!)
            val beaconId = altBeacon.altBeaconId
            val mfgId = altBeacon.manufacturerId
            val refRssi = "" + altBeacon.altBeaconReferenceRssi
            dataValue += context.getString(R.string.beacon_details_dialog_beacon_id) + ":<br> " + beaconId +
                    "<br><br>"
            dataValue +=  context.getString(R.string.beacon_details_dialog_manufacturer_id) + ": " + mfgId +
                    "<br><br>"
            dataValue +=  context.getString(R.string.beacon_details_dialog_reference_rssi) + ": " + refRssi +
                    "&nbsp;dBm<br>"
            rows.add(Pair("AltBeacon data", dataValue))
        }
    }

    class AdvertisementData(val rows: List<Pair<String, String>>)

    companion object {
        private val CURRENT_ADVERTISEMENT_DATA_MAP: MutableMap<String, AdvertisementData?> = HashMap()
        val EDDYSTONE_SERVICE_UUID = ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB")
    }
}
