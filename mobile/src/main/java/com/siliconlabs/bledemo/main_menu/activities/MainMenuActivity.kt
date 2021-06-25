package com.siliconlabs.bledemo.main_menu.activities

import android.Manifest
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.siliconlabs.bledemo.BuildConfig
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.base.BaseActivity
import com.siliconlabs.bledemo.bluetooth.ConnectedGatts
import com.siliconlabs.bledemo.bluetooth.services.BluetoothService
import com.siliconlabs.bledemo.browser.adapters.ViewPagerAdapter
import com.siliconlabs.bledemo.browser.dialogs.LeaveApplicationDialog
import com.siliconlabs.bledemo.browser.dialogs.LeaveApplicationDialog.Callback
import com.siliconlabs.bledemo.main_menu.adapters.MenuAdapter
import com.siliconlabs.bledemo.main_menu.dialogs.LocationInfoDialog
import com.siliconlabs.bledemo.main_menu.fragments.DemoFragment
import com.siliconlabs.bledemo.main_menu.fragments.DevelopFragment
import com.siliconlabs.bledemo.main_menu.menu_items.MainMenuItem
import com.siliconlabs.bledemo.utils.Constants
import com.siliconlabs.bledemo.utils.SharedPrefUtils
import com.siliconlabs.bledemo.views.BluetoothEnableBar
import kotlinx.android.synthetic.main.actionbar.*
import kotlinx.android.synthetic.main.activity_main_menu.*
import kotlinx.android.synthetic.main.bluetooth_enable_bar.*

class MainMenuActivity : BaseActivity(), MenuAdapter.OnMenuItemClickListener {
    private lateinit var sharedPrefUtils: SharedPrefUtils
    private lateinit var bluetoothEnableBar: BluetoothEnableBar
    private lateinit var binding: BluetoothService.Binding
    private var service: BluetoothService? = null

    private var helpDialog: Dialog? = null
    private var isBluetoothAdapterEnabled = true

    private val bluetoothAdapterStateChangeListener: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_TURNING_OFF -> {
                        isBluetoothAdapterEnabled = false
                        bluetoothEnableBar.show()
                    }
                    BluetoothAdapter.STATE_ON -> {
                        if (!isBluetoothAdapterEnabled) showMessage(R.string.toast_bluetooth_enabled)
                        isBluetoothAdapterEnabled = true
                        bluetooth_enable?.visibility = View.GONE
                    }
                    BluetoothAdapter.STATE_TURNING_ON -> isBluetoothAdapterEnabled = false
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)
        askForWriteExternalStoragePermission()
        bottom_navigation_view.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)
        setSupportActionBar(toolbar)
        title = getString(R.string.title_Develop)
        bottom_navigation_view.selectedItemId = R.id.navigation_develop
        iv_go_back.visibility = View.INVISIBLE
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        isBluetoothAdapterEnabled = bluetoothAdapter?.isEnabled ?: false
        bluetoothEnableBar = findViewById(R.id.bluetooth_enable)

        binding = object : BluetoothService.Binding(applicationContext) {
            override fun onBound(service: BluetoothService?) {
                this@MainMenuActivity.service = service
            }
        }
        binding.bind()

        sharedPrefUtils = SharedPrefUtils(this)

        // handle bluetooth adapter on/off state
        bluetooth_enable_btn.setOnClickListener { bluetoothEnableBar.changeEnableBluetoothAdapterToConnecting() }
        enable_location.setOnClickListener {
            val enableLocationIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(enableLocationIntent)
        }
        location_info.setOnClickListener {
            val dialog = LocationInfoDialog()
            dialog.show(supportFragmentManager, "location_info_dialog")
        }

        initHelpDialog()
        initViewPager()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.unbind()
    }

    private fun initViewPager() {
        setupViewPager(view_pager)
        view_pager.currentItem = 1
        initViewPagerBehavior(view_pager)
    }

    override fun onBackPressed() {
        if (sharedPrefUtils.shouldDisplayLeaveApplicationDialog() && !ConnectedGatts.isEmpty()) {
            val dialog = LeaveApplicationDialog(object : Callback {
                override fun onOkClicked() {
                    super@MainMenuActivity.onBackPressed()
                }
            })
            dialog.show(supportFragmentManager, "leave_application_dialog")
        } else {
            super.onBackPressed()
        }
    }

    private fun askForWriteExternalStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), WRITE_EXTERNAL_STORAGE_REQUEST_CODE)
        }
    }

    private fun initViewPagerBehavior(viewPager: ViewPager?) {
        viewPager?.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(i: Int, v: Float, i1: Int) {}
            override fun onPageSelected(position: Int) {
                bottom_navigation_view.menu.getItem(position).isChecked = true
                if (position == 0) toolbar.title = Constants.BOTTOM_NAVI_DEMO
                else if (position == 1) toolbar.title = Constants.BOTTOM_NAVI_DEVELOP
            }

            override fun onPageScrollStateChanged(i: Int) {}
        })
    }

    private fun setupViewPager(viewPager: ViewPager?) {
        val viewPagerAdapter = ViewPagerAdapter(supportFragmentManager)
        viewPagerAdapter.addFragment(DemoFragment(), getString(R.string.title_Demo))
        viewPagerAdapter.addFragment(DevelopFragment(), getString(R.string.title_Develop))
        viewPager?.adapter = viewPagerAdapter
    }

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { menuItem ->
        when (menuItem.itemId) {
            R.id.navigation_demo -> {
                view_pager.currentItem = 0
                toolbar?.title = Constants.BOTTOM_NAVI_DEMO
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_develop -> {
                view_pager.currentItem = 1
                toolbar?.title = Constants.BOTTOM_NAVI_DEVELOP
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onResume() {
        super.onResume()
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        isBluetoothAdapterEnabled = bluetoothAdapter?.isEnabled ?: false

        if (!isBluetoothAdapterEnabled) bluetoothEnableBar.show() else bluetoothEnableBar.hide()
        if (!isLocationEnabled) showLocationDisabledBar() else hideLocationDisabledBar()

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothAdapterStateChangeListener, filter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(bluetoothAdapterStateChangeListener)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_help -> {
                helpDialog?.show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private val isLocationEnabled: Boolean
        get() {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }

    private fun hideLocationDisabledBar() {
        location_disabled.visibility = View.GONE
    }

    private fun showLocationDisabledBar() {
        location_disabled.visibility = View.VISIBLE
    }

    private fun initHelpDialog() {
        helpDialog = Dialog(this@MainMenuActivity).apply {
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_help_demo_item)
            findViewById<TextView>(R.id.dialog_help_version_text).text = getString(
                R.string.version_text,
                BuildConfig.VERSION_NAME
            )
            findViewById<View>(R.id.help_ok_button)?.setOnClickListener { dismiss() }

            findViewById<TextView>(R.id.silabs_products_wireless)
                ?.linkToWebpage("https://" + getString(R.string.help_text_moreinfo_url))
            findViewById<TextView>(R.id.silabs_support)
                ?.linkToWebpage("https://" + getString(R.string.help_text_support_url))
            findViewById<TextView>(R.id.github_siliconlabs_efrconnect)
                ?.linkToWebpage("https://" + getString(R.string.help_text_sourcecode_url))
            findViewById<TextView>(R.id.docs_silabs_bluetooth_latest)
                ?.linkToWebpage("https://" + getString(R.string.help_text_documentation_url))
            findViewById<TextView>(R.id.docs_silabs_release_notes)
                ?.linkToWebpage("https://" + getString(R.string.help_text_release_notes_url))
            findViewById<TextView>(R.id.users_guide_efrconnect)
                ?.linkToWebpage("https://" + getString(R.string.help_text_users_guide_url))
            findViewById<TextView>(R.id.help_text_playstore)
                ?.linkToWebpage("https://play.google.com/store/apps/developer?id=Silicon+Laboratories")
        }
    }

    fun View.linkToWebpage(url: String) {
        setOnClickListener {
            val uri = Uri.parse(url)
            val launchBrowser = Intent(Intent.ACTION_VIEW, uri)
            startActivity(launchBrowser)
        }
    }

    override fun onMenuItemClick(menuItem: MainMenuItem) {
        if (askForLocationPermission()) {
            menuItem.onClick(this@MainMenuActivity)
        }
    }

    private fun askForLocationPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISION_REQUEST_CODE
            )
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISION_REQUEST_CODE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showMessage(R.string.permissions_granted_successfully)
            } else {
                showMessage(R.string.permissions_not_granted)
            }
            WRITE_EXTERNAL_STORAGE_REQUEST_CODE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showMessage(R.string.permissions_granted_successfully)
            } else {
                showMessage(R.string.Grant_WRITE_FILES_permission_to_access_OTA)
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISION_REQUEST_CODE = 200
        private const val WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 300
    }
}
