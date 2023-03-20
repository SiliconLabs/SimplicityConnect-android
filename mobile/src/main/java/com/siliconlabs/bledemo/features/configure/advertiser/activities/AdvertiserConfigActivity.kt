package com.siliconlabs.bledemo.features.configure.advertiser.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.util.TypedValue
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import com.siliconlabs.bledemo.features.configure.advertiser.adapters.DataTypeAdapter
import com.siliconlabs.bledemo.features.configure.advertiser.presenters.AdvertiserConfigActivityPresenter
import com.siliconlabs.bledemo.features.configure.advertiser.utils.AdvertiserStorage
import com.siliconlabs.bledemo.features.configure.advertiser.utils.Translator
import com.siliconlabs.bledemo.features.configure.advertiser.utils.Validator
import com.siliconlabs.bledemo.base.activities.BaseActivity
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.features.configure.advertiser.dialogs.*
import com.siliconlabs.bledemo.features.configure.advertiser.enums.*
import com.siliconlabs.bledemo.features.configure.advertiser.models.*
import kotlinx.android.synthetic.main.advertiser_config_data.*
import kotlinx.android.synthetic.main.advertiser_config_name.*
import kotlinx.android.synthetic.main.advertiser_config_parameters.*
import kotlinx.android.synthetic.main.advertiser_config_type.*
import kotlinx.android.synthetic.main.advertiser_data_container.view.*
import kotlinx.android.synthetic.main.data_type_item.view.*
import kotlinx.android.synthetic.main.data_type_layout.view.*
import kotlinx.android.synthetic.main.data_type_layout.view.ib_remove

class AdvertiserConfigActivity : BaseActivity(), IAdvertiserConfigActivityView {
    private lateinit var presenter: AdvertiserConfigActivityPresenter
    private lateinit var advertiserData: AdvertiserData
    private lateinit var startConfigData: AdvertiserData

    private val translator = Translator(this)
    private var position: Int? = null

    private var spLegacyInitialSetup = true
    private var spExtendedInitialSetup = true

    companion object {
        const val EXTRA_ADVERTISER_ITEM = "EXTRA_ADVERTISER_ITEM"
        const val EXTRA_ITEM_POSITION = "EXTRA_ITEM_POSITION"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_advertiser_config)
        prepareToolbar()

        resetInitialSetupFlags()

        presenter = AdvertiserConfigActivityPresenter(this, AdvertiserStorage(this@AdvertiserConfigActivity))
        presenter.prepareAdvertisingTypes()
        presenter.preparePhyParameters()

        advertiserData = intent.extras?.getParcelable<AdvertiserData>(EXTRA_ADVERTISER_ITEM) as AdvertiserData
        position = intent.getIntExtra(EXTRA_ITEM_POSITION, 0)
        presenter.onItemReceived(advertiserData, AdvertiserStorage(this@AdvertiserConfigActivity).isAdvertisingExtensionSupported())

        startConfigData = advertiserData.deepCopy()

        prepareDataSpinners()
        loadData()
        handleAdvertisingSetNameChanges()
        handleAdvertisingLimitSelection()
    }

    private fun resetInitialSetupFlags() {
        spExtendedInitialSetup = true
        spLegacyInitialSetup = true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_advertiser_configuration, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.save_advertiser -> {
                presenter.handleSave()
                true
            }
            android.R.id.home -> {
                exitConfigView()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        exitConfigView()
    }

    private fun prepareToolbar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    private fun exitConfigView() {
        if (hasConfigurationChanged() && AdvertiserStorage(this).shouldDisplayLeaveAdvertiserConfigDialog()) {
            LeaveAdvertiserConfigDialog(object : LeaveAdvertiserConfigDialog.Callback {
                override fun onYesClicked() {
                    presenter.handleSave()
                }

                override fun onNoClicked() {
                    super@AdvertiserConfigActivity.onBackPressed()
                }

            }).show(supportFragmentManager, "dialog_leave_advertiser_config")
        } else {
            super.onBackPressed()
        }
    }

    private fun hasConfigurationChanged(): Boolean {

        // 1. Verify if advertising data / scan response data has changed
        if (startConfigData.advertisingData != advertiserData.advertisingData) return true
        else if (startConfigData.scanResponseData != advertiserData.scanResponseData) return true

        // 2. Verify if any text input is currently not valid
        if (isAnyInputNotValid()) return true

        // 3. Verify if other data has changed
        startConfigData.apply {
            when {
                name != et_advertising_set_name.text.toString() -> return true
                isLegacy != rb_legacy_advertising.isChecked -> return true
                mode != getAdvertisingMode() -> return true
                settings != getExtendedSettings() -> return true
                advertisingIntervalMs != getAdvertisingInterval() -> return true
                txPower != getTxPower() -> return true
                limitType != getAdvertisingLimitType() -> return true
                timeLimit.toString() != et_time_limit.text.toString() -> return true
                isEventLimitAvailable() && eventLimit.toString() != et_event_limit.text.toString() -> return true
            }
        }

        // 4. If configuration has not changed return false
        return false
    }


    private fun handleAdvertisingSetNameChanges() {
        et_advertising_set_name.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                title = s.toString()
            }
        })
    }

    private fun isEventLimitAvailable(): Boolean {
        return ll_event_limit.visibility == View.VISIBLE
    }

    private fun isTxPowerNotValid(): Boolean {
        return ll_tx_power.visibility == View.VISIBLE && !Validator.isTxPowerValid(et_tx_power.text.toString())
    }

    private fun isAdvertisingIntervalNotValid(): Boolean {
        return ll_advertising_interval.visibility == View.VISIBLE && !Validator.isAdvertisingIntervalValid(et_advertising_interval.text.toString())
    }

    private fun isTimeLimitNotValid(): Boolean {
        return rb_time_limit.isChecked && !Validator.isAdvertisingTimeLimitValid(et_time_limit.text.toString(), true)
    }

    private fun isEventLimitNotValid(): Boolean {
        return rb_event_limit.isChecked && !Validator.isAdvertisingEventLimitValid(et_event_limit.text.toString())
    }

    private fun isAnyInputNotValid(): Boolean {
        return isTxPowerNotValid() || isAdvertisingIntervalNotValid() || isTimeLimitNotValid() || isEventLimitNotValid()
    }

    override fun onSaveHandled() {
        if (isTxPowerNotValid())
            showMessage(R.string.advertiser_config_message_invalid_tx_power)
        else if (isAdvertisingIntervalNotValid())
            showMessage(R.string.advertiser_config_message_invalid_interval)
        else if (isTimeLimitNotValid())
            showMessage(R.string.advertiser_config_message_invalid_time_limit)
        else if (isEventLimitNotValid())
            showMessage(R.string.advertiser_config_message_invalid_event_limit)
        else {
            val name = et_advertising_set_name.text.toString()
            val isLegacy = rb_legacy_advertising.isChecked
            val advertisingMode = getAdvertisingMode()
            val settings = getExtendedSettings()
            val interval = getAdvertisingInterval()
            val txPower = getTxPower()

            val limitType = getAdvertisingLimitType()
            val timeLimit = getAdvertisingTimeLimit()
            val eventLimit = getAdvertisingEventLimit()

            presenter.setAdvertisingName(name)
            presenter.setAdvertisingType(isLegacy, advertisingMode)
            presenter.setAdvertisingParams(settings, interval, txPower)
            presenter.setAdvertisingLimit(limitType, timeLimit, eventLimit)

            val resultIntent = Intent()
            resultIntent.putExtra(EXTRA_ADVERTISER_ITEM, advertiserData)
            resultIntent.putExtra(EXTRA_ITEM_POSITION, position)
            setResult(Activity.RESULT_OK, resultIntent)
            super.onBackPressed()
        }
    }

    private fun getAdvertisingInterval(): Int {
        return et_advertising_interval.text.toString().toInt()
    }

    private fun getTxPower(): Int {
        return et_tx_power.text.toString().toInt()
    }

    private fun getAdvertisingLimitType(): LimitType {
        return when {
            rb_no_limit.isChecked -> LimitType.NO_LIMIT
            rb_time_limit.isChecked -> LimitType.TIME_LIMIT
            else -> LimitType.EVENT_LIMIT
        }
    }

    private fun getAdvertisingTimeLimit(): Int {
        return if (rb_time_limit.isChecked) et_time_limit.text.toString().toInt()
        else -1
    }

    private fun getAdvertisingEventLimit(): Int {
        return if (rb_event_limit.isChecked) et_event_limit.text.toString().toInt()
        else -1
    }

    private fun getAdvertisingMode(): AdvertisingMode {
        return if (rb_legacy_advertising.isChecked) translator.getStringAsAdvertisingMode(sp_legacy.selectedItem.toString())
        else translator.getStringAsAdvertisingMode(sp_extended.selectedItem.toString())
    }

    private fun getExtendedSettings(): ExtendedSettings {
        return if (sp_primary_phy.isEnabled) {
            val primaryPhy = translator.getStringAsPhy(sp_primary_phy.selectedItem.toString())
            val secondaryPhy: Phy = translator.getStringAsPhy(sp_secondary_phy.selectedItem.toString())
            ExtendedSettings(cb_include_tx_power.isChecked, cb_anonymous.isChecked, primaryPhy, secondaryPhy)
        } else ExtendedSettings(cb_include_tx_power.isChecked, cb_anonymous.isChecked, null, null)
    }

    private fun handleAdvertisingLimitSelection() {
        rb_no_limit.setOnClickListener {
            rb_no_limit.isChecked = true
            setTimeLimitState(false)
            setEventLimitState(false)
        }
        rb_time_limit.setOnClickListener {
            rb_no_limit.isChecked = false
            setTimeLimitState(true)
            setEventLimitState(false)
        }
        rb_event_limit.setOnClickListener {
            rb_no_limit.isChecked = false
            setTimeLimitState(false)
            setEventLimitState(true)
        }
    }

    private fun setTimeLimitState(enabled: Boolean) {
        rb_time_limit.isChecked = enabled
        et_time_limit.isEnabled = enabled
    }

    private fun setEventLimitState(enabled: Boolean) {
        rb_event_limit.isChecked = enabled
        et_event_limit.isEnabled = enabled
    }

    override fun onAdvertisingTypesPrepared(isLegacy: Boolean, legacyModes: List<AdvertisingMode>, extendedModes: List<AdvertisingMode>) {
        val legacyAdapter = ArrayAdapter(this, R.layout.spinner_item_layout, translator.getValuesAsStringList(legacyModes))
        legacyAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_layout)
        sp_legacy.adapter = legacyAdapter

        val extendedAdapter = ArrayAdapter(this, R.layout.spinner_item_layout, translator.getValuesAsStringList(extendedModes))
        extendedAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_layout)
        sp_extended.adapter = extendedAdapter

        if (isLegacy) tv_extended_adv_not_supported.visibility = View.VISIBLE
        sp_extended.isEnabled = false
        cb_anonymous.isEnabled = false
        cb_include_tx_power.isEnabled = false
        rb_extended_advertising.isEnabled = !isLegacy
        sp_primary_phy.isEnabled = false
        sp_secondary_phy.isEnabled = false

        if (!isLegacy) {
            rb_extended_advertising.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    cb_anonymous.isEnabled = sp_extended.selectedItem.toString() == getString(R.string.advertiser_mode_non_connectable_non_scannable)
                    rb_legacy_advertising.isChecked = false
                    sp_legacy.isEnabled = false
                    sp_extended.isEnabled = true
                    cb_include_tx_power.isEnabled = true
                    sp_primary_phy.isEnabled = true
                    sp_secondary_phy.isEnabled = true

                    presenter.setSupportedData(false, translator.getStringAsAdvertisingMode(sp_extended.selectedItem.toString()))
                }
            }

            sp_extended.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {}
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (!spExtendedInitialSetup) {
                        if (sp_extended.selectedItem.toString() == getString(R.string.advertiser_mode_non_connectable_non_scannable)) {
                            cb_anonymous.isEnabled = true
                        } else {
                            cb_anonymous.isEnabled = false
                            cb_anonymous.isChecked = false
                        }
                        presenter.setSupportedData(false, translator.getStringAsAdvertisingMode(sp_extended.selectedItem.toString()))
                    } else {
                        spExtendedInitialSetup = false
                    }
                }
            }
        }

        rb_legacy_advertising.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                rb_extended_advertising.isChecked = false
                sp_legacy.isEnabled = true
                sp_extended.isEnabled = false
                cb_anonymous.isEnabled = false
                cb_include_tx_power.isEnabled = false
                sp_primary_phy.isEnabled = false
                sp_secondary_phy.isEnabled = false
                cb_include_tx_power.isChecked = false
                cb_anonymous.isChecked = false

                presenter.setSupportedData(true, translator.getStringAsAdvertisingMode(sp_legacy.selectedItem.toString()))
            }
        }

        sp_legacy.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!spLegacyInitialSetup) {
                    presenter.setSupportedData(true, translator.getStringAsAdvertisingMode(sp_legacy.selectedItem.toString()))
                } else {
                    spLegacyInitialSetup = false
                }
            }
        }
    }

    override fun onAdvertisingParametersPrepared(isLegacy: Boolean, primaryPhys: List<Phy>, secondaryPhys: List<Phy>) {
        val primaryPhyAdapter = ArrayAdapter(this, R.layout.spinner_item_layout, translator.getValuesAsStringList(primaryPhys))
        primaryPhyAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_layout)
        sp_primary_phy.adapter = primaryPhyAdapter

        val secondaryPhyAdapter = ArrayAdapter(this, R.layout.spinner_item_layout, translator.getValuesAsStringList(secondaryPhys))
        secondaryPhyAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_layout)
        sp_secondary_phy.adapter = secondaryPhyAdapter
    }

    override fun onSupportedDataPrepared(isAdvertisingData: Boolean, isScanRespData: Boolean) {
        btn_add_advertising_data.isEnabled = isAdvertisingData
        tv_adv_data_available_bytes.visibility = if (isAdvertisingData) View.VISIBLE else View.GONE
        ll_data_advertising_data.visibility = if (isAdvertisingData) View.VISIBLE else View.GONE
        btn_add_scan_response_data.isEnabled = isScanRespData
        tv_scan_resp_available_bytes.visibility = if (isScanRespData) View.VISIBLE else View.GONE
        ll_data_scan_resp_data.visibility = if (isScanRespData) View.VISIBLE else View.GONE
    }

    override fun populateUi(data: AdvertiserData, isAdvertisingEventSupported: Boolean) {
        et_advertising_set_name.setText(data.name)
        title = data.name

        data.isLegacy.let {
            if (data.isLegacy) setSpinnerSelection(data.mode, sp_legacy)
            else setSpinnerSelection(data.mode, sp_extended)
            rb_legacy_advertising.isChecked = data.isLegacy
            rb_extended_advertising.isChecked = !data.isLegacy
        }

        data.settings.let {
            cb_include_tx_power.isChecked = data.settings.includeTxPower
            cb_anonymous.isChecked = data.settings.anonymous
            setSpinnerSelection(data.settings.primaryPhy, sp_primary_phy)
            setSpinnerSelection(data.settings.secondaryPhy, sp_secondary_phy)
        }

        et_advertising_interval.setText(data.advertisingIntervalMs.toString())
        et_tx_power.setText(data.txPower.toString())

        rb_time_limit.text = getString(R.string.advertiser_label_time_limit)

        rb_no_limit.isChecked = data.limitType == LimitType.NO_LIMIT
        setTimeLimitState(data.limitType == LimitType.TIME_LIMIT)
        setEventLimitState(data.limitType == LimitType.EVENT_LIMIT)
        et_time_limit.setText(data.timeLimit.toString())

        if (isAdvertisingEventSupported) et_event_limit.setText(data.eventLimit.toString())
        else ll_event_limit.visibility = View.GONE
    }

    private fun setSpinnerSelection(selection: Any?, spinner: Spinner) {
        for (i in 0 until spinner.adapter.count) if (spinner.getItemAtPosition(i) == translator.getString(selection)) {
            spinner.setSelection(i); break
        }
    }

    private fun loadData() {
        presenter.loadData(DataMode.ADVERTISING_DATA)
        presenter.loadData(DataMode.SCAN_RESPONSE_DATA)
        handleAddData(ll_data_advertising_data.findViewById<LinearLayout>(R.id.data_flags), DataType.FLAGS, DataMode.ADVERTISING_DATA)
    }

    private fun prepareDataSpinners() {
        prepareDataSpinner(sp_advertising_data, ll_data_advertising_data, DataMode.ADVERTISING_DATA)
        prepareDataSpinner(sp_scan_response_data, ll_data_scan_resp_data, DataMode.SCAN_RESPONSE_DATA)
        btn_add_advertising_data.setOnClickListener { sp_advertising_data.performClick() }
        btn_add_scan_response_data.setOnClickListener { sp_scan_response_data.performClick() }
    }

    override fun onDataLoaded(data: DataPacket?, mode: DataMode) {
        val dataContainer = if (mode == DataMode.ADVERTISING_DATA) ll_data_advertising_data else ll_data_scan_resp_data

        data?.let {
            if (data.includeCompleteLocalName)
                handleAddData(dataContainer.findViewById(R.id.data_complete_local_name), DataType.COMPLETE_LOCAL_NAME, mode)
            if (data.manufacturers.isNotEmpty())
                for (item in data.manufacturers) handleAddData(dataContainer.findViewById(R.id.data_manufacturer_data), DataType.MANUFACTURER_SPECIFIC_DATA, mode, item)
            if (data.includeTxPower)
                handleAddData(dataContainer.findViewById(R.id.data_tx_power), DataType.TX_POWER, mode)
            if (data.services16Bit.isNotEmpty())
                handleAddData(dataContainer.findViewById(R.id.data_16bit_services), DataType.COMPLETE_16_BIT, mode, data.services16Bit)
            if (data.services128Bit.isNotEmpty())
                handleAddData(dataContainer.findViewById(R.id.data_128bit_services), DataType.COMPLETE_128_BIT, mode, data.services128Bit)
        }
    }

    private fun prepareDataSpinner(spinner: Spinner, dataContainer: View, mode: DataMode) {
        spinner.setSelection(0)
        spinner.adapter = DataTypeAdapter(this, translator.getAdvertisingDataTypes(), object : DataTypeAdapter.Callback {
            override fun onItemClick(position: Int) {
                when (position) {
                    0 -> handleAddData(dataContainer.findViewById(R.id.data_complete_local_name), DataType.COMPLETE_LOCAL_NAME, mode)
                    1 -> {
                        currentFocus?.clearFocus()
                        hideKeyboard()
                        handleAddData(dataContainer.findViewById(R.id.data_manufacturer_data), DataType.MANUFACTURER_SPECIFIC_DATA, mode)
                    }
                    2 -> handleAddData(dataContainer.data_tx_power, DataType.TX_POWER, mode)
                    3 -> handleAddData(dataContainer.data_16bit_services, DataType.COMPLETE_16_BIT, mode)
                    4 -> handleAddData(dataContainer.data_128bit_services, DataType.COMPLETE_128_BIT, mode)
                }
            }
        })
    }

    private fun handleAddData(layout: ViewGroup, type: DataType, mode: DataMode, extra: Any? = null) {
        val baseContainer = prepareBaseContainer(layout, type)
        when (type) {
            DataType.FLAGS -> addFlagsData(layout, baseContainer)
            DataType.COMPLETE_16_BIT -> addServiceData(layout, baseContainer, mode, DataType.COMPLETE_16_BIT, extra)
            DataType.COMPLETE_128_BIT -> addServiceData(layout, baseContainer, mode, DataType.COMPLETE_128_BIT, extra)
            DataType.COMPLETE_LOCAL_NAME -> addCompleteLocalNameData(layout, baseContainer, mode)
            DataType.TX_POWER -> addTxPowerData(layout, baseContainer, mode)
            DataType.MANUFACTURER_SPECIFIC_DATA -> addManufacturerSpecificData(layout, baseContainer, mode, extra)
        }
    }

    private fun addFlagsData(layout: ViewGroup, baseContainer: View) {
        baseContainer.ib_remove.visibility = View.INVISIBLE
        val itemContainer = prepareItemContainer(baseContainer, getString(R.string.advertiser_label_flags_default))
        itemContainer.ib_remove.visibility = View.GONE
        baseContainer.ll_data.addView(itemContainer)
        layout.addView(baseContainer)
    }

    private fun addServiceData(layout: ViewGroup, baseContainer: View, mode: DataMode, type: DataType, extra: Any? = null) {
        baseContainer.ib_remove.setOnClickListener { layout.removeView(baseContainer) }
        baseContainer.btn_add_service.visibility = View.VISIBLE
        baseContainer.btn_add_service.text = if (type == DataType.COMPLETE_16_BIT) getString(R.string.advertiser_button_add_16bit_service) else getString(R.string.advertiser_button_add_128bit_service)
        setDataSpinnerItemState(false, mode, if (type == DataType.COMPLETE_16_BIT) 3 else 4)

        baseContainer.btn_add_service.setOnClickListener {
            currentFocus?.clearFocus()
            hideKeyboard()
            if (type == DataType.COMPLETE_16_BIT) {
                Service16BitDataDialog(object : Service16BitDataDialog.Callback {
                    override fun onSave(service: Service16Bit) {
                        val serviceItem = prepareItemContainer(baseContainer, service.toString())
                        presenter.include16BitService(mode, service)

                        serviceItem.ib_remove.setOnClickListener {
                            presenter.exclude16BitService(mode, service)
                            baseContainer.ll_data.removeView(serviceItem)
                        }

                        baseContainer.ll_data.addView(serviceItem)
                    }
                }).show(supportFragmentManager, "dialog_16bit_service_data")
            } else {
                Service128BitDataDialog(object : Service128BitDataDialog.Callback {
                    override fun onSave(service: Service128Bit) {
                        val serviceItem = prepareItemContainer(baseContainer, service.uuid.toString())
                        presenter.include128BitService(mode, service)

                        serviceItem.ib_remove.setOnClickListener {
                            presenter.exclude128BitService(mode, service)
                            baseContainer.ll_data.removeView(serviceItem)
                        }

                        baseContainer.ll_data.addView(serviceItem)
                    }
                }).show(supportFragmentManager, "dialog_128bit_service_data")
            }
        }

        baseContainer.ib_remove.setOnClickListener {
            currentFocus?.clearFocus()
            hideKeyboard()
            val count = baseContainer.ll_data.childCount
            removeServicesIfAllowed(layout, baseContainer, count, mode, type)
        }

        extra?.let {
            for (service in extra as List<*>) {
                val serviceItem = prepareItemContainer(baseContainer, if (service is Service16Bit) service.toString() else (service as Service128Bit).uuid.toString())

                serviceItem.ib_remove.setOnClickListener {
                    if (service is Service16Bit) presenter.exclude16BitService(mode, service)
                    else if (service is Service128Bit) presenter.exclude128BitService(mode, service)
                    baseContainer.ll_data.removeView(serviceItem)
                }

                baseContainer.ll_data.addView(serviceItem)
            }
        }

        layout.addView(baseContainer)
    }

    @SuppressLint("MissingPermission")
    private fun addCompleteLocalNameData(layout: ViewGroup, baseContainer: View, mode: DataMode) {
        val itemContainer = prepareItemContainer(baseContainer, BluetoothAdapter.getDefaultAdapter().name)
        itemContainer.ib_remove.visibility = View.GONE
        setDataSpinnerItemState(false, mode, 0)

        presenter.includeCompleteLocalName(mode)

        baseContainer.ib_remove.setOnClickListener {
            layout.removeView(baseContainer)
            presenter.excludeCompleteLocalName(mode)
            setDataSpinnerItemState(true, mode, 0)
        }

        baseContainer.ll_data.addView(itemContainer)
        layout.addView(baseContainer)
    }

    private fun addTxPowerData(layout: ViewGroup, baseContainer: View, mode: DataMode) {
        val itemContainer = prepareItemContainer(baseContainer, getString(R.string.unit_value_dbm, getTxPower()))

        et_tx_power.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (Validator.isTxPowerValid(et_tx_power.text.toString())) {
                    itemContainer.tv_data_text.text = getString(R.string.unit_value_dbm, et_tx_power.text.toString().toInt())
                }
            }
        })

        itemContainer.ib_remove.visibility = View.GONE
        setDataSpinnerItemState(false, mode, 2)

        presenter.includeTxPower(mode)

        baseContainer.ib_remove.setOnClickListener {
            layout.removeView(baseContainer)
            presenter.excludeTxPower(mode)
            setDataSpinnerItemState(true, mode, 2)
        }

        baseContainer.ll_data.addView(itemContainer)
        layout.addView(baseContainer)
    }

    private fun addManufacturerSpecificData(layout: ViewGroup, baseContainer: View, mode: DataMode, extra: Any? = null) {
        if (extra == null) {
            ManufacturerDataDialog(presenter.getManufacturers(mode), object : ManufacturerDataDialog.Callback {
                override fun onSave(manufacturer: Manufacturer) {
                    val itemContainer = prepareItemContainer(baseContainer, manufacturer.getAsDescriptiveText())
                    itemContainer.ib_remove.visibility = View.GONE

                    presenter.includeManufacturerSpecificData(mode, manufacturer)

                    baseContainer.ib_remove.setOnClickListener {
                        presenter.excludeManufacturerSpecificData(mode, manufacturer)
                        layout.removeView(baseContainer)
                    }

                    baseContainer.ll_data.addView(itemContainer)
                    layout.addView(baseContainer)
                }
            }).show(supportFragmentManager, "dialog_manufacturer_data")
        } else {
            val manufacturer = extra as Manufacturer
            val itemContainer = prepareItemContainer(baseContainer, manufacturer.getAsDescriptiveText())
            itemContainer.ib_remove.visibility = View.GONE

            baseContainer.ib_remove.setOnClickListener {
                presenter.excludeManufacturerSpecificData(mode, manufacturer)
                layout.removeView(baseContainer)
            }

            baseContainer.ll_data.addView(itemContainer)
            layout.addView(baseContainer)
        }
    }

    private fun prepareBaseContainer(layout: ViewGroup, type: DataType): View {
        val container = LayoutInflater.from(this@AdvertiserConfigActivity).inflate(R.layout.data_type_layout, layout, false)
        setBaseContainerTitle(container, type)
        return container
    }

    private fun setBaseContainerTitle(container: View, type: DataType) {
        val name = "<b>".plus(type.getIdentifier().plus(" </b>").plus(translator.getDataTypeAsString(type)))
        container.tv_name.text = Html.fromHtml(name, Html.FROM_HTML_MODE_LEGACY)
    }

    private fun prepareItemContainer(baseContainer: View, text: String): View {
        val container = LayoutInflater.from(this@AdvertiserConfigActivity).inflate(R.layout.data_type_item, baseContainer.ll_data, false)
        container.tv_data_text.text = text
        return container
    }

    private fun removeServicesIfAllowed(layout: ViewGroup, container: View, count: Int, mode: DataMode, type: DataType) {
        if (count > 0 && AdvertiserStorage(this).shouldDisplayRemoveServicesDialog()) {
            RemoveServicesDialog(object : RemoveServicesDialog.Callback {
                override fun onOkClicked() {
                    presenter.excludeServices(mode, type)
                    layout.removeView(container)
                    setDataSpinnerItemState(true, mode, if (type == DataType.COMPLETE_16_BIT) 3 else 4)
                }
            }).show(supportFragmentManager, "dialog_remove_services")
        } else {
            presenter.excludeServices(mode, type)
            layout.removeView(container)
            setDataSpinnerItemState(true, mode, if (type == DataType.COMPLETE_16_BIT) 3 else 4)
        }
    }

    private fun setDataSpinnerItemState(enabled: Boolean, mode: DataMode, position: Int) {
        if (mode == DataMode.ADVERTISING_DATA) (sp_advertising_data.adapter as DataTypeAdapter).setItemState(enabled, position)
        else (sp_scan_response_data.adapter as DataTypeAdapter).setItemState(enabled, position)
    }

    override fun updateAvailableBytes(advDataBytes: Int, scanRespDataBytes: Int, maxPacketSize: Int, includeFlags: Boolean) {
        changeDataContainerPadding(advDataBytes < maxPacketSize, ll_data_advertising_data)
        changeDataContainerPadding(scanRespDataBytes < maxPacketSize, ll_data_scan_resp_data)

        ll_data_advertising_data.findViewById<LinearLayout>(R.id.data_flags).visibility = if (includeFlags) View.VISIBLE else View.GONE

        tv_adv_data_available_bytes.setTextAppearance(if (advDataBytes >= 0) R.style.TextViewNoteInfo else R.style.TextViewNoteWarning)
        tv_adv_data_available_bytes.text = (if (advDataBytes >= 0) getString(R.string.advertiser_note_x_bytes_available, advDataBytes)
        else getString(R.string.advertiser_note_x_bytes_beyond, -advDataBytes))

        tv_scan_resp_available_bytes.setTextAppearance(if (scanRespDataBytes >= 0) R.style.TextViewNoteInfo else R.style.TextViewNoteWarning)
        tv_scan_resp_available_bytes.text = (if (scanRespDataBytes >= 0) getString(R.string.advertiser_note_x_bytes_available, scanRespDataBytes)
        else getString(R.string.advertiser_note_x_bytes_beyond, -scanRespDataBytes))
    }

    private fun changeDataContainerPadding(showPadding: Boolean, container: View) {
        val padding8Dp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics).toInt()
        if (showPadding) container.setPadding(0, padding8Dp, 0, padding8Dp)
        else container.setPadding(0, 0, 0, 0)
    }

}
