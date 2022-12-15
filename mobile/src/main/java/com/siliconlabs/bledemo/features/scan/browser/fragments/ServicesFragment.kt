package com.siliconlabs.bledemo.features.scan.browser.fragments

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.siliconlabs.bledemo.bluetooth.parsing.DescriptorParser
import com.siliconlabs.bledemo.features.scan.browser.activities.DeviceServicesActivity
import com.siliconlabs.bledemo.features.scan.browser.dialogs.DictionaryEntryEditDialog
import com.siliconlabs.bledemo.features.scan.browser.adapters.MappingCallback
import com.siliconlabs.bledemo.features.scan.browser.models.Mapping
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.features.scan.browser.views.CharacteristicItemContainer
import com.siliconlabs.bledemo.features.scan.browser.views.DescriptorContainer
import com.siliconlabs.bledemo.features.scan.browser.views.ServiceItemContainer
import com.siliconlabs.bledemo.databinding.FragmentServicesBinding
import com.siliconlabs.bledemo.features.configure.gatt_configurator.models.Property
import com.siliconlabs.bledemo.features.iop_test.models.CommonUUID
import com.siliconlabs.bledemo.utils.*
import kotlinx.android.synthetic.main.descriptor_container.view.*
import java.util.*

abstract class ServicesFragment(private val isRemote: Boolean) : Fragment(R.layout.fragment_services) {

    private lateinit var _binding: FragmentServicesBinding

    private lateinit var sharedPrefUtils: SharedPrefUtils
    private lateinit var characteristicNamesMap: HashMap<String, Mapping>
    private lateinit var serviceNamesMap: HashMap<String, Mapping>

    protected val bluetoothGatt get() = (activity as? DeviceServicesActivity)?.bluetoothGatt // prevent from crashes when clicking "back" when still loading

    private val characteristicFragments = mutableMapOf<Int, FragmentCharacteristicDetail?>()
    protected var currentWriteReadFragment: FragmentCharacteristicDetail? = null
    private val descriptorsMap = mutableMapOf<BluetoothGattDescriptor, View>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        _binding = FragmentServicesBinding.inflate(inflater)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPrefUtils = SharedPrefUtils(requireContext())
        characteristicNamesMap = sharedPrefUtils.characteristicNamesMap
        serviceNamesMap = sharedPrefUtils.serviceNamesMap
        setupSwipeRefreshLayout()
    }

    private fun setupSwipeRefreshLayout() {
        _binding.swipeRefreshContainer.apply {
            setOnRefreshListener {
                this.isRefreshing = false
                (activity as DeviceServicesActivity).refreshServices()
            }
            setColorSchemeColors(
                    ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark),
                    ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark),
                    ContextCompat.getColor(requireContext(), android.R.color.holo_orange_light),
                    ContextCompat.getColor(requireContext(), android.R.color.holo_red_light)
            )
        }
    }

    fun init(services: List<BluetoothGattService>) {
        clear()
        setServicesList(services)
        initServicesViews()
    }

    abstract fun setServicesList(services: List<BluetoothGattService>)

    fun clear() {
        _binding.servicesContainer.removeAllViews()
    }

    fun updateDescriptorView(descriptor: BluetoothGattDescriptor) {
        descriptorsMap[descriptor]?.apply {
            activity?.runOnUiThread {
                container_descriptor_value.visibility = View.VISIBLE
                tv_value.text = DescriptorParser(descriptor).getFormattedValue()
            }
        }
    }

    fun updateCharacteristicView(characteristic: BluetoothGattCharacteristic) {
        characteristicFragments.values
            .find { it != null && it.mBluetoothCharact?.uuid == characteristic.uuid }
            ?.onActionDataAvailable(characteristic.uuid.toString(), true)
    }

    fun updateCurrentCharacteristicView(uuid: UUID) {
        currentWriteReadFragment?.onActionDataAvailable(uuid.toString(), false)
    }

    fun updateCurrentCharacteristicView(uuid: UUID, status: Int) {
        currentWriteReadFragment?.onActionDataWrite(uuid.toString(), status)
    }

    protected abstract var services: List<BluetoothGattService>

    private val serviceContainerCallback = object : ServiceItemContainer.Callback {
        override fun onRenameClicked(container: ServiceItemContainer) {
            DictionaryEntryEditDialog(
                container.getServiceName(),
                UuidUtils.getUuidText(container.service.uuid),
                Mapping.Type.SERVICE,
                object : MappingCallback {
                    override fun onNameChanged(mapping: Mapping) {
                        container.setServiceName(mapping.name)
                        serviceNamesMap[mapping.uuid] = mapping
                    }
                }
            ).show(parentFragmentManager, "dialog_mappings_edit")
        }
    }

    private val characteristicContainerCallback = object : CharacteristicItemContainer.Callback {
        override fun onRenameClicked(container: CharacteristicItemContainer) {
           DictionaryEntryEditDialog(
                   container.getCharacteristicName(),
                   UuidUtils.getUuidText(container.characteristic.uuid),
                   Mapping.Type.CHARACTERISTIC,
                   object : MappingCallback {
                       override fun onNameChanged(mapping: Mapping) {
                           container.setCharacteristicName(mapping.name)
                           characteristicNamesMap[mapping.uuid] = mapping
                       }
                   }
           ).show(parentFragmentManager, "dialog_mapping_edit")
        }

        override fun onPropertyClicked(property: Property, characteristicContainer: CharacteristicItemContainer) {
            handleOnPropertyClicked(property, characteristicContainer)
        }
    }

    private val descriptorContainerCallback = object : DescriptorContainer.Callback {
        override fun onReadPropertyClicked(descriptor: BluetoothGattDescriptor) {
            readDescriptor(descriptor)
        }
    }

    private fun initServicesViews() {
        context?.let {
            services.forEachIndexed { position, service ->
                val serviceItemContainer = ServiceItemContainer(
                        it,
                        serviceContainerCallback,
                        service,
                        isMandatorySystemService(service.uuid),
                        serviceNamesMap
                )

                service.characteristics.forEach { char ->
                    val characteristicContainer = CharacteristicItemContainer(
                            it,
                            characteristicContainerCallback,
                            char,
                            isMandatorySystemService(service.uuid),
                            characteristicNamesMap
                    )

                    if (char.descriptors.isEmpty()) characteristicContainer.hideDescriptorsContainer()
                    else {
                        char.descriptors.forEach { descriptor ->
                            DescriptorContainer(
                                    it,
                                    descriptorContainerCallback,
                                    descriptor
                            ).also {
                                characteristicContainer.addDescriptorContainer(it)
                                descriptorsMap[descriptor] = it
                            }
                        }
                    }
                    serviceItemContainer.addCharacteristicContainer(characteristicContainer)
                }

                serviceItemContainer.setMargins(position)
                _binding.servicesContainer.addView(
                        serviceItemContainer,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            (activity as? DeviceServicesActivity)?.isUiCreated = true // prevent from crashes when clicking "back" when still loading
        }

    }

    private  fun initFragmentCharacteristicDetail(
            bluetoothGattCharacteristic: BluetoothGattCharacteristic,
            expansionId: Int,
            service: BluetoothGattService,
            characteristicExpansion: LinearLayout,
            displayWriteDialog: Boolean = false,
            writeType: FragmentCharacteristicDetail.WriteType = FragmentCharacteristicDetail.WriteType.REMOTE_WRITE
    ): FragmentCharacteristicDetail {
        val characteristicDetail = FragmentCharacteristicDetail().apply {
            isRemote = this@ServicesFragment.isRemote
            setmService(service)
            setmBluetoothCharact(bluetoothGattCharacteristic)
            this.displayWriteDialog = displayWriteDialog
            characteristicExpansion.visibility = View.VISIBLE
            this.writeType = writeType
        }
        // show characteristic's expansion and add the fragment to view/edit characteristic detail
        parentFragmentManager
                .beginTransaction()
                .add(expansionId, characteristicDetail, CHARACTERISTIC_ADD_FRAGMENT_TRANSACTION_ID)
                .commitNow()

        return characteristicDetail
    }


    private fun handleOnPropertyClicked(
            property: Property,
            characteristicContainer: CharacteristicItemContainer
    ) {
        if (isMandatorySystemService(characteristicContainer.characteristic.service.uuid)) return

        val bluetoothGattCharacteristic = characteristicContainer.characteristic
        val service = bluetoothGattCharacteristic.service
        val characteristicExpansion = characteristicContainer.characteristicExpansion

        when (property) {
            Property.READ -> {
                saveCurrentWriteReadFragment(
                        bluetoothGattCharacteristic,
                        service,
                        characteristicExpansion
                )
                readCharacteristic(bluetoothGattCharacteristic)
            }
            Property.WRITE -> {
                val writeType =
                        if (this is RemoteServicesFragment) FragmentCharacteristicDetail.WriteType.REMOTE_WRITE
                        else FragmentCharacteristicDetail.WriteType.LOCAL_WRITE
                openWriteDialog(
                    bluetoothGattCharacteristic,
                    service,
                    characteristicExpansion,
                    writeType
                )
            }
            Property.NOTIFY -> {
                if (this is RemoteServicesFragment) {
                    saveCurrentWriteReadFragment(
                            bluetoothGattCharacteristic,
                            service,
                            characteristicExpansion
                    )
                    toggleNotifications(
                        bluetoothGattCharacteristic,
                        characteristicContainer.getPropertyBinding(Property.NOTIFY),
                        characteristicContainer.getPropertyBinding(Property.INDICATE)
                    )
                }
                else {
                    openWriteDialog(
                        bluetoothGattCharacteristic,
                        service,
                        characteristicExpansion,
                        FragmentCharacteristicDetail.WriteType.LOCAL_NOTIFY
                    )
                }
            }
            Property.INDICATE -> {
                if (this is RemoteServicesFragment) {
                    saveCurrentWriteReadFragment(
                            bluetoothGattCharacteristic,
                            service,
                            characteristicExpansion
                    )
                    toggleIndications(
                        bluetoothGattCharacteristic,
                        characteristicContainer.getPropertyBinding(Property.INDICATE),
                        characteristicContainer.getPropertyBinding(Property.NOTIFY)
                    )
                }
                else {
                    openWriteDialog(
                        bluetoothGattCharacteristic,
                        service,
                        characteristicExpansion,
                        FragmentCharacteristicDetail.WriteType.LOCAL_INDICATE
                    )
                }
            }
            else -> { }
        }
    }

    private fun openWriteDialog(
        characteristic: BluetoothGattCharacteristic,
        service: BluetoothGattService,
        characteristicExpansion: LinearLayout,
        writeType: FragmentCharacteristicDetail.WriteType
    ) {
        val id = characteristicExpansion.id

        if (characteristicFragments.containsKey(id)) {
            currentWriteReadFragment = characteristicFragments[id]
            characteristicFragments[id]?.createWriteDialog(writeType)
        } else {
            currentWriteReadFragment = initFragmentCharacteristicDetail(
                characteristic,
                id,
                service,
                characteristicExpansion,
                displayWriteDialog = true,
                writeType = writeType
            )
            characteristicFragments[id] = currentWriteReadFragment
        }
        characteristicExpansion.visibility = View.VISIBLE
    }

    private fun saveCurrentWriteReadFragment(
            characteristic: BluetoothGattCharacteristic,
            service: BluetoothGattService,
            characteristicExpansion: LinearLayout
    ) {
        val id = characteristicExpansion.id

        if (characteristicFragments.containsKey(id)) {
            currentWriteReadFragment = characteristicFragments[id]
        } else {
            currentWriteReadFragment = initFragmentCharacteristicDetail(
                    characteristic,
                    id,
                    service,
                    characteristicExpansion
            )
            characteristicFragments[id] = currentWriteReadFragment
        }
    }

    protected abstract fun readCharacteristic(bluetoothGattCharacteristic: BluetoothGattCharacteristic)

    abstract fun readDescriptor(descriptor: BluetoothGattDescriptor)

    private fun isMandatorySystemService(uuid: UUID): Boolean {
        return (!isRemote &&
                (uuid == UUID.fromString(CommonUUID.Service.UUID_GENERIC_ACCESS.toString())
                || uuid == UUID.fromString(CommonUUID.Service.UUID_GENERIC_ATTRIBUTE.toString())))
    }

    override fun onPause() {
        sharedPrefUtils.saveCharacteristicNamesMap(characteristicNamesMap)
        sharedPrefUtils.saveServiceNamesMap(serviceNamesMap)
        super.onPause()
    }

    companion object {
        private const val CHARACTERISTIC_ADD_FRAGMENT_TRANSACTION_ID = "characteristicdetail"
    }
}
