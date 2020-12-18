package com.siliconlabs.bledemo.Advertiser.Activities

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.Advertiser.Adapters.AdvertiserAdapter
import com.siliconlabs.bledemo.Advertiser.Dialogs.DeviceNameDialog
import com.siliconlabs.bledemo.Advertiser.Dialogs.RemoveAdvertiserDialog
import com.siliconlabs.bledemo.Advertiser.Models.Advertiser
import com.siliconlabs.bledemo.Advertiser.Models.AdvertiserData
import com.siliconlabs.bledemo.Advertiser.Models.AdvertiserList
import com.siliconlabs.bledemo.Advertiser.Presenters.AdvertiserActivityPresenter
import com.siliconlabs.bledemo.Advertiser.Services.AdvertiserService
import com.siliconlabs.bledemo.Advertiser.Utils.AdvertiserStorage
import com.siliconlabs.bledemo.Base.BaseActivity
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.Views.BluetoothEnableBar
import com.siliconlabs.bledemo.Views.FullScreenInfo
import kotlinx.android.synthetic.main.actionbar.*
import kotlinx.android.synthetic.main.activity_advertiser.*
import kotlinx.android.synthetic.main.bluetooth_enable_bar.*

class AdvertiserActivity : BaseActivity(), AdvertiserAdapter.OnItemClickListener, IAdvertiserActivityView {
    private lateinit var presenter: AdvertiserActivityPresenter
    private lateinit var advertiserAdapter: AdvertiserAdapter
    private var isBluetoothAdapterEnabled = true
    private lateinit var bluetoothEnableBar: BluetoothEnableBar
    private lateinit var fullScreenInfo: FullScreenInfo

    private companion object {
        const val EXTRA_ITEM_POSITION = "EXTRA_ITEM_POSITION"
        const val REQUEST_CODE_EDIT = 1000
        const val DELAY_NOTIFY_DATA_CHANGED_MS: Long = 100
        const val ACTION_UPDATE_ITEM_VIEW = "ACTION_UPDATE_ITEM_VIEW"
    }

    private val bluetoothReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_TURNING_OFF -> {
                        context.let { for (item in AdvertiserList.getList(context)) item.stop() }
                        advertiserAdapter.notifyDataSetChanged()
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
        setContentView(R.layout.activity_advertiser)
        prepareToolbar()
        prepareCustomComponents()
        presenter = AdvertiserActivityPresenter(this, AdvertiserStorage(this), AdvertiserList.getList(this@AdvertiserActivity))
        presenter.populateAdvertiserAdapter()

        if (isBluetoothAdapterEnabled) presenter.checkExtendedAdvertisingSupported()
        bluetooth_enable_btn.setOnClickListener { bluetoothEnableBar.changeEnableBluetoothAdapterToConnecting() }

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothReceiver, filter)
    }

    private fun prepareToolbar() {
        setSupportActionBar(toolbar)
        iv_go_back.setOnClickListener { onBackPressed() }
        toolbar.overflowIcon = ContextCompat.getDrawable(this, R.drawable.ic_menu)
    }

    private fun prepareCustomComponents() {
        bluetoothEnableBar = findViewById(R.id.bluetooth_enable)
        fullScreenInfo = findViewById(R.id.full_screen_info_no_advertisers)
        fullScreenInfo.initialize(R.drawable.ic_advertiser, getString(R.string.advertiser_label_no_configured_advertisers))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_advertiser, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.device_name -> {
                DeviceNameDialog(object : DeviceNameDialog.DeviceNameCallback {
                    override fun onDismiss() {
                        Handler().postDelayed({
                            advertiserAdapter.notifyDataSetChanged()
                        }, DELAY_NOTIFY_DATA_CHANGED_MS)
                    }
                }).show(supportFragmentManager, "dialog_device_name")
                true
            }
            R.id.create_new -> { presenter.createNewItem(); true }
            R.id.switch_all_off -> { presenter.switchAllItemsOff(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCopyClick(item: Advertiser) { presenter.copyItem(item) }
    override fun onEditClick(position: Int) { presenter.editItem(position) }
    override fun onRemoveClick(position: Int) {

        if (AdvertiserStorage(this@AdvertiserActivity).shouldDisplayRemoveAdvertiserDialog()) {
            RemoveAdvertiserDialog(object : RemoveAdvertiserDialog.Callback {
                override fun onOkClicked() {
                    presenter.removeItem(position)
                }
            }).show(supportFragmentManager, "dialog_remove_advertiser")
        } else presenter.removeItem(position)

    }

    override fun onAdvertiserPopulated(items: ArrayList<Advertiser>) {
        rv_advertiser.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        advertiserAdapter = AdvertiserAdapter(items, this, this)
        rv_advertiser.adapter = advertiserAdapter
        handleFullScreenInfoChanges()
    }

    override fun onCopyClicked(position: Int) {
        advertiserAdapter.notifyItemInserted(position)
        advertiserAdapter.notifyItemRangeChanged(position - 1, 2, Unit)
    }

    override fun onItemCreated(position: Int) {
        advertiserAdapter.notifyItemInserted(position)
        advertiserAdapter.notifyItemRangeChanged(position - 1, 2, Unit)
        handleFullScreenInfoChanges()
    }

    override fun showEditIntent(item: Advertiser) {
        val intent = AdvertiserConfigActivity.newIntent(item.data, advertiserAdapter.items.indexOf(item), this)
        startActivityForResult(intent, REQUEST_CODE_EDIT)
    }

    override fun onItemRemoved(position: Int) {
        advertiserAdapter.notifyItemRemoved(position)
        advertiserAdapter.notifyItemRangeChanged(position - 1, 2, Unit)
        handleFullScreenInfoChanges()
    }

    override fun refreshItem(position: Int) {
        val intent = Intent()
        intent.action = ACTION_UPDATE_ITEM_VIEW
        intent.putExtra(EXTRA_ITEM_POSITION, position)
        sendBroadcast(intent)
    }

    override fun switchItemOn(position: Int) {
        presenter.switchItemOn(position)
    }

    override fun switchItemOff(position: Int) {
        presenter.switchItemOff(position)
    }

    override fun onResume() {
        super.onResume()
        advertiserAdapter.notifyDataSetChanged()

        isBluetoothAdapterEnabled = BluetoothAdapter.getDefaultAdapter()?.isEnabled ?: false
        if (!isBluetoothAdapterEnabled) bluetoothEnableBar.show() else bluetoothEnableBar.hide()
    }

    override fun onStop() {
        super.onStop()
        presenter.persistData()
        unregisterReceiver(broadcastReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)
    }

    override fun startAdvertiserService() {
        AdvertiserService.startService(this@AdvertiserActivity)
    }

    override fun stopAdvertiserService() {
        AdvertiserService.stopService(this@AdvertiserActivity)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_EDIT) {
            val advertiserData = data?.extras?.getParcelable<AdvertiserData>(AdvertiserConfigActivity.EXTRA_ADVERTISER_ITEM) as AdvertiserData
            val position = data.getIntExtra(EXTRA_ITEM_POSITION, 0)

            advertiserAdapter.items[position].data = advertiserData
            advertiserAdapter.notifyItemChanged(position)
        }
    }

    private fun handleFullScreenInfoChanges() {
        if (advertiserAdapter.isEmpty()) {
            fullScreenInfo.show()
            rv_advertiser.visibility = View.INVISIBLE
        } else {
            fullScreenInfo.hide()
            rv_advertiser.visibility = View.VISIBLE
        }
    }

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_UPDATE_ITEM_VIEW)
        registerReceiver(broadcastReceiver, intentFilter)
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val position = intent?.getIntExtra(EXTRA_ITEM_POSITION, -1)
            position?.let { if (position != -1) rv_advertiser?.post { advertiserAdapter.notifyItemChanged(position, Unit) } }
        }
    }

}
