/*
 * Bluegiga’s Bluetooth Smart Android SW for Bluegiga BLE modules
 * Contact: support@bluegiga.com.
 *
 * This is free software distributed under the terms of the MIT license reproduced below.
 *
 * Copyright (c) 2013, Bluegiga Technologies
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files ("Software")
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF
 * ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A  PARTICULAR PURPOSE.
 */
package com.siliconlabs.bledemo.Browser.Activities

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.app.Dialog
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.webkit.WebView
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.google.android.gms.appindexing.Action
import com.google.android.gms.appindexing.AppIndex
import com.google.android.gms.appindexing.Thing
import com.google.android.gms.common.api.GoogleApiClient
import com.siliconlabs.bledemo.Adapters.DeviceInfoViewHolder
import com.siliconlabs.bledemo.Base.BaseActivity
import com.siliconlabs.bledemo.Bluetooth.BLE.BlueToothService
import com.siliconlabs.bledemo.Bluetooth.BLE.BluetoothDeviceInfo
import com.siliconlabs.bledemo.Bluetooth.BLE.Discovery
import com.siliconlabs.bledemo.Bluetooth.BLE.Discovery.BluetoothDiscoveryHost
import com.siliconlabs.bledemo.Bluetooth.BLE.Discovery.DeviceContainer
import com.siliconlabs.bledemo.Bluetooth.BLE.ErrorCodes.getDeviceDisconnectedMessage
import com.siliconlabs.bledemo.Bluetooth.BLE.ErrorCodes.getFailedConnectingToDeviceMessage
import com.siliconlabs.bledemo.Bluetooth.BLE.TimeoutGattCallback
import com.siliconlabs.bledemo.Bluetooth.Parsing.Engine
import com.siliconlabs.bledemo.Browser.Adapters.ConnectionsAdapter
import com.siliconlabs.bledemo.Browser.Adapters.DebugModeDeviceAdapter
import com.siliconlabs.bledemo.Browser.Adapters.LogAdapter
import com.siliconlabs.bledemo.Browser.DebugModeCallback
import com.siliconlabs.bledemo.Browser.Dialogs.LeaveBrowserDialog
import com.siliconlabs.bledemo.Browser.Dialogs.LeaveBrowserDialog.LeaveBrowserCallback
import com.siliconlabs.bledemo.Browser.Fragments.*
import com.siliconlabs.bledemo.Browser.Models.Logs.TimeoutLog
import com.siliconlabs.bledemo.Browser.Models.ToolbarName
import com.siliconlabs.bledemo.Browser.ServicesConnectionsCallback
import com.siliconlabs.bledemo.Browser.ToolbarCallback
import com.siliconlabs.bledemo.Browser.Views.ExpandableTextView
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.Utils.Constants
import com.siliconlabs.bledemo.Utils.FilterDeviceParams
import com.siliconlabs.bledemo.Utils.SharedPrefUtils
import kotlinx.android.synthetic.main.actionbar.*
import kotlinx.android.synthetic.main.activity_browser.*
import kotlinx.android.synthetic.main.toolbar_browser.*
import java.util.*

class BrowserActivity : BaseActivity(), DebugModeCallback, OnRefreshListener, BluetoothDiscoveryHost, DeviceContainer<BluetoothDeviceInfo>, ServicesConnectionsCallback {

    private var binding: BlueToothService.Binding? = null
    private var service: BlueToothService? = null
    private val discovery = Discovery(this, this)

    private lateinit var sharedPrefUtils: SharedPrefUtils
    private lateinit var devicesAdapter: DebugModeDeviceAdapter
    private lateinit var connectionsAdapter: ConnectionsAdapter
    private lateinit var handler: Handler
    private lateinit var connectionsFragment: ConnectionsFragment
    private lateinit var filterFragment: FilterFragment
    private lateinit var loggerFragment: LoggerFragment
    private lateinit var sortFragment: SortFragment

    private lateinit var activeFilterBar: ExpandableTextView

    private var bluetoothEnableDialog: Dialog? = null
    private var dialogLicense: Dialog? = null
    private var toast: Toast? = null

    private var isBluetoothAdapterEnabled = false
    private var btToolbarOpened = false
    private var allowUpdating = true
    private var scanning = false

    private var connectToDeviceAddress = ""
    private var retryAttempts = 0

    private val bluetoothAdapterStateChangeListener: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val defaultBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_OFF -> finish()
                    BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_TURNING_ON -> isBluetoothAdapterEnabled = false
                    BluetoothAdapter.STATE_ON -> {
                        if (defaultBluetoothAdapter != null &&
                                defaultBluetoothAdapter.isEnabled) {
                            if (!isBluetoothAdapterEnabled) {
                                toast = Toast.makeText(this@BrowserActivity, R.string.toast_bluetooth_enabled, Toast.LENGTH_SHORT)
                                toast?.show()
                            }
                            updateListWhenAdapterIsReady = false
                            bluetooth_enable.visibility = View.GONE
                            discovery.disconnect()
                            discovery.connect(this@BrowserActivity)
                            startScanning()
                        }
                        isBluetoothAdapterEnabled = true
                    }
                }
            }
        }
    }

    private var btToolbarOpenedName: ToolbarName? = null
    private val errorMessageQueue = LinkedList<String>()

    private val displayQueuedMessages: Runnable = object : Runnable {
        override fun run() {
            handler.removeCallbacks(this)
            synchronized(this) {
                if (errorMessageQueue.size > 0 && toast?.view?.isShown != null && toast?.view?.isShown!!) {
                    handler.postDelayed(this, 1000)
                } else if (errorMessageQueue.size > 0) {
                    toast = Toast.makeText(this@BrowserActivity, errorMessageQueue.removeFirst(), Toast.LENGTH_LONG)
                    toast?.show()
                    handler.postDelayed(this, 1000)
                } else {
                }
            }
        }
    }

    private val restartScanTimeout = Runnable {
        discovery.clearDevicesCache()
        flushContainer()
        sharedPrefUtils.mergeTmpDevicesToFavorites()
        allowUpdating = true
        if (!scanning) {
            startScanning()
        }
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private var client: GoogleApiClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_browser)
        setSupportActionBar(toolbar)
        setScanningButtonListener()

        sharedPrefUtils = SharedPrefUtils(applicationContext)
        handler = Handler()

        findViewById<View>(R.id.iv_go_back).setOnClickListener { onBackPressed() }

        activeFilterBar = findViewById(R.id.etv_active_filter_bar)
        activeFilterBar.setup(5)

        bindBluetoothService()

        Engine.instance?.init(this.applicationContext)
        Constants.clearLogs()
        initLicenseDialog()
        initDevicesRecyclerView()
        initSwipeRefreshLayout()

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothAdapterStateChangeListener, filter)
        bluetooth_enable_btn.setOnClickListener {
            BluetoothAdapter.getDefaultAdapter().enable()
            changeEnableBluetoothAdapterToConnecting()
        }

        discovery.connect(this)
        client = GoogleApiClient.Builder(this).addApi(AppIndex.API).build()

        startScanning()

        fragmentsInit()
        handleToolbarClickEvents()
        bluetooth_browser_background.setOnClickListener {
            if (btToolbarOpened) {
                closeToolbar()
                btToolbarOpened = !btToolbarOpened
            }
        }

        closeToolbar()
    }

    private fun setScanningButtonListener() {
        btn_scanning.setOnClickListener {
            if (scanning || btn_scanning.text == getString(R.string.button_stop_scanning)) {
                handler.removeCallbacks(restartScanTimeout)
                rv_debug_devices.visibility = View.VISIBLE
                onScanningStopped()
            } else {
                startScanning()
            }
        }
    }

    private fun bindBluetoothService() {
        binding = object : BlueToothService.Binding(this@BrowserActivity) {
            override fun onBound(service: BlueToothService?) {
                this@BrowserActivity.service = service
            }
        }
        binding?.bind()
    }

    private fun fragmentsInit() {
        loggerFragment = LoggerFragment().setCallback(object : ToolbarCallback {
            override fun close() {
                closeToolbar()
                btToolbarOpened = !btToolbarOpened
            }

            override fun submit(filterDeviceParams: FilterDeviceParams?, close: Boolean) {}
        })
        loggerFragment.adapter = LogAdapter(Constants.LOGS, applicationContext)
        connectionsFragment = ConnectionsFragment().setCallback(object : ToolbarCallback {
            override fun close() {
                closeToolbar()
                btToolbarOpened = !btToolbarOpened
                devicesAdapter.notifyDataSetChanged()
                updateCountOfConnectedDevices()
            }

            override fun submit(filterDeviceParams: FilterDeviceParams?, close: Boolean) {}
        })
        connectionsAdapter = ConnectionsAdapter(connectedBluetoothDevices, applicationContext)
        connectionsFragment.adapter = connectionsAdapter
        connectionsFragment.adapter?.setServicesConnectionsCallback(this)
        filterFragment = FilterFragment()
        sortFragment = SortFragment().setCallback(object : SortCallback {
            override fun setSortMode(mode: SortMode) {
                changeToolbarSortIcon(mode)
                devicesAdapter.setSortMode(mode)
            }

        })
    }

    private fun changeToolbarSortIcon(mode: SortMode) {
        iv_sort.apply {
            setImageDrawable(ContextCompat.getDrawable(this@BrowserActivity, mode.resId))
            if (btToolbarOpenedName == ToolbarName.SORT) {
                DrawableCompat.setTint(drawable, ContextCompat.getColor(this@BrowserActivity, R.color.silabs_blue))
            }
        }
    }

    override fun onDisconnectClicked(deviceInfo: BluetoothDeviceInfo?) {
        val successDisconnected = service?.disconnectGatt(deviceInfo?.address!!)
        if (!successDisconnected!!) {
            toast = Toast.makeText(applicationContext, R.string.device_not_from_EFR, Toast.LENGTH_LONG)
            toast?.show()
        }
        updateCountOfConnectedDevices()
        devicesAdapter.notifyDataSetChanged()
    }

    override fun onDeviceClicked(device: BluetoothDeviceInfo?) {
        connectToDevice(device)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == BLUETOOTH_SETTINGS_REQUEST_CODE) {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (!bluetoothAdapter.isEnabled && bluetoothEnableDialog != null) {
                bluetoothEnableDialog?.show()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        scanning_gradient_container.visibility = View.VISIBLE
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothAdapterStateChangeListener, filter)
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!scanning) {
            setScanningButtonStart()
        }

        if (!bluetoothAdapter.isEnabled) {
            finish()
        }

        isBluetoothAdapterEnabled = bluetoothAdapter?.isEnabled ?: false
        updateCountOfConnectedDevices()
        devicesAdapter.notifyDataSetChanged()
    }

    private fun handleToolbarClickEvents() {
        setToolbarItemsNotClicked()
        ll_log.setOnClickListener(View.OnClickListener {
            if (btToolbarOpened && btToolbarOpenedName == ToolbarName.LOGS) {
                loggerFragment.stopLogUpdater()
                closeToolbar()
                btToolbarOpened = !btToolbarOpened
                return@OnClickListener
            }
            if (!btToolbarOpened) {
                bluetooth_browser_background.setBackgroundColor(Color.parseColor("#99000000"))
                bluetooth_browser_background.visibility = View.VISIBLE
                ViewCompat.setTranslationZ(bluetooth_browser_background, 4f)
                animateToolbar(0, percentHeightToPx(TOOLBAR_OPEN_PERCENTAGE))
                btToolbarOpened = !btToolbarOpened
            } else if (btToolbarOpenedName == ToolbarName.SORT) {
                animateToolbar(resources.getDimensionPixelSize(R.dimen.sort_toolbar_height), percentHeightToPx(TOOLBAR_OPEN_PERCENTAGE))
            }
            setToolbarItemsNotClicked()
            setToolbarItemClicked(iv_log, tv_log)
            btToolbarOpenedName = ToolbarName.LOGS
            setToolbarFragment(loggerFragment)
            loggerFragment.runLogUpdater()
            loggerFragment.scrollToEnd()
        })

        ll_connections?.setOnClickListener(View.OnClickListener {
            if (btToolbarOpened && btToolbarOpenedName == ToolbarName.CONNECTIONS) {
                closeToolbar()
                btToolbarOpened = !btToolbarOpened
                return@OnClickListener
            }
            if (!btToolbarOpened) {
                bluetooth_browser_background.setBackgroundColor(Color.parseColor("#99000000"))
                bluetooth_browser_background.visibility = View.VISIBLE
                ViewCompat.setTranslationZ(bluetooth_browser_background, 4f)
                animateToolbar(0, percentHeightToPx(TOOLBAR_OPEN_PERCENTAGE))
                btToolbarOpened = !btToolbarOpened
            } else if (btToolbarOpenedName == ToolbarName.SORT) {
                animateToolbar(resources.getDimensionPixelSize(R.dimen.sort_toolbar_height), percentHeightToPx(TOOLBAR_OPEN_PERCENTAGE))
            }
            setToolbarItemsNotClicked()
            setToolbarItemClicked(iv_connections, tv_connections)
            btToolbarOpenedName = ToolbarName.CONNECTIONS
            setToolbarFragment(connectionsFragment)
        })

        ll_filter.setOnClickListener(View.OnClickListener {
            if (btToolbarOpened && btToolbarOpenedName == ToolbarName.FILTER) {
                closeToolbar()
                btToolbarOpened = !btToolbarOpened
                return@OnClickListener
            }
            if (!btToolbarOpened) {
                bluetooth_browser_background.setBackgroundColor(Color.parseColor("#99000000"))
                bluetooth_browser_background.visibility = View.VISIBLE
                ViewCompat.setTranslationZ(bluetooth_browser_background, 4f)
                animateToolbar(0, percentHeightToPx(TOOLBAR_OPEN_PERCENTAGE))
                btToolbarOpened = !btToolbarOpened
            } else if (btToolbarOpenedName == ToolbarName.SORT) {
                animateToolbar(resources.getDimensionPixelSize(R.dimen.sort_toolbar_height), percentHeightToPx(TOOLBAR_OPEN_PERCENTAGE))
            }
            setToolbarItemsNotClicked()
            setToolbarItemClicked(iv_filter, tv_filter)
            btToolbarOpenedName = ToolbarName.FILTER
            setToolbarFragment(filterFragment.setCallback(object : ToolbarCallback {
                override fun close() {
                    closeToolbar()
                    btToolbarOpened = !btToolbarOpened
                }

                override fun submit(filterDeviceParams: FilterDeviceParams?, close: Boolean) {
                    if (close) {
                        closeToolbar()
                        btToolbarOpened = !btToolbarOpened
                        activeFilterBar.minimize()
                    }
                    if (filterDeviceParams?.isEmptyFilter!!) {
                        val params = debug_body.layoutParams as RelativeLayout.LayoutParams
                        params.topMargin = 0
                        debug_body.layoutParams = params

                        activeFilterBar.hide()
                        iv_filter.setImageDrawable(ContextCompat.getDrawable(this@BrowserActivity, R.drawable.ic_filter))
                    } else {
                        val params = debug_body.layoutParams as RelativeLayout.LayoutParams
                        params.topMargin = resources.getDimensionPixelSize(R.dimen.active_filter_min_height)
                        debug_body.layoutParams = params

                        activeFilterBar.show()
                        iv_filter.setImageDrawable(ContextCompat.getDrawable(this@BrowserActivity, R.drawable.ic_filter_active))
                    }
                    filterDevices(filterDeviceParams)
                }
            }))
        })

        ll_sort.setOnClickListener(View.OnClickListener {
            if (btToolbarOpened && btToolbarOpenedName == ToolbarName.SORT) {
                closeToolbar()
                btToolbarOpened = !btToolbarOpened
                return@OnClickListener
            }
            if (!btToolbarOpened) {
                bluetooth_browser_background.setBackgroundColor(Color.parseColor("#99000000"))
                bluetooth_browser_background.visibility = View.VISIBLE
                ViewCompat.setTranslationZ(bluetooth_browser_background, 4f)
                animateToolbar(0, resources.getDimensionPixelSize(R.dimen.sort_toolbar_height))
                btToolbarOpened = !btToolbarOpened
            } else {
                animateToolbar(percentHeightToPx(TOOLBAR_OPEN_PERCENTAGE), resources.getDimensionPixelSize(R.dimen.sort_toolbar_height))
            }
            setToolbarItemsNotClicked()
            setToolbarItemClicked(iv_sort, tv_sort)
            btToolbarOpenedName = ToolbarName.SORT
            setToolbarFragment(sortFragment)
        })

    }

    private fun setScanningButtonStart() {
        btn_scanning.text = resources.getString(R.string.button_start_scanning)
        btn_scanning.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@BrowserActivity, R.color.silabs_blue))
    }

    private fun setScanningButtonStop() {
        btn_scanning.text = resources.getString(R.string.button_stop_scanning)
        btn_scanning.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@BrowserActivity, R.color.silabs_red))
    }

    private fun closeToolbar() {
        if (btToolbarOpenedName == ToolbarName.SORT) {
            animateToolbar(resources.getDimensionPixelSize(R.dimen.sort_toolbar_height), 0)
        } else {
            animateToolbar(percentHeightToPx(TOOLBAR_OPEN_PERCENTAGE), 0)
        }

        setToolbarItemsNotClicked()
        bluetooth_browser_background.visibility = View.GONE
        hideKeyboard()
    }

    private fun setToolbarItemClicked(iv: ImageView, tv: TextView) {
        tv.setTextColor(ContextCompat.getColor(this, R.color.silabs_blue))
        DrawableCompat.setTint(iv.drawable, ContextCompat.getColor(this, R.color.silabs_blue))
    }

    private fun setToolbarItemsNotClicked() {
        tv_log.setTextColor(ContextCompat.getColor(this, R.color.silabs_primary_text))
        DrawableCompat.setTint(iv_log.drawable, ContextCompat.getColor(this, R.color.silabs_primary_text))
        tv_filter.setTextColor(ContextCompat.getColor(this, R.color.silabs_primary_text))
        DrawableCompat.setTint(iv_filter.drawable, ContextCompat.getColor(this, R.color.silabs_primary_text))
        tv_connections.setTextColor(ContextCompat.getColor(this, R.color.silabs_primary_text))
        DrawableCompat.setTint(iv_connections.drawable, ContextCompat.getColor(this, R.color.silabs_primary_text))
        tv_sort.setTextColor(ContextCompat.getColor(this, R.color.silabs_primary_text))
        DrawableCompat.setTint(iv_sort.drawable, ContextCompat.getColor(this, R.color.silabs_primary_text))
    }

    private fun animateToolbar(from: Int, to: Int) {
        val animator = ValueAnimator.ofInt(from, to).setDuration(350)

        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            frame_layout.layoutParams.height = value
            frame_layout.requestLayout()
        }
        frame_layout.visibility = View.VISIBLE
        ViewCompat.setTranslationZ(framelayout_container, 5f)
        val set = AnimatorSet()
        set.play(animator)
        set.interpolator = AccelerateDecelerateInterpolator()
        set.start()
    }

    private fun percentHeightToPx(percent: Int): Int {
        require(!(percent < 0 || percent > 100))
        val height = rv_debug_devices.height + scanning_gradient_container.height
        return (percent.toFloat() / 100.0 * height).toInt()
    }

    private fun setToolbarFragment(fragment: Fragment) {
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .commit()
    }

    override fun onPause() {
        super.onPause()
        Log.d("onPause", "Called")
        onScanningStopped()
        unregisterReceiver(bluetoothAdapterStateChangeListener)
    }

    override fun onBackPressed() {
        if (connecting_container.visibility == View.VISIBLE) {
            Log.d("onBackPressed", "Called")
            service?.clearGatt()
            hideConnectingAnimation()
        } else {
            if (sharedPrefUtils.shouldDisplayLeaveBrowserDialog() && connectedBluetoothDevices.isNotEmpty()) {
                val dialog = LeaveBrowserDialog(object : LeaveBrowserCallback {
                    override fun onOkClicked() {
                        super@BrowserActivity.onBackPressed()
                    }
                })
                dialog.show(supportFragmentManager, "leave_browser_dialog")
            } else {
                super.onBackPressed()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        service?.clearAllGatts()
        binding?.unbind()
        discovery.disconnect()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_browser, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_item_license -> showAbout()
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.menu_mappings -> {
                val intent = Intent(this@BrowserActivity, MappingDictionaryActivity::class.java)
                startActivity(intent)
            }
            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun filterDevices(filterDeviceParams: FilterDeviceParams?) {
        filterDeviceParams?.let {
            activeFilterBar.setText(it.getActiveFilterText(this@BrowserActivity))
            devicesAdapter.filterDevices(it, true)
            rv_debug_devices.adapter = devicesAdapter
            rv_debug_devices.setHasFixedSize(true)
        }
    }

    override fun onRefresh() {
        handler.removeCallbacks(restartScanTimeout)
        allowUpdating = false
        rv_debug_devices.visibility = View.GONE
        no_devices_found.visibility = View.GONE
        looking_for_devices_background.visibility = View.VISIBLE
        setScanningButtonStop()
        handler.postDelayed(restartScanTimeout, RESTART_SCAN_TIMEOUT.toLong())
        swipe_refresh_container.post { swipe_refresh_container.isRefreshing = false }
    }

    private fun initDevicesRecyclerView() {
        sharedPrefUtils.mergeTmpDevicesToFavorites()
        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        rv_debug_devices.layoutManager = layoutManager
        devicesAdapter = DebugModeDeviceAdapter(this,
                object : DeviceInfoViewHolder.Generator(R.layout.adapter_browser_device) {
                    override fun generate(itemView: View): DeviceInfoViewHolder {
                        return DebugModeDeviceAdapter.ViewHolder(
                                this@BrowserActivity,
                                itemView,
                                this@BrowserActivity,
                                SharedPrefUtils(this@BrowserActivity))
                    }
                })
        rv_debug_devices.adapter = devicesAdapter
        rv_debug_devices.setHasFixedSize(true)
    }

    private fun initSwipeRefreshLayout() {
        swipe_refresh_container.setOnRefreshListener(this)
        swipe_refresh_container.setColorSchemeColors(ContextCompat.getColor(this@BrowserActivity, android.R.color.holo_red_dark),
                ContextCompat.getColor(this@BrowserActivity, android.R.color.holo_orange_dark),
                ContextCompat.getColor(this@BrowserActivity, android.R.color.holo_orange_light),
                ContextCompat.getColor(this@BrowserActivity, android.R.color.holo_red_light))
    }

    private fun initLicenseDialog() {
        dialogLicense = Dialog(this)
        dialogLicense?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogLicense?.setContentView(R.layout.dialog_about_silicon_labs_blue_gecko)
        val view = dialogLicense?.findViewById<WebView>(R.id.menu_item_license)
        val closeButton = dialogLicense?.findViewById<Button>(R.id.close_about_btn)
        closeButton?.setOnClickListener { dialogLicense?.dismiss() }
        view?.loadUrl(ABOUT_DIALOG_HTML_ASSET_FILE_PATH)
    }

    // Displays about dialog
    private fun showAbout() {
        dialogLicense?.show()
    }

    // Displays scanning status in UI and starts scanning for new BLE devices
    private fun startScanning() {
        setScanningButtonStop()
        scanning = true
        setScanningProgress(true)
        setScanningStatus(true)
        devicesAdapter.setRunUpdater(true)
        // Connected devices are not deleted from list
        reDiscover(false)
    }

    private fun onScanningStopped() {
        setScanningButtonStart()
        scanning = false
        discovery.stopDiscovery(false)
        setScanningStatus(devicesAdapter.itemCount > 0)
        setScanningProgress(false)
        devicesAdapter.setRunUpdater(false)
        val numbDevicesCurrentlyDisplaying = devicesAdapter.itemCount
        if (numbDevicesCurrentlyDisplaying > 0) {
            no_devices_found.visibility = View.GONE
            looking_for_devices_background.visibility = View.GONE
        }
    }

    private fun setScanningStatus(foundDevices: Boolean) {
        if (!foundDevices) {
            no_devices_found.visibility = View.VISIBLE
            looking_for_devices_background.visibility = View.GONE
        }
    }

    private fun setScanningProgress(isScanning: Boolean) {
        if (devicesAdapter.itemCount == 0) {
            if (isScanning) {
                looking_for_devices_background.visibility = View.VISIBLE
                no_devices_found.visibility = View.GONE
            } else {
                looking_for_devices_background.visibility = View.GONE
                no_devices_found.visibility = View.VISIBLE
            }
        }
    }

    private fun showConnectingAnimation() {
        runOnUiThread {
            scanning_gradient_container.visibility = View.GONE
            val connectingGradientAnimation = AnimationUtils.loadAnimation(this@BrowserActivity, R.anim.connection_translate_right)
            connecting_container.visibility = View.VISIBLE
            connecting_anim_gradient_right_container.startAnimation(connectingGradientAnimation)
            val connectingBarFlyIn = AnimationUtils.loadAnimation(this@BrowserActivity, R.anim.scanning_bar_fly_in)
            connecting_bar_container.startAnimation(connectingBarFlyIn)
        }
    }

    fun hideConnectingAnimation() {
        runOnUiThread {
            devicesAdapter.notifyDataSetChanged()
            connecting_container.visibility = View.GONE
            connecting_anim_gradient_right_container.clearAnimation()
            scanning_gradient_container.visibility = View.VISIBLE
        }
    }

    override fun connectToDevice(device: BluetoothDeviceInfo?) {

        connectToDeviceAddress = device?.address!!
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
            return
        }
        if (scanning) {
            onScanningStopped()
        }

        if (device == null) {
            Log.e("deviceInfo", "null")
            return
        }

        val bluetoothDeviceInfo: BluetoothDeviceInfo = device

        showConnectingAnimation()

        if (service?.isGattConnected(device.address)!!) {
            connectToDeviceAddress = ""
            hideConnectingAnimation()
            if (btToolbarOpened) {
                closeToolbar()
                btToolbarOpened = !btToolbarOpened
            }
            val intent = Intent(this@BrowserActivity, DeviceServicesActivity::class.java)
            intent.putExtra("DEVICE_SELECTED_ADDRESS", device.address)
            startActivity(intent)
            return
        }

        service?.connectGatt(bluetoothDeviceInfo.device, false, object : TimeoutGattCallback() {
            override fun onTimeout() {
                Constants.LOGS.add(TimeoutLog(bluetoothDeviceInfo.device))
                toast = Toast.makeText(this@BrowserActivity, R.string.toast_connection_timed_out, Toast.LENGTH_SHORT)
                toast?.show()
                hideConnectingAnimation()
                connectToDeviceAddress = ""
            }

            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                updateCountOfConnectedDevices()
                service?.let { it.gattMap[device.address] = gatt }

                hideConnectingAnimation()
                if (newState == BluetoothGatt.STATE_DISCONNECTED && status != BluetoothGatt.GATT_SUCCESS) {
                    if (status == 133 && retryAttempts < RECONNECTION_RETRIES) {
                        Log.d("onConnectionStateChange", "[Browser]: Reconnect due to 0x85 (133) error")
                        retryAttempts++
                        handler.postDelayed({
                            gatt.close()
                            connectToDevice(device)
                        }, 1000)
                        return
                    }

                    val deviceName = if (TextUtils.isEmpty(bluetoothDeviceInfo.name)) getString(R.string.not_advertising_shortcut) else bluetoothDeviceInfo.name
                    if (gatt.device.address == connectToDeviceAddress) {
                        connectToDeviceAddress = ""
                        synchronized(errorMessageQueue) { errorMessageQueue.add(getFailedConnectingToDeviceMessage(deviceName, status)) }
                    } else {
                        synchronized(errorMessageQueue) { errorMessageQueue.add(getDeviceDisconnectedMessage(deviceName, status)) }
                    }
                    handler.removeCallbacks(displayQueuedMessages)
                    handler.postDelayed(displayQueuedMessages, 500)
                } else if (newState == BluetoothGatt.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                    service?.let {
                        if (it.isGattConnected) {
                            connectToDeviceAddress = ""
                            if (btToolbarOpened) {
                                closeToolbar()
                                btToolbarOpened = !btToolbarOpened
                            }
                            val intent = Intent(this@BrowserActivity, DeviceServicesActivity::class.java)
                            intent.putExtra("DEVICE_SELECTED_ADDRESS", device.address)
                            startActivity(intent)
                        }
                    }
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    Log.d("STATE_DISCONNECTED", "Called")
                    gatt.close()
                    service?.clearGatt()
                }
                retryAttempts = 0
            }
        })
    }

    override fun addToFavorite(deviceAddress: String) {
        sharedPrefUtils.addDeviceToFavorites(deviceAddress)
    }

    override fun removeFromFavorite(deviceAddress: String) {
        sharedPrefUtils.removeDeviceFromFavorites(deviceAddress)
        sharedPrefUtils.removeDeviceFromTemporaryFavorites(deviceAddress)
    }

    override fun addToTemporaryFavorites(deviceAddress: String) {
        sharedPrefUtils.addDeviceToTemporaryFavorites(deviceAddress)
    }

    override fun updateCountOfConnectedDevices() {
        val connectedBluetoothDevices = connectedBluetoothDevices
        val size = connectedBluetoothDevices.size
        runOnUiThread {
            tv_connections.text = getString(R.string.n_Connections, size)
            connectionsFragment.adapter?.connectionsList = connectedBluetoothDevices
            connectionsFragment.adapter?.notifyDataSetChanged()
        }
    }

    private val connectedBluetoothDevices: List<BluetoothDevice>
        get() {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            return bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
        }

    override fun isReady(): Boolean {
        return !isFinishing
    }

    override fun reDiscover() {
        reDiscover(false)
    }

    private var updateListWhenAdapterIsReady = false
    override fun onAdapterDisabled() {}
    override fun onAdapterEnabled() {}

    private fun changeEnableBluetoothAdapterToConnecting() {
        runOnUiThread {
            BluetoothAdapter.getDefaultAdapter().enable()
            updateListWhenAdapterIsReady = true
            bluetooth_enable_btn.visibility = View.GONE
            bluetooth_enable_msg.setText(R.string.bluetooth_adapter_bar_turning_on)
            bluetooth_enable.setBackgroundColor(ContextCompat.getColor(this@BrowserActivity, R.color.silabs_blue))
        }
    }

    override fun flushContainer() {
        devicesAdapter.flushContainer()
    }

    override fun updateWithDevices(devices: List<BluetoothDeviceInfo>) {
        if (allowUpdating) devicesAdapter.updateWith(devices) else return
        if (devicesAdapter.itemCount > 0) {
            looking_for_devices_background.visibility = View.GONE
            no_devices_found.visibility = View.GONE
            rv_debug_devices.visibility = View.VISIBLE
        } else {
            looking_for_devices_background.visibility = View.VISIBLE
        }
    }

    private fun reDiscover(clearCachedDiscoveries: Boolean) {
        discovery.startDiscovery(clearCachedDiscoveries)
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private val indexApiAction: Action
        get() {
            val `object` = Thing.Builder()
                    .setName("BrowserActivity Page")
                    .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                    .build()
            return Action.Builder(Action.TYPE_VIEW)
                    .setObject(`object`)
                    .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                    .build()
        }

    public override fun onStart() {
        super.onStart()
        client?.connect()
        AppIndex.AppIndexApi.start(client, indexApiAction)
    }

    public override fun onStop() {
        super.onStop()
        AppIndex.AppIndexApi.end(client, indexApiAction)
        client?.disconnect()
    }

    companion object {
        private const val ABOUT_DIALOG_HTML_ASSET_FILE_PATH = "file:///android_asset/about.html"
        private const val BLUETOOTH_SETTINGS_REQUEST_CODE = 100
        private const val TOOLBAR_OPEN_PERCENTAGE = 95
        private const val TOOLBAR_CLOSE_PERCENTAGE = 95
        private const val RESTART_SCAN_TIMEOUT = 1000
        private const val RECONNECTION_RETRIES = 3
    }
}