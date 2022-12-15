package com.siliconlabs.bledemo.features.scan.browser.fragments

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.PropertyContainerBinding
import com.siliconlabs.bledemo.features.configure.gatt_configurator.models.Property
import com.siliconlabs.bledemo.utils.BLEUtils
import com.siliconlabs.bledemo.utils.Notifications

class RemoteServicesFragment : ServicesFragment(isRemote = true) {

    override var services: List<BluetoothGattService> = bluetoothGatt?.services.orEmpty()

    override fun readCharacteristic(bluetoothGattCharacteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.readCharacteristic(bluetoothGattCharacteristic)
    }

    override fun readDescriptor(descriptor: BluetoothGattDescriptor) {
        bluetoothGatt?.readDescriptor(descriptor)
    }

    override fun setServicesList(services: List<BluetoothGattService>) {
        this.services = services
    }

    fun toggleNotifications(
        bluetoothGattCharacteristic: BluetoothGattCharacteristic,
        notifyPropertyContainer: PropertyContainerBinding?,
        indicatePropertyContainer: PropertyContainerBinding?
    ) {

        var notificationsEnabled = currentWriteReadFragment?.notificationsEnabled ?: return
        val isNewStateSubmittedCorrectly = BLEUtils.setNotificationForCharacteristic(
            bluetoothGatt ?: return,
            bluetoothGattCharacteristic,
            if (notificationsEnabled) Notifications.DISABLED else Notifications.NOTIFY
        )

        if (isNewStateSubmittedCorrectly) {
            notificationsEnabled = !notificationsEnabled
        }

        currentWriteReadFragment?.apply {
            this.notificationsEnabled = notificationsEnabled
            this.indicationsEnabled = false
        }

        updatePropertyView(notifyPropertyContainer, notificationsEnabled)
        updatePropertyView(indicatePropertyContainer, isEnabled = false)
    }

    fun toggleIndications(
            bluetoothGattCharacteristic: BluetoothGattCharacteristic,
            indicatePropertyContainer: PropertyContainerBinding?,
            notifyPropertyContainer: PropertyContainerBinding?
    ) {
        var indicationsEnabled = currentWriteReadFragment?.indicationsEnabled!!
        val isNewStateSubmittedCorrectly = BLEUtils.setNotificationForCharacteristic(
                bluetoothGatt ?: return,
                bluetoothGattCharacteristic,
                if (indicationsEnabled) Notifications.DISABLED else Notifications.INDICATE
        )

        if (isNewStateSubmittedCorrectly) {
            indicationsEnabled = !indicationsEnabled
        }

        currentWriteReadFragment?.apply {
            this.indicationsEnabled = indicationsEnabled
            this.notificationsEnabled = false
        }

        updatePropertyView(indicatePropertyContainer, indicationsEnabled)
        updatePropertyView(notifyPropertyContainer, isEnabled = false)
    }

    private fun updatePropertyView(container: PropertyContainerBinding?, isEnabled: Boolean) {
        container?.apply {
            propertyText.setTextColor(ContextCompat.getColor(requireContext(),
                    if (isEnabled) R.color.silabs_blue
                    else R.color.silabs_black)
            )
            ImageViewCompat.setImageTintList(
                    propertyIcon, ColorStateList.valueOf(ContextCompat.getColor(requireContext(),
                        if (isEnabled) R.color.blue_primary
                        else R.color.silabs_dark_gray_icon
            )))
        }
    }

}
