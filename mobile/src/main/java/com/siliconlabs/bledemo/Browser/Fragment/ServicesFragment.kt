package com.siliconlabs.bledemo.Browser.Fragment

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.siliconlabs.bledemo.Bluetooth.Parsing.Common
import com.siliconlabs.bledemo.Bluetooth.Parsing.DescriptorParser
import com.siliconlabs.bledemo.Bluetooth.Parsing.Engine
import com.siliconlabs.bledemo.Browser.Activities.DeviceServicesActivity
import com.siliconlabs.bledemo.Browser.Dialogs.MappingsEditDialog
import com.siliconlabs.bledemo.Browser.MappingCallback
import com.siliconlabs.bledemo.Browser.Models.Mapping
import com.siliconlabs.bledemo.Browser.Models.MappingType
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.utils.*
import com.siliconlabs.bledemo.Views.CharacteristicItemContainer
import com.siliconlabs.bledemo.Views.ServiceItemContainer
import com.siliconlabs.bledemo.utils.Notifications
import com.siliconlabs.bledemo.iop_test.models.CommonUUID
import java.util.*

abstract class ServicesFragment(private val isRemote: Boolean) : Fragment(R.layout.fragment_services) {
    private lateinit var sharedPrefUtils: SharedPrefUtils
    private lateinit var characteristicNamesMap: HashMap<String, Mapping>
    private lateinit var serviceNamesMap: HashMap<String, Mapping>

    protected val bluetoothGatt get() = (activity as DeviceServicesActivity).bluetoothGatt

    protected var serviceItemContainers = mutableMapOf<String, ServiceItemContainer>()
    private lateinit var servicesContainerView: ViewGroup
    protected val characteristicFragments = mutableMapOf<Int, FragmentCharacteristicDetail?>()
    protected var currentWriteReadFragment: FragmentCharacteristicDetail? = null
    private val descriptorsMap = mutableMapOf<BluetoothGattDescriptor, View>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        servicesContainerView = view.findViewById(R.id.services_container)

        sharedPrefUtils = SharedPrefUtils(requireContext())
        characteristicNamesMap = sharedPrefUtils.characteristicNamesMap
        serviceNamesMap = sharedPrefUtils.serviceNamesMap
    }

    fun init() {
        clear()
        initServicesViews()
    }

    fun clear() {
        servicesContainerView.removeAllViews()
    }

    fun updateDescriptorView(descriptor: BluetoothGattDescriptor) {
        descriptorsMap[descriptor]?.apply {
            activity?.runOnUiThread {
                findViewById<LinearLayout>(R.id.ll_value).visibility = View.VISIBLE
                findViewById<TextView>(R.id.tv_value).text =
                    DescriptorParser(descriptor).getFormattedValue()
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

    protected abstract val services: List<BluetoothGattService>

    private fun initServicesViews() {
        // iterate through all of the services for the device, inflate and add views to the scrollview
        services.forEachIndexed forEach@{ position, service ->
            val serviceItemContainer = ServiceItemContainer(requireContext())

            val engineService = getEngineService(service.uuid, service)
            val systemMandatoryService = isMandatorySystemServices(service.uuid)
            var serviceNameText = Common.getServiceName(engineService.uuid, requireContext())
            if (systemMandatoryService) {
                serviceNameText += " (System)"
            }
            val serviceUuidText = UuidUtils.getUuidText(engineService.uuid)

            populateServiceContainerWithValues(serviceItemContainer, position, serviceNameText, serviceUuidText)

            val characteristics = engineService.characteristics.orEmpty()
            if (characteristics.isEmpty()) {
                serviceItemContainer.cvServiceInfo.setBackgroundColor(Color.LTGRAY)
                return@forEach
            }
            // iterate through the characteristics of this service to initialize views
            for (bluetoothGattCharacteristic in characteristics) {
                // retrieve relevant bluetooth data for characteristic of service
                // the engine parses through the data of the btgattcharac and returns a wrapper characteristic
                // the wrapper characteristic is matched with accepted bt gatt profiles, provides field types/values/units
                val engineCharacteristic = getEngineCharacteristic(bluetoothGattCharacteristic.uuid, engineService, bluetoothGattCharacteristic)
                val characteristicNameText = Common.getCharacteristicName(engineCharacteristic.uuid, requireContext())
                val characteristicUuidText = UuidUtils.getUuidText(engineCharacteristic.uuid)

                val characteristicContainer = CharacteristicItemContainer(requireContext())

                loadCharacteristicDescriptors(
                    bluetoothGattCharacteristic,
                    characteristicContainer.descriptorsLabel,
                    characteristicContainer.descriptorContainer
                )

                populateCharacteristicContainerWithValues(characteristicContainer, characteristicNameText, characteristicUuidText)

                // hide divider between characteristics if last characteristic of service
                if (serviceItemContainer.llGroupOfCharacteristicsForService.childCount == characteristics.size - 1) {
                    characteristicContainer.characteristicSeparator.visibility = View.GONE
                    serviceItemContainer.llLastItemDivider.visibility = View.VISIBLE
                }
                serviceItemContainer.llGroupOfCharacteristicsForService.addView(
                    characteristicContainer
                )

                // add properties to characteristic list item in expansion
                addPropertiesToCharacteristic(bluetoothGattCharacteristic, characteristicContainer.propsContainer)
                if (!systemMandatoryService) {
                    setPropertyClickListeners(
                            characteristicContainer.propsContainer,
                            bluetoothGattCharacteristic,
                            engineService,
                            serviceNameText,
                            characteristicContainer.characteristicExpansion
                    )
                }
                serviceItemContainer.setCharacteristicNotificationState(
                    characteristicUuidText,
                    Notifications.DISABLED
                )
                characteristicContainer.setOnClickListener {
                    if (characteristicContainer.characteristicExpansion.visibility == View.VISIBLE) {
                        characteristicContainer.characteristicExpansion.visibility = View.GONE
                    } else {
                        characteristicContainer.characteristicExpansion.visibility = View.VISIBLE
                        if (characteristicFragments.containsKey(id)) {
                            currentWriteReadFragment = characteristicFragments[id]
                        } else {
                            currentWriteReadFragment = initFragmentCharacteristicDetail(
                                bluetoothGattCharacteristic,
                                id,
                                service,
                                characteristicContainer.characteristicExpansion,
                                false
                            )
                            characteristicFragments[id] = currentWriteReadFragment
                        }
                    }
                }
            }

            serviceItemContainer.setMargins(position, services.lastIndex)
            servicesContainerView.addView(
                serviceItemContainer,
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            serviceItemContainers[serviceNameText] = serviceItemContainer
        }
    }

    protected abstract fun getEngineService(
        uuid: UUID?,
        service: BluetoothGattService
    ): BluetoothGattService

    protected abstract fun getEngineCharacteristic(
        uuid: UUID?,
        service: BluetoothGattService,
        characteristic: BluetoothGattCharacteristic
    ): BluetoothGattCharacteristic

    private fun ServiceItemContainer.setMargins(position: Int, lastIndex: Int) {
        val outerMargin = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            16f,
            resources.displayMetrics
        ).toInt()
        val innerMargin = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            8f,
            resources.displayMetrics
        ).toInt()

        cvServiceInfo.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            when (position) {
                0 -> setMargins(outerMargin, outerMargin, outerMargin, innerMargin)
                lastIndex -> setMargins(outerMargin, innerMargin, outerMargin, outerMargin)
                else -> setMargins(outerMargin, innerMargin, outerMargin, innerMargin)
            }
        }
    }

    protected fun initFragmentCharacteristicDetail(
            bluetoothGattCharacteristic: BluetoothGattCharacteristic,
            expansionId: Int,
            service: BluetoothGattService,
            characteristicExpansion: LinearLayout,
            displayWriteDialog: Boolean = false,
            writeType: FragmentCharacteristicDetail.WriteType = FragmentCharacteristicDetail.WriteType.REMOTE_WRITE
    ): FragmentCharacteristicDetail {
        val characteristicDetail = FragmentCharacteristicDetail().apply {
            isRemote = this@ServicesFragment.isRemote
            address = bluetoothGatt?.device?.address
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

    private fun addPropertiesToCharacteristic(
        bluetoothGattCharacteristic: BluetoothGattCharacteristic,
        propsContainer: LinearLayout
    ) {
        var propsExploded: Array<String?> =
            Common.getProperties(requireContext(), bluetoothGattCharacteristic.properties)
                .split(",")
                .toTypedArray()

        if (
            propsExploded.any { it?.contains("write no response", ignoreCase = true) == true }
        ) {
            val temp = ArrayList<String?>()
            var writeAdded = false
            for (s: String? in propsExploded) {
                if (
                    s?.contains("write no response", ignoreCase = true)!! && !writeAdded
                ) {
                    temp.add("Write")
                    writeAdded = true
                } else if (!s.toLowerCase(Locale.getDefault()).contains("write")) {
                    temp.add(s)
                }
            }
            propsExploded = arrayOfNulls(temp.size)
            for (i in temp.indices) {
                propsExploded[i] = temp[i]
            }
        }
        for (propertyValue: String? in propsExploded) {
            val propertyView = TextView(requireContext())
            var propertyValueTrimmed: String? = propertyValue?.trim { it <= ' ' }
            propertyValueTrimmed =
                if (propertyValue?.length!! > 13) propertyValue.substring(0, 13)
                else propertyValueTrimmed
            propertyView.text = propertyValueTrimmed
            propertyView.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.silabs_white)
            )
            propertyView.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                resources.getDimension(R.dimen.characteristic_property_text_size)
            )
            propertyView.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.silabs_inactive)
            )
            propertyView.typeface = Typeface.DEFAULT_BOLD
            propertyView.gravity = Gravity.CENTER_VERTICAL
            val propertyContainer = LinearLayout(requireContext())
            propertyContainer.orientation = LinearLayout.HORIZONTAL
            val propertyIcon = ImageView(requireContext())

            @DrawableRes val iconRes: Int =
                when (propertyValue.trim(' ').toUpperCase(Locale.getDefault())) {
                    Common.PROPERTY_VALUE_BROADCAST -> R.drawable.ic_debug_prop_broadcast
                    Common.PROPERTY_VALUE_READ -> R.drawable.ic_read_off
                    Common.PROPERTY_VALUE_WRITE -> R.drawable.ic_edit_off
                    Common.PROPERTY_VALUE_NOTIFY -> R.drawable.ic_notify_off
                    Common.PROPERTY_VALUE_INDICATE -> R.drawable.ic_indicate_off
                    Common.PROPERTY_VALUE_SIGNED_WRITE -> R.drawable.ic_debug_prop_signed_write
                    Common.PROPERTY_VALUE_EXTENDED_PROPS -> R.drawable.ic_debug_prop_ext
                    else -> R.drawable.ic_debug_prop_ext
                }

            propertyIcon.setBackgroundResource(iconRes)
            propertyIcon.tag = PROPERTY_ICON_TAG
            propertyView.tag = PROPERTY_NAME_TAG
            val paramsText = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            paramsText.gravity = Gravity.CENTER_VERTICAL

            val paramsIcon = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val d = resources.displayMetrics.density
            paramsIcon.marginEnd = (8 * d).toInt()
            paramsIcon.gravity = Gravity.CENTER_VERTICAL
            propertyContainer.addView(propertyIcon, paramsIcon)
            propertyContainer.addView(propertyView, paramsText)
            propertyContainer.tag = propertyValue
            val paramsTextAndIconContainer = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            paramsTextAndIconContainer.setMargins(0, (4 * d).toInt(), (10 * d).toInt(), 0)
            propertyContainer.setPadding(
                (2 * d).toInt(),
                (8 * d).toInt(),
                (6 * d).toInt(),
                (6 * d).toInt()
            )
            propsContainer.addView(propertyContainer, paramsTextAndIconContainer)
        }
    }

    private fun setPropertyClickListeners(
        propsContainer: LinearLayout,
        bluetoothGattCharacteristic: BluetoothGattCharacteristic,
        service: BluetoothGattService,
        serviceName: String,
        characteristicExpansion: LinearLayout
    ) {
        val notificationIcon = getIconWithValue(propsContainer, Common.PROPERTY_VALUE_NOTIFY)
        val notificationText = getTextViewWithValue(propsContainer, Common.PROPERTY_VALUE_NOTIFY)
        val indicationIcon = getIconWithValue(propsContainer, Common.PROPERTY_VALUE_INDICATE)
        val indicationText = getTextViewWithValue(propsContainer, Common.PROPERTY_VALUE_INDICATE)
        val readIcon = getIconWithValue(propsContainer, Common.PROPERTY_VALUE_READ)
        val writeIcon = getIconWithValue(propsContainer, Common.PROPERTY_VALUE_WRITE)
        val id = characteristicExpansion.id

        for (i in 0 until propsContainer.childCount) {
            if (propsContainer.getChildAt(i).tag == null) continue

            val propertyContainer = propsContainer.getChildAt(i) as LinearLayout
            val tag = (propertyContainer.tag as String)
                .trim { it <= ' ' }
                .toUpperCase(Locale.getDefault())
            when (tag) {
                Common.PROPERTY_VALUE_READ -> propertyContainer.setOnClickListener {
                    readIcon?.startAnimation(
                        AnimationUtils.loadAnimation(requireContext(), R.anim.property_image_click)
                    )
                    if (characteristicFragments.containsKey(id)) {
                        currentWriteReadFragment = characteristicFragments[id]
                    } else {
                        currentWriteReadFragment = initFragmentCharacteristicDetail(
                            bluetoothGattCharacteristic,
                            id,
                            service,
                            characteristicExpansion
                        )
                        characteristicFragments[id] = currentWriteReadFragment
                    }
                    characteristicExpansion.visibility = View.VISIBLE
                    readCharacteristic(bluetoothGattCharacteristic)
                }
                Common.PROPERTY_VALUE_WRITE -> propertyContainer.setOnClickListener {
                    val writeType =
                            if (this is RemoteServicesFragment) FragmentCharacteristicDetail.WriteType.REMOTE_WRITE
                            else FragmentCharacteristicDetail.WriteType.LOCAL_WRITE
                    openWriteDialog(
                        writeIcon,
                        id,
                        bluetoothGattCharacteristic,
                        service,
                        characteristicExpansion,
                        writeType
                    )
                }
                Common.PROPERTY_VALUE_NOTIFY -> propertyContainer.setOnClickListener {
                    if (this is RemoteServicesFragment) {
                        enableNotifications(
                            notificationIcon,
                            id,
                            characteristicExpansion,
                            notificationText,
                            bluetoothGattCharacteristic,
                            service,
                            serviceName,
                            indicationIcon,
                            indicationText
                        )
                    }
                    else {
                        openWriteDialog(
                            notificationIcon,
                            id,
                            bluetoothGattCharacteristic,
                            service,
                            characteristicExpansion,
                            FragmentCharacteristicDetail.WriteType.LOCAL_NOTIFY
                        )
                    }
                }
                Common.PROPERTY_VALUE_INDICATE -> propertyContainer.setOnClickListener {
                    if (this is RemoteServicesFragment) {
                        enableIndications(
                            indicationIcon,
                            id,
                            characteristicExpansion,
                            indicationText,
                            bluetoothGattCharacteristic,
                            service,
                            serviceName,
                            notificationText,
                            notificationIcon
                        )
                    }
                    else {
                        openWriteDialog(
                            indicationIcon,
                            id,
                            bluetoothGattCharacteristic,
                            service,
                            characteristicExpansion,
                            FragmentCharacteristicDetail.WriteType.LOCAL_INDICATE
                        )
                    }
                }
                else -> {
                }
            }
        }
    }

    private fun openWriteDialog(
        icon: ImageView?,
        id: Int,
        characteristic: BluetoothGattCharacteristic,
        service: BluetoothGattService,
        characteristicExpansion: LinearLayout,
        writeType: FragmentCharacteristicDetail.WriteType
    ) {
        icon?.startAnimation(
            AnimationUtils.loadAnimation(requireContext(), R.anim.property_image_click)
        )
        if (characteristicFragments.containsKey(id)) {
            currentWriteReadFragment = characteristicFragments[id]
            characteristicFragments[id]?.showCharacteristicWriteDialog(writeType)
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

    protected abstract fun readCharacteristic(bluetoothGattCharacteristic: BluetoothGattCharacteristic)

    protected fun getUuidFromBluetoothGattCharacteristic(bluetoothGattCharacteristic: BluetoothGattCharacteristic): String {
        val characteristic = Engine.getCharacteristic(bluetoothGattCharacteristic.uuid)
        return (characteristic?.uuid ?: bluetoothGattCharacteristic.uuid)
            .let { UuidUtils.getUuidText(it) }
    }



    private fun getTextViewWithValue(propsContainer: LinearLayout, value: String): TextView? {
        for (i in 0 until propsContainer.childCount) {
            if (propsContainer.getChildAt(i).tag == null) {
                continue
            }
            val propertyContainer = propsContainer.getChildAt(i) as LinearLayout
            for (j in 0 until propertyContainer.childCount) {
                val view = propertyContainer.getChildAt(j)
                if (view.tag != null && (view.tag == PROPERTY_NAME_TAG)) {
                    val propertyValue = (propertyContainer.tag as String).trim { it <= ' ' }
                        .toUpperCase(Locale.getDefault())
                    if ((propertyValue == value)) {
                        return view as TextView
                    }
                }
            }
        }
        return null
    }

    private fun getIconWithValue(propsContainer: LinearLayout, value: String): ImageView? {
        for (i in 0 until propsContainer.childCount) {
            if (propsContainer.getChildAt(i).tag == null) {
                continue
            }
            val propertyContainer = propsContainer.getChildAt(i) as LinearLayout
            for (j in 0 until propertyContainer.childCount) {
                val view = propertyContainer.getChildAt(j)
                if (view.tag != null && (view.tag == PROPERTY_ICON_TAG)) {
                    val propertyValue = (propertyContainer.tag as String).trim { it <= ' ' }
                        .toUpperCase(Locale.getDefault())
                    if ((propertyValue == value)) {
                        return view as ImageView
                    }
                }
            }
        }
        return null
    }

    private fun loadCharacteristicDescriptors(
        bluetoothGattCharacteristic: BluetoothGattCharacteristic,
        descriptorsLabelTextView: TextView,
        descriptorLinearLayout: LinearLayout
    ) {
        if (bluetoothGattCharacteristic.descriptors.size <= 0) {
            descriptorsLabelTextView.visibility = View.GONE
        } else {
            for (bgd: BluetoothGattDescriptor in bluetoothGattCharacteristic.descriptors) {
                val descriptor = Engine.getDescriptorByUUID(bgd.uuid)
                val view = View.inflate(requireContext(), R.layout.descriptor_view, null)

                val nameTV = view.findViewById<TextView>(R.id.tv_name)
                nameTV.text = descriptor?.name ?: "Unknown"

                val uuidTV = view.findViewById<TextView>(R.id.tv_uuid)
                uuidTV.text = descriptor?.uuid?.let { UuidUtils.getUuidText(it) } ?: "Unknown"

                val readIV = view.findViewById<ImageView>(R.id.iv_read)
                view.setOnClickListener {
                    readIV.startAnimation(
                        AnimationUtils.loadAnimation(
                            requireContext(),
                            R.anim.property_image_click
                        )
                    )
                    readDescriptor(bgd)
                }

                descriptorLinearLayout.addView(view)
                descriptorsMap[bgd] = view
            }
        }
    }

    abstract fun readDescriptor(descriptor: BluetoothGattDescriptor)

    private fun populateServiceContainerWithValues(
        serviceItemContainer: ServiceItemContainer,
        position: Int,
        serviceName: String,
        serviceUuid: String
    ) {
        if (position == 0) {
            (activity as DeviceServicesActivity).UICreated = true
            Constants.ota_button?.isVisible =
                bluetoothGatt?.services?.contains(bluetoothGatt?.getService(UuidConsts.OTA_SERVICE))!!
        }
        serviceItemContainer.llGroupOfCharacteristicsForService.visibility = View.GONE
        serviceItemContainer.llGroupOfCharacteristicsForService.removeAllViews()
        serviceItemContainer.tvServiceTitle.text = serviceName
        serviceItemContainer.tvServiceUuid.text = serviceUuid
        if (serviceName == getString(R.string.unknown_service)) {
            serviceItemContainer.ivEditServiceName.visibility = View.VISIBLE
            serviceItemContainer.llServiceEditName.setOnClickListener {
                val dialog: DialogFragment =
                    MappingsEditDialog(
                        serviceItemContainer.tvServiceTitle.text.toString(),
                        serviceItemContainer.tvServiceUuid.text.toString(),
                        object : MappingCallback {
                            override fun onNameChanged(mapping: Mapping) {
                                serviceItemContainer.tvServiceTitle.text = mapping.name
                                serviceNamesMap[mapping.uuid] = mapping
                            }
                        }, MappingType.SERVICE
                    )
                dialog.show(parentFragmentManager, "dialog_mappings_edit")
            }

            if (serviceNamesMap.containsKey(serviceUuid)) {
                serviceItemContainer.tvServiceTitle.text = serviceNamesMap[serviceUuid]?.name
            }
        }
    }

    private fun populateCharacteristicContainerWithValues(
            characteristicContainer: CharacteristicItemContainer,
            characteristicNameText: String,
            characteristicUuidText: String) {

        characteristicContainer.characteristicName.text = characteristicNameText
        if (characteristicNameText == getString(R.string.unknown_characteristic_label)) {
            characteristicContainer.characteristicEditNameIcon.visibility = View.VISIBLE
            characteristicContainer.characteristicEditNameLayout.setOnClickListener {
                MappingsEditDialog(
                        characteristicContainer.characteristicName.text.toString(),
                        characteristicUuidText,
                        object : MappingCallback {
                            override fun onNameChanged(mapping: Mapping) {
                                characteristicContainer.characteristicName.text = mapping.name
                                characteristicNamesMap[mapping.uuid] = mapping
                            }
                        }, MappingType.CHARACTERISTIC
                ).show(parentFragmentManager, "dialog_mappings_edit")
            }
            if (characteristicNamesMap.containsKey(characteristicUuidText)) {
                characteristicContainer.characteristicName.text =
                        characteristicNamesMap[characteristicUuidText]?.name
            }
        }
        characteristicContainer.characteristicUuid.text = characteristicUuidText
    }

    private fun isMandatorySystemServices(uuid: UUID): Boolean {
        return (this is LocalServicesFragment
                && (uuid == UUID.fromString(CommonUUID.Service.UUID_GENERIC_ACCESS.toString())
                || uuid == UUID.fromString(CommonUUID.Service.UUID_GENERIC_ATTRIBUTE.toString())))
    }

    override fun onPause() {
        sharedPrefUtils.saveCharacteristicNamesMap(characteristicNamesMap)
        sharedPrefUtils.saveServiceNamesMap(serviceNamesMap)
        super.onPause()
    }

    companion object {
        private const val CHARACTERISTIC_ADD_FRAGMENT_TRANSACTION_ID = "characteristicdetail"
        private const val PROPERTY_ICON_TAG = "characteristicpropertyicon"
        private const val PROPERTY_NAME_TAG = "characteristispropertyname"
    }
}
