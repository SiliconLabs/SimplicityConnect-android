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
import com.siliconlabs.bledemo.Utils.*
import com.siliconlabs.bledemo.Views.ServiceItemContainer
import java.util.*

abstract class ServicesFragment(private val isRemote: Boolean) : Fragment(R.layout.fragment_services) {
    private lateinit var sharedPrefUtils: SharedPrefUtils
    private lateinit var characteristicNamesMap: HashMap<String, Mapping>
    private lateinit var serviceNamesMap: HashMap<String, Mapping>

    protected val bluetoothGatt get() = (activity as DeviceServicesActivity).bluetoothGatt

    private var serviceItemContainers = mutableMapOf<String, ServiceItemContainer>()
    private lateinit var servicesContainerView: ViewGroup
    private val characteristicFragments = mutableMapOf<Int, FragmentCharacteristicDetail?>()
    private var currentWriteReadFragment: FragmentCharacteristicDetail? = null
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
            ?.onActionDataAvailable(characteristic.uuid.toString())
    }

    fun updateCurrentCharacteristicView(uuid: UUID) {
        currentWriteReadFragment?.onActionDataAvailable(uuid.toString())
    }

    fun updateCurrentCharacteristicView(uuid: UUID, status: Int) {
        currentWriteReadFragment?.onActionDataWrite(uuid.toString(), status)
    }

    protected abstract val services: List<BluetoothGattService>

    private fun initServicesViews() {
        // iterate through all of the services for the device, inflate and add views to the scrollview
        services.forEachIndexed forEach@{ position, service ->
            val serviceItemContainer = ServiceItemContainer(requireContext())

            // get information about service at index 'position'
            val uuid = service.uuid
            val serviceName = Common.getServiceName(uuid, requireContext())
            val serviceUuid = UuidUtils.getUuidText(uuid)

            // initialize information about services in service item container
            initServiceItemContainer(serviceItemContainer, position, serviceName, serviceUuid)

            // initialize views for each characteristic of the service, put into characteristics expansion for service's list item
            val bluetoothGattService = getEditableService(uuid, service)
            val characteristics = bluetoothGattService.characteristics.orEmpty()
            if (characteristics.isEmpty()) {
                serviceItemContainer.cvServiceInfo.setBackgroundColor(Color.LTGRAY)
                return@forEach
            }
            // iterate through the characteristics of this service
            for (bluetoothGattCharacteristic in characteristics) {
                // retrieve relevant bluetooth data for characteristic of service
                // the engine parses through the data of the btgattcharac and returns a wrapper characteristic
                // the wrapper characteristic is matched with accepted bt gatt profiles, provides field types/values/units
                val localCharacteristic =
                    Engine.getCharacteristic(bluetoothGattCharacteristic.uuid)
                val characteristicUuid = localCharacteristic?.uuid
                    ?: bluetoothGattCharacteristic.uuid
                val characteristicName = Common.getCharacteristicName(characteristicUuid, requireContext())

                val characteristicUuidText = UuidUtils.getUuidText(characteristicUuid)

                // inflate/create ui elements
                val characteristicContainer = View.inflate(
                    requireContext(),
                    R.layout.list_item_debug_mode_characteristic_of_service,
                    null
                ) as LinearLayout
                val characteristicExpansion =
                    characteristicContainer.findViewById<LinearLayout>(R.id.characteristic_expansion)
                val propsContainer =
                    characteristicContainer.findViewById<LinearLayout>(R.id.characteristic_props_container)
                val characteristicNameTextView =
                    characteristicContainer.findViewById<TextView>(R.id.characteristic_title)
                val characteristicUuidTextView =
                    characteristicContainer.findViewById<TextView>(R.id.characteristic_uuid)
                val descriptorsLabelTextView =
                    characteristicContainer.findViewById<TextView>(R.id.text_view_descriptors_label)
                val descriptorLinearLayout =
                    characteristicContainer.findViewById<LinearLayout>(R.id.linear_layout_descriptor)
                val characteristicEditNameImageView =
                    characteristicContainer.findViewById<ImageView>(R.id.image_view_edit_charac_name)
                val characEditNameLinearLayout =
                    characteristicContainer.findViewById<LinearLayout>(R.id.linear_layout_edit_charac_name)
                val characteristicSeparator =
                    characteristicContainer.findViewById<View>(R.id.characteristics_separator)
                val id = View.generateViewId()
                characteristicExpansion.id = id
                loadCharacteristicDescriptors(
                    bluetoothGattCharacteristic,
                    descriptorsLabelTextView,
                    descriptorLinearLayout
                )

                // init/populate ui elements with info from bluetooth data for characteristic of service
                characteristicNameTextView.text = characteristicName
                if (characteristicName == getString(R.string.unknown_characteristic_label)) {
                    characteristicEditNameImageView.visibility = View.VISIBLE
                    characEditNameLinearLayout.setOnClickListener {
                        MappingsEditDialog(
                            characteristicNameTextView.text.toString(),
                            characteristicUuidText,
                            object : MappingCallback {
                                override fun onNameChanged(mapping: Mapping) {
                                    characteristicNameTextView.text = mapping.name
                                    characteristicNamesMap[mapping.uuid] = mapping
                                }
                            }, MappingType.CHARACTERISTIC
                        ).show(parentFragmentManager, "dialog_mappings_edit")
                    }
                    if (characteristicNamesMap.containsKey(characteristicUuidText)) {
                        characteristicNameTextView.text =
                            characteristicNamesMap[characteristicUuidText]?.name
                    }
                }
                characteristicUuidTextView.text = characteristicUuidText

                // hide divider between characteristics if last characteristic of service
                if (serviceItemContainer.llGroupOfCharacteristicsForService.childCount == characteristics.size - 1) {
                    characteristicSeparator.visibility = View.GONE
                    serviceItemContainer.llLastItemDivider.visibility = View.VISIBLE
                }
                serviceItemContainer.llGroupOfCharacteristicsForService.addView(
                    characteristicContainer
                )
                val finalServiceName = serviceName

                // add properties to characteristic list item in expansion
                addPropertiesToCharacteristic(bluetoothGattCharacteristic, propsContainer)
                setPropertyClickListeners(
                    propsContainer,
                    bluetoothGattCharacteristic,
                    bluetoothGattService,
                    finalServiceName,
                    characteristicExpansion
                )
                serviceItemContainer.setCharacteristicNotificationState(
                    characteristicUuidText,
                    BLEUtils.Notifications.DISABLED
                )
                characteristicContainer.setOnClickListener {
                    if (characteristicExpansion.visibility == View.VISIBLE) {
                        characteristicExpansion.visibility = View.GONE
                    } else {
                        characteristicExpansion.visibility = View.VISIBLE
                        if (characteristicFragments.containsKey(id)) {
                            currentWriteReadFragment = characteristicFragments[id]
                        } else {
                            currentWriteReadFragment = initFragmentCharacteristicDetail(
                                bluetoothGattCharacteristic,
                                id,
                                bluetoothGattService,
                                characteristicExpansion,
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
            serviceItemContainers[serviceName] = serviceItemContainer
        }
    }

    protected abstract fun getEditableService(
        uuid: UUID?,
        service: BluetoothGattService
    ): BluetoothGattService

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

    private fun initFragmentCharacteristicDetail(
        bluetoothGattCharacteristic: BluetoothGattCharacteristic,
        expansionId: Int,
        service: BluetoothGattService,
        characteristicExpansion: LinearLayout,
        displayWriteDialog: Boolean
    ): FragmentCharacteristicDetail {
        val characteristicDetail = FragmentCharacteristicDetail()
        characteristicDetail.isRemote = isRemote
        characteristicDetail.address = bluetoothGatt?.device?.address
        characteristicDetail.setmService(service)
        characteristicDetail.setmBluetoothCharact(bluetoothGattCharacteristic)
        characteristicDetail.displayWriteDialog = displayWriteDialog
        characteristicExpansion.visibility = View.VISIBLE

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
                            characteristicExpansion,
                            false
                        )
                        characteristicFragments[id] = currentWriteReadFragment
                    }
                    characteristicExpansion.visibility = View.VISIBLE
                    readCharacteristic(bluetoothGattCharacteristic)
                }
                Common.PROPERTY_VALUE_WRITE -> propertyContainer.setOnClickListener {
                    writeIcon?.startAnimation(
                        AnimationUtils.loadAnimation(requireContext(), R.anim.property_image_click)
                    )
                    if (characteristicFragments.containsKey(id)) {
                        currentWriteReadFragment = characteristicFragments[id]
                        characteristicFragments[id]?.showCharacteristicWriteDialog()
                    } else {
                        currentWriteReadFragment = initFragmentCharacteristicDetail(
                            bluetoothGattCharacteristic,
                            id,
                            service,
                            characteristicExpansion,
                            true
                        )
                        characteristicFragments[id] = currentWriteReadFragment
                    }
                    characteristicExpansion.visibility = View.VISIBLE
                }
                Common.PROPERTY_VALUE_NOTIFY -> propertyContainer.setOnClickListener {
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
                            characteristicExpansion,
                            false
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
                Common.PROPERTY_VALUE_INDICATE -> propertyContainer.setOnClickListener {
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
                            characteristicExpansion,
                            false
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
                else -> {
                }
            }
        }
    }

    protected abstract fun readCharacteristic(bluetoothGattCharacteristic: BluetoothGattCharacteristic)

    private fun setIndicateProperty(
        bluetoothGattCharacteristic: BluetoothGattCharacteristic,
        serviceName: String,
        indicatePropertyIcon: ImageView?,
        indicatePropertyName: TextView?,
        notificationIcon: ImageView?,
        notificationText: TextView?
    ) {
        var indicationsEnabled =
            currentWriteReadFragment?.indicationsEnabled!! // Indication not enabled
        val submitted = BLEUtils.setNotificationForCharacteristic(
            bluetoothGatt!!,
            bluetoothGattCharacteristic,
            if (indicationsEnabled) BLEUtils.Notifications.DISABLED else BLEUtils.Notifications.INDICATE
        ) // If indication not enabled -> enable

        if (submitted) {
            indicationsEnabled = !indicationsEnabled
        }

        currentWriteReadFragment?.indicationsEnabled = indicationsEnabled
        indicatePropertyIcon?.setBackgroundResource(
            if (indicationsEnabled) R.drawable.ic_indicate_on else R.drawable.ic_indicate_off
        ) // enable -> blue, disable -> grey
        indicatePropertyName?.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (indicationsEnabled) R.color.silabs_blue else R.color.silabs_inactive
            )
        ) // enable -> blue, disable -> grey

        val characteristicUuid = getUuidFromBluetoothGattCharacteristic(bluetoothGattCharacteristic)
        serviceItemContainers[serviceName]?.setCharacteristicNotificationState(
            characteristicUuid,
            if (indicationsEnabled) BLEUtils.Notifications.INDICATE else BLEUtils.Notifications.DISABLED
        )
        currentWriteReadFragment?.notificationsEnabled = false

        if (notificationIcon != null) {
            notificationIcon.setBackgroundResource(R.drawable.ic_notify_off)
            notificationText?.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.silabs_inactive)
            )
        }
    }

    private fun getUuidFromBluetoothGattCharacteristic(bluetoothGattCharacteristic: BluetoothGattCharacteristic): String {
        val characteristic = Engine.getCharacteristic(bluetoothGattCharacteristic.uuid)
        return (characteristic?.uuid ?: bluetoothGattCharacteristic.uuid)
            .let { UuidUtils.getUuidText(it) }
    }

    private fun setNotifyProperty(
        bluetoothGattCharacteristic: BluetoothGattCharacteristic,
        serviceName: String,
        notifyPropertyIcon: ImageView?,
        notifyPropertyName: TextView?,
        indicationIcon: ImageView?,
        indicationText: TextView?
    ) {
        var notificationsEnabled = currentWriteReadFragment?.notificationsEnabled!!
        val submitted = BLEUtils.setNotificationForCharacteristic(
            bluetoothGatt!!,
            bluetoothGattCharacteristic,
            if (notificationsEnabled) BLEUtils.Notifications.DISABLED else BLEUtils.Notifications.NOTIFY
        )

        if (submitted) {
            notificationsEnabled = !notificationsEnabled
        }

        currentWriteReadFragment?.notificationsEnabled = notificationsEnabled
        notifyPropertyIcon?.setBackgroundResource(if (notificationsEnabled) R.drawable.ic_notify_on else R.drawable.ic_notify_off)
        notifyPropertyName?.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (notificationsEnabled) R.color.silabs_blue else R.color.silabs_inactive
            )
        )

        val characteristicUuid = getUuidFromBluetoothGattCharacteristic(bluetoothGattCharacteristic)
        serviceItemContainers[serviceName]?.setCharacteristicNotificationState(
            characteristicUuid,
            if (notificationsEnabled) BLEUtils.Notifications.NOTIFY else BLEUtils.Notifications.DISABLED
        )
        currentWriteReadFragment?.indicationsEnabled = false

        if (indicationIcon != null) {
            indicationIcon.setBackgroundResource(R.drawable.ic_indicate_off)
            indicationText?.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.silabs_inactive)
            )
        }
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

    private fun initServiceItemContainer(
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
        if ((serviceName == getString(R.string.unknown_service))) {
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
