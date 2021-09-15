package com.siliconlabs.bledemo.Browser.Fragment

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import com.siliconlabs.bledemo.Bluetooth.Parsing.Engine
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.utils.BLEUtils
import com.siliconlabs.bledemo.utils.Notifications
import java.util.*

class RemoteServicesFragment : ServicesFragment(isRemote = true) {

    override val services: List<BluetoothGattService> by lazy {
        bluetoothGatt?.services.orEmpty()
    }

    override fun getEngineService(
        uuid: UUID?,
        service: BluetoothGattService
    ) = Engine.getService(uuid)?.let { bluetoothGatt?.getService(uuid) } ?: service

    override fun getEngineCharacteristic(
        uuid: UUID?,
        service: BluetoothGattService,
        characteristic: BluetoothGattCharacteristic
    ) = Engine.getCharacteristic(uuid)?.let { service.getCharacteristic(uuid) } ?: characteristic

    override fun readCharacteristic(bluetoothGattCharacteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.readCharacteristic(bluetoothGattCharacteristic)
    }

    override fun readDescriptor(descriptor: BluetoothGattDescriptor) {
        bluetoothGatt?.readDescriptor(descriptor)
    }

    fun enableNotifications(
        notificationIcon: ImageView?,
        id: Int,
        characteristicExpansion: LinearLayout,
        notificationText: TextView?,
        bluetoothGattCharacteristic: BluetoothGattCharacteristic,
        service: BluetoothGattService,
        serviceName: String,
        indicationIcon: ImageView?,
        indicationText: TextView?
    ) {
        notificationIcon?.startAnimation(
                AnimationUtils.loadAnimation(requireContext(), R.anim.property_image_click)
        )

        if (characteristicFragments.containsKey(id)) {
            currentWriteReadFragment = characteristicFragments[id]
            if (characteristicExpansion.visibility == View.GONE &&
                notificationText?.currentTextColor ==
                ContextCompat.getColor(requireContext(), R.color.silabs_inactive)
            ) {
                characteristicExpansion.visibility = View.VISIBLE
            }
        } else {
            currentWriteReadFragment = initFragmentCharacteristicDetail(
                bluetoothGattCharacteristic,
                id,
                service,
                characteristicExpansion
            )
            characteristicFragments[id] = currentWriteReadFragment
        }
        setNotifyProperty(
            bluetoothGattCharacteristic,
            serviceName,
            notificationIcon,
            notificationText,
            indicationIcon,
            indicationText
        )
    }

    fun enableIndications(
            indicationIcon: ImageView?,
            id: Int,
            characteristicExpansion: LinearLayout,
            indicationText: TextView?,
            bluetoothGattCharacteristic: BluetoothGattCharacteristic,
            service: BluetoothGattService,
            serviceName: String,
            notificationText: TextView?,
            notificationIcon: ImageView?
    ) {
        indicationIcon?.startAnimation(
                AnimationUtils.loadAnimation(requireContext(), R.anim.property_image_click)
        )
        if (characteristicFragments.containsKey(id)) {
            currentWriteReadFragment = characteristicFragments[id]
            if (characteristicExpansion.visibility == View.GONE
                    && indicationText?.currentTextColor ==
                    ContextCompat.getColor(requireContext(), R.color.silabs_inactive)
            ) {
                characteristicExpansion.visibility = View.VISIBLE
            }
        } else {
            currentWriteReadFragment = initFragmentCharacteristicDetail(
                    bluetoothGattCharacteristic,
                    id,
                    service,
                    characteristicExpansion
            )
            characteristicFragments[id] = currentWriteReadFragment
        }
        setIndicateProperty(
                bluetoothGattCharacteristic,
                serviceName,
                indicationIcon,
                indicationText,
                notificationIcon,
                notificationText
        )
    }

    private fun setNotifyProperty(
        bluetoothGattCharacteristic: BluetoothGattCharacteristic,
        serviceName: String,
        notifyPropertyIcon: ImageView?,
        notifyPropertyName: TextView?,
        indicationIcon: ImageView?,
        indicationText: TextView?
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

        currentWriteReadFragment?.notificationsEnabled = notificationsEnabled
        notifyPropertyIcon?.setBackgroundResource(
                if (notificationsEnabled) R.drawable.ic_notify_on else R.drawable.ic_notify_off)
        notifyPropertyName?.setTextColor(ContextCompat.getColor(
                requireContext(),
                if (notificationsEnabled) R.color.silabs_blue else R.color.silabs_inactive
            )
        )

        val characteristicUuid = getUuidFromBluetoothGattCharacteristic(bluetoothGattCharacteristic)
        serviceItemContainers[serviceName]?.setCharacteristicNotificationState(
            characteristicUuid,
            if (notificationsEnabled) Notifications.NOTIFY else Notifications.DISABLED
        )

        currentWriteReadFragment?.indicationsEnabled = false
        indicationIcon?.setBackgroundResource(R.drawable.ic_indicate_off)
        indicationText?.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.silabs_inactive)
        )
    }

    private fun setIndicateProperty(
            bluetoothGattCharacteristic: BluetoothGattCharacteristic,
            serviceName: String,
            indicatePropertyIcon: ImageView?,
            indicatePropertyName: TextView?,
            notificationIcon: ImageView?,
            notificationText: TextView?
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

        currentWriteReadFragment?.indicationsEnabled = indicationsEnabled
        indicatePropertyIcon?.setBackgroundResource(
                if (indicationsEnabled) R.drawable.ic_indicate_on else R.drawable.ic_indicate_off
        )
        indicatePropertyName?.setTextColor(ContextCompat.getColor(
                requireContext(),
                if (indicationsEnabled) R.color.silabs_blue else R.color.silabs_inactive
        ))

        val characteristicUuid = getUuidFromBluetoothGattCharacteristic(bluetoothGattCharacteristic)
        serviceItemContainers[serviceName]?.setCharacteristicNotificationState(
                characteristicUuid,
                if (indicationsEnabled) Notifications.INDICATE else Notifications.DISABLED
        )

        currentWriteReadFragment?.notificationsEnabled = false
        notificationIcon?.setBackgroundResource(R.drawable.ic_notify_off)
        notificationText?.setTextColor(ContextCompat.getColor(requireContext(), R.color.silabs_inactive))
    }
}
