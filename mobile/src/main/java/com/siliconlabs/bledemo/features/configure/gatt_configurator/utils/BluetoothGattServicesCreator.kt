package com.siliconlabs.bledemo.features.configure.gatt_configurator.utils

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import com.siliconlabs.bledemo.features.configure.gatt_configurator.models.Characteristic
import com.siliconlabs.bledemo.features.configure.gatt_configurator.models.GattServer
import com.siliconlabs.bledemo.features.configure.gatt_configurator.models.Service
import java.util.*

class BluetoothGattServicesCreator {
    companion object {
        fun getBluetoothGattServices(gattServer: GattServer): LinkedList<BluetoothGattService> {
            val services = LinkedList<BluetoothGattService>()

            for (service in gattServer.services) {
                services.add(getBluetoothGattService(service))
            }

            return services
        }

        private fun getBluetoothGattService(service: Service): BluetoothGattService {
            val bluetoothGattService = BluetoothGattService(service.uuid?.getAs128BitUuid(), service.type.getBluetoothGattServiceType())

            for (characteristic in service.characteristics) {
                bluetoothGattService.addCharacteristic(getBluetoothGattCharacteristic(characteristic))
            }

            return bluetoothGattService
        }

        private fun getBluetoothGattCharacteristic(characteristic: Characteristic): BluetoothGattCharacteristic {
            val bluetoothGattCharacteristic = BluetoothGattCharacteristic(characteristic.uuid?.getAs128BitUuid(), characteristic.getBluetoothGattProperties(), characteristic.getBluetoothGattPermissions())
            bluetoothGattCharacteristic.value = characteristic.value?.getValueAsArrayOfBytes()

            for (descriptor in characteristic.descriptors) {
                val bluetoothGattDescriptor = BluetoothGattDescriptor(descriptor.uuid?.getAs128BitUuid(), descriptor.getBluetoothGattPermissions())
                bluetoothGattDescriptor.value = descriptor.value?.value?.toByteArray()
                bluetoothGattCharacteristic.addDescriptor(bluetoothGattDescriptor)
            }

            return bluetoothGattCharacteristic
        }
    }
}