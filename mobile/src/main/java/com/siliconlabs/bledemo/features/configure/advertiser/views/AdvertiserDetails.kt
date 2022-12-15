package com.siliconlabs.bledemo.features.configure.advertiser.views

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.widget.LinearLayout
import com.siliconlabs.bledemo.features.configure.advertiser.models.Advertiser
import com.siliconlabs.bledemo.features.configure.advertiser.models.AdvertiserData
import com.siliconlabs.bledemo.features.configure.advertiser.models.DataPacket
import com.siliconlabs.bledemo.features.configure.advertiser.models.Manufacturer
import com.siliconlabs.bledemo.features.configure.advertiser.utils.Translator
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.common.views.DetailsRow

class AdvertiserDetails(val context: Context) {

    fun getAdvertiserDetailsView(item: Advertiser, translator: Translator): LinearLayout {
        val data = item.data

        return LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            orientation = LinearLayout.VERTICAL

            val advertisingType = (if (data.isLegacy) context.getString(R.string.advertiser_label_legacy)
            else context.getString(R.string.advertiser_label_extended)).plus(" (").plus(translator.getString(data.mode)).plus(")")
            addView(DetailsRow(context, context.getString(R.string.advertiser_title_advertising_type), advertisingType))

            if (!data.isLegacy) addView(DetailsRow(context, context.getString(R.string.Bluetooth_5_Advertising_Extension), getAdvertisingExtensionText(context, translator, item.data)))

            if (data.isAdvertisingData()) {
                if (data.mode.isConnectable()) addView(DetailsRow(context, context.getString(R.string.advertiser_data_type_flags), context.getString(R.string.advertiser_label_flags_default)))

                if (data.advertisingData.includeCompleteLocalName)
                    addView(DetailsRow(context, context.getString(R.string.advertiser_data_type_complete_local_name), getCompleteLocalName(context, data.advertisingData)))

                for (manufacturer in data.advertisingData.manufacturers)
                    addView(DetailsRow(context, context.getString(R.string.advertiser_data_type_manufacturer_specific_data), getManufacturerData(manufacturer)))

                if (data.advertisingData.includeTxPower)
                    addView(DetailsRow(context, context.getString(R.string.advertiser_data_type_tx_power), getTxPower(context, data)))

                if (data.advertisingData.services16Bit.size > 0)
                    addView(DetailsRow(context, context.getString(R.string.advertiser_data_type_complete_16bit_service), get16BitServicesText(item.data.advertisingData)))

                if (data.advertisingData.services128Bit.size > 0)
                    addView(DetailsRow(context, context.getString(R.string.advertiser_data_type_complete_128bit_service), get128BitServicesText(item.data.advertisingData)))
            }

            if (data.isScanRespData()) {

                if (data.scanResponseData.includeCompleteLocalName)
                    addView(DetailsRow(context, context.getString(R.string.advertiser_data_type_complete_local_name), getCompleteLocalName(context, data.scanResponseData)))

                for (manufacturer in data.scanResponseData.manufacturers)
                    addView(DetailsRow(context, context.getString(R.string.advertiser_data_type_manufacturer_specific_data), getManufacturerData(manufacturer)))

                if (data.scanResponseData.includeTxPower)
                    addView(DetailsRow(context, context.getString(R.string.advertiser_data_type_tx_power), getTxPower(context, data)))

                if (data.scanResponseData.services16Bit.size > 0)
                    addView(DetailsRow(context, context.getString(R.string.advertiser_data_type_complete_16bit_service), get16BitServicesText(item.data.scanResponseData)))

                if (data.scanResponseData.services128Bit.size > 0)
                    addView(DetailsRow(context, context.getString(R.string.advertiser_data_type_complete_128bit_service), get128BitServicesText(item.data.scanResponseData)))
            }
        }
    }

    private fun get16BitServicesText(data: DataPacket): String {
        return StringBuilder().apply {
            for (service in data.services16Bit) {
                append(service.toString())
                if (data.services16Bit.indexOf(service) != data.services16Bit.size - 1) append(",\n")
            }
        }.toString()
    }

    private fun get128BitServicesText(data: DataPacket): String {
        return StringBuilder().apply {
            for (service in data.services128Bit) {
                append(service.toString())
                if (data.services128Bit.indexOf(service) != data.services128Bit.size - 1) append(",\n")
            }
        }.toString()
    }

    private fun getCompleteLocalName(context: Context, data: DataPacket): String {
        return if (data.includeCompleteLocalName) BluetoothAdapter.getDefaultAdapter().name
        else context.getString(R.string.not_advertising_shortcut)
    }

    private fun getTxPower(context: Context, data: AdvertiserData): String {
        return StringBuilder().apply {
            append(context.getString(R.string.unit_value_dbm, data.txPower))
        }.toString()
    }

    private fun getManufacturerData(manufacturer: Manufacturer): String {
        return manufacturer.getAsDescriptiveText()
    }

    private fun getAdvertisingExtensionText(context: Context, translator: Translator, data: AdvertiserData): String {
        return StringBuilder().apply {
            append(context.getString(R.string.Primary_PHY_colon)).append(" ").append(translator.getString(data.settings.primaryPhy)).append("\n")
                    .append(context.getString(R.string.Secondary_PHY_colon)).append(" ").append(translator.getString(data.settings.secondaryPhy)).append("\n")
                    .append(context.getString(R.string.Advertising_Set_ID)).append(" ").append(1)
            if (data.settings.includeTxPower) append("\n").append(context.getString(R.string.Tx_Power)).append(" ").append(context.getString(R.string.unit_value_dbm, data.txPower))
        }.toString()
    }

}