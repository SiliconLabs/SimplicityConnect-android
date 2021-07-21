package com.siliconlabs.bledemo.gatt_configurator.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.Base.BaseActivity
import com.siliconlabs.bledemo.Bluetooth.Services.BluetoothService
import com.siliconlabs.bledemo.gatt_configurator.adapters.GattServerAdapter
import com.siliconlabs.bledemo.gatt_configurator.adapters.GattServerAdapter.OnClickListener
import com.siliconlabs.bledemo.gatt_configurator.models.GattServer
import com.siliconlabs.bledemo.gatt_configurator.utils.removeAsking
import com.siliconlabs.bledemo.gatt_configurator.viewmodels.GattConfiguratorViewModel
import com.siliconlabs.bledemo.Views.BottomBarWithButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.actionbar.*
import kotlinx.android.synthetic.main.activity_gatt_configurator.*

@AndroidEntryPoint
class GattConfiguratorActivity : BaseActivity(), OnClickListener {
    private lateinit var viewModel: GattConfiguratorViewModel
    private lateinit var adapter: GattServerAdapter
    private lateinit var binding: BluetoothService.Binding
    private var service: BluetoothService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gatt_configurator)
        viewModel = ViewModelProvider(this).get(GattConfiguratorViewModel::class.java)

        prepareToolbar()
        observeChanges()
        initFullScreenInfo()
        initBottomBar()
        initAdapter()
        bindBluetoothService()
    }

    private fun bindBluetoothService() {
        binding = object : BluetoothService.Binding(this) {
            override fun onBound(service: BluetoothService?) {
                this@GattConfiguratorActivity.service = service
            }
        }
        binding.bind()
    }

    private fun observeChanges() {
        viewModel.removedPosition.observe(this, Observer { position ->
            adapter.notifyItemRemoved(position)
            adapter.notifyItemRangeChanged(position - 1, 2, Unit)
        })

        viewModel.insertedPosition.observe(this, Observer { position ->
            adapter.notifyItemInserted(position)
            adapter.notifyItemRangeChanged(position - 1, 2, Unit)
        })

        viewModel.changedPosition.observe(this, Observer { position ->
            adapter.notifyItemChanged(position, Unit)
        })

        viewModel.areAnyGattServers.observe(this, Observer { areAnyGattServers ->
            showOrHideNoGattServersInfo(areAnyGattServers)
        })

        viewModel.switchedOffPosition.observe(this, Observer { position ->
            rv_gatt_servers.post {
                adapter.notifyItemChanged(position, Unit)
            }
        })
    }

    private fun prepareToolbar() {
        setSupportActionBar(toolbar)
        findViewById<View>(R.id.iv_go_back).setOnClickListener { onBackPressed() }
        toolbar.overflowIcon = ContextCompat.getDrawable(this, R.drawable.ic_menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.create_new -> {
                viewModel.createGattServer()
                true
            }
            R.id.import_xml -> {
                adapter.setExportMode(false)
                Toast.makeText(this, "Import .xml file", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.export_xml -> {
                adapter.setExportMode(true)
                //Toast.makeText(this, "Export .xml file", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.unbind()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_gatt_configurator, menu)
        return true
    }

    private fun initFullScreenInfo() {
        full_screen_info_no_gatt_servers.initialize(R.drawable.ic_gatt_configurator, getString(R.string.gatt_configurator_label_no_configured_gatt_servers))
    }

    private fun showOrHideNoGattServersInfo(areAnyGattServers: Boolean) {
        if (areAnyGattServers) {
            hideNoGattServersInfo()
        } else {
            showNoGattServersInfo()
        }
    }

    private fun showNoGattServersInfo() {
        full_screen_info_no_gatt_servers.show()
        rv_gatt_servers.visibility = View.INVISIBLE
    }

    private fun hideNoGattServersInfo() {
        full_screen_info_no_gatt_servers.hide()
        rv_gatt_servers.visibility = View.VISIBLE
    }

    private fun initBottomBar() {
        bottom_bar_export.init(getString(R.string.button_export), object : BottomBarWithButton.Listener {
            override fun onClick() {
                Toast.makeText(this@GattConfiguratorActivity, "Export", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun initAdapter() {
        adapter = GattServerAdapter(viewModel.gattServers.value!!, this)
        rv_gatt_servers.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        rv_gatt_servers.adapter = adapter
    }

    override fun onCopyClick(gattServer: GattServer) {
        viewModel.copyGattServer(gattServer)
    }

    override fun onEditClick(position: Int, gattServer: GattServer) {
        viewModel.switchGattServerOffAt(position)
        GattServerActivity.startActivityForResult(this, position, gattServer)
    }

    override fun onRemoveClick(position: Int) {
        removeAsking(R.string.server) {
            viewModel.removeGattServerAt(position)
        }
    }

    override fun switchItemOn(position: Int) {
        viewModel.switchGattServerOnAt(position)
    }

    override fun switchItemOff(position: Int) {
        viewModel.switchGattServerOffAt(position)
    }

    override fun onBackPressed() {
        if (viewModel.isAnyGattServerSwitchedOn()) {
            service?.setGattServer()
        } else {
            service?.clearGattServer()
        }
        super.onBackPressed()
    }

    override fun onPause() {
        super.onPause()
        viewModel.persistGattServers()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (resultCode == RESULT_OK && requestCode == GattServerActivity.REQUEST_CODE_EDIT_GATT_SERVER) {
            val position = intent?.getIntExtra(GattServerActivity.EXTRA_GATT_SERVER_POSITION, -1)
            val gattServer = intent?.getParcelableExtra<GattServer>(GattServerActivity.EXTRA_GATT_SERVER)

            if (position != -1 && gattServer != null) {
                viewModel.replaceGattServerAt(position!!, gattServer)
            }
        }
    }

    companion object {
        fun startActivity(context: Context) {
            val intent = Intent(context, GattConfiguratorActivity::class.java)
            startActivity(context, intent, null)
        }
    }
}
