package com.siliconlabs.bledemo.gatt_configurator.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.Base.BaseActivity
import com.siliconlabs.bledemo.gatt_configurator.adapters.EditGattServerAdapter
import com.siliconlabs.bledemo.gatt_configurator.adapters.EditGattServerAdapter.AddServiceListener
import com.siliconlabs.bledemo.gatt_configurator.adapters.EditGattServerAdapter.ServiceListener
import com.siliconlabs.bledemo.gatt_configurator.dialogs.LeaveGattServerConfigDialog
import com.siliconlabs.bledemo.gatt_configurator.dialogs.ServiceDialog
import com.siliconlabs.bledemo.gatt_configurator.dialogs.ServiceDialog.ServiceChangeListener
import com.siliconlabs.bledemo.gatt_configurator.models.GattServer
import com.siliconlabs.bledemo.gatt_configurator.models.Service
import com.siliconlabs.bledemo.gatt_configurator.utils.GattConfiguratorStorage
import com.siliconlabs.bledemo.gatt_configurator.utils.removeAsking
import com.siliconlabs.bledemo.gatt_configurator.viewmodels.GattServerViewModel
import com.siliconlabs.bledemo.gatt_configurator.viewmodels.GattServerViewModel.Validation
import com.siliconlabs.bledemo.other.EqualVerticalItemDecoration
import kotlinx.android.synthetic.main.actionbar.*
import kotlinx.android.synthetic.main.activity_gatt_server.*

class GattServerActivity : BaseActivity(), ServiceListener, AddServiceListener {
    private lateinit var viewModel: GattServerViewModel
    private lateinit var adapter: EditGattServerAdapter

    private var savedServerState: GattServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gatt_server)
        viewModel = ViewModelProvider(this).get(GattServerViewModel::class.java)

        initViewModel()
        initAdapter()
        prepopulateFields()
        prepareToolbar()
        observeChanges()
        handleGattServerNameChanges()

        savedServerState = viewModel.getGattServer()?.deepCopy()
    }

    private fun observeChanges() {
        viewModel.insertedPosition.observe(this, Observer { position ->
            adapter.notifyItemInserted(position)
        })

        viewModel.removedPosition.observe(this, Observer { position ->
            adapter.notifyItemRemoved(position)
        })

        viewModel.changedPosition.observe(this, Observer { position ->
            adapter.notifyItemChanged(position)
        })

        viewModel.validation.observe(this, Observer {
            when (it) {
                Validation.INVALID_NAME -> Toast.makeText(this, R.string.gatt_configurator_toast_invalid_gatt_server_name, Toast.LENGTH_SHORT).show()
                else -> saveGattServer()
            }
        })
    }

    private fun initViewModel() {
        val position = intent.getIntExtra(EXTRA_GATT_SERVER_POSITION, -1)
        val gattServer = intent.getParcelableExtra<GattServer>(EXTRA_GATT_SERVER)

        gattServer?.let {
            viewModel.init(position, gattServer)
        }
    }

    private fun handleGattServerNameChanges() {
        et_gatt_server_name.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                title = s.toString()
                viewModel.getGattServer()?.name = s.toString()
            }
        })
    }

    private fun prepopulateFields() {
        viewModel.getGattServerName()?.let { name ->
            et_gatt_server_name.setText(name)
        }
    }

    private fun prepareToolbar() {
        setSupportActionBar(toolbar)
        title = viewModel.getGattServerName()
        findViewById<View>(R.id.iv_go_back).setOnClickListener { onBackPressed() }
    }

    private fun exitServerConfigView() {
        if (hasConfigurationChanged() &&
                GattConfiguratorStorage(this).shouldDisplayLeaveGattServerConfigDialog()) {
            LeaveGattServerConfigDialog(object : LeaveGattServerConfigDialog.Callback {
                override fun onYesClicked() {
                    saveGattServer()
                }

                override fun onNoClicked() {
                    super@GattServerActivity.onBackPressed()
                }

            }).show(supportFragmentManager, "dialog_leave_gatt_server_config")
        }
        else super@GattServerActivity.onBackPressed()
    }

    private fun hasConfigurationChanged() : Boolean {
        return savedServerState != viewModel.getGattServer()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_edit_gatt_server, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.save_gatt_server -> {
                viewModel.validateGattServer(et_gatt_server_name.text.toString())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveGattServer() {
        viewModel.setGattServerName(et_gatt_server_name.text.toString())
        val intent = Intent().apply {
            putExtra(EXTRA_GATT_SERVER_POSITION, viewModel.getPosition()!!)
            putExtra(EXTRA_GATT_SERVER, viewModel.getGattServer())
        }

        setResult(RESULT_OK, intent)
        finish()
    }

    private fun initAdapter() {
        adapter = EditGattServerAdapter(viewModel.getServiceList()!!, this, this)
        rv_edit_gatt_server.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        rv_edit_gatt_server.addItemDecoration(EqualVerticalItemDecoration(resources.getDimensionPixelSize(R.dimen.edit_gatt_server_adapter_margin)))
        rv_edit_gatt_server.adapter = adapter
    }

    override fun onCopyService(service: Service) {
        viewModel.copyService(service)
    }

    override fun onRemoveService(position: Int) {
        removeAsking(R.string.service) {
            viewModel.removeServiceAt(position)
        }
    }

    override fun onAddService() {
        ServiceDialog(object : ServiceChangeListener {
            override fun onServiceChanged(service: Service) {
                viewModel.addService(service)
            }
        }).show(supportFragmentManager, "dialog_service")
    }

    override fun onBackPressed() {
        exitServerConfigView()
    }

    companion object {
        fun startActivityForResult(activity: FragmentActivity, position: Int, gattServer: GattServer) {
            val intent = Intent(activity, GattServerActivity::class.java).apply {
                putExtra(EXTRA_GATT_SERVER_POSITION, position)
                putExtra(EXTRA_GATT_SERVER, gattServer)
            }
            ActivityCompat.startActivityForResult(activity, intent, REQUEST_CODE_EDIT_GATT_SERVER, null)
        }

        const val REQUEST_CODE_EDIT_GATT_SERVER = 8765
        const val EXTRA_GATT_SERVER_POSITION = "EXTRA_GATT_SERVER_POSITION"
        const val EXTRA_GATT_SERVER = "EXTRA_GATT_SERVER"
    }
}
