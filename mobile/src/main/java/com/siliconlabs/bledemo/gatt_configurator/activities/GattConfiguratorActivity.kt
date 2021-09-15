package com.siliconlabs.bledemo.gatt_configurator.activities

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.documentfile.provider.DocumentFile
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
import com.siliconlabs.bledemo.Views.ExportBar
import com.siliconlabs.bledemo.gatt_configurator.import_export.GattServerExporter
import com.siliconlabs.bledemo.gatt_configurator.import_export.GattServerImporter
import com.siliconlabs.bledemo.gatt_configurator.import_export.ImportException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.actionbar.*
import kotlinx.android.synthetic.main.activity_gatt_configurator.*
import kotlinx.android.synthetic.main.view_export_bar.view.*
import java.io.*
import java.util.*

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
        initExportBar()
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
                toggleExportMode(false)
                viewModel.createGattServer()
                true
            }
            R.id.import_xml -> {
                toggleExportMode(false)
                openFileChooser()
                true
            }
            R.id.export_xml -> {
                toggleExportMode(true)
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

    private fun toggleExportMode(enable: Boolean) {
        adapter.setExportMode(enable)
        toggleExportBarVisibility(enable)
        if (!enable) {
            viewModel.gattServers.value?.forEach { it.isCheckedForExport = false }
        }
        bottom_bar_export.export_bar.isEnabled = viewModel.isAnyGattServerCheckedForExport()
    }

    private fun toggleExportBarVisibility(makeVisible: Boolean) {
        if (makeVisible) {
            bottom_bar_export_shadow.visibility = View.VISIBLE
            bottom_bar_export.show()
        }
        else {
            bottom_bar_export_shadow.visibility = View.INVISIBLE
            bottom_bar_export.hide()
        }
    }

    private fun initExportBar() {
        bottom_bar_export.init(object : ExportBar.Listener {
            override fun onExportClick() {
                openLocationChooser()
                Toast.makeText(
                        this@GattConfiguratorActivity,
                        getString(R.string.gatt_configurator_toast_export_location_choice),
                        Toast.LENGTH_SHORT).show()
            }

            override fun onCancelClick() {
                toggleExportMode(false)
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
        service?.setGattServer()
    }

    override fun switchItemOff(position: Int) {
        viewModel.switchGattServerOffAt(position)
        if (!viewModel.isAnyGattServerSwitchedOn()) {
            service?.clearGattServer()
        }
    }

    override fun onExportBoxClick() {
        bottom_bar_export.export_bar.isEnabled = viewModel.isAnyGattServerCheckedForExport()
    }

    private fun openFileChooser() {
        val requestFileIntent = Intent(ACTION_GET_CONTENT)
        requestFileIntent.type = "text/xml"
        startActivityForResult(requestFileIntent, IMPORT_GATT_SERVER_CODE)
    }

    private fun openLocationChooser() {
        val requestLocationIntent = Intent(ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(requestLocationIntent, EXPORT_GATT_SERVER_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                GattServerActivity.REQUEST_CODE_EDIT_GATT_SERVER -> refreshGattServerInfo(intent)
                IMPORT_GATT_SERVER_CODE -> onImportFileChosen(intent)
                EXPORT_GATT_SERVER_CODE -> onExportLocationChosen(intent)
            }
        }
    }

    private fun refreshGattServerInfo(intent: Intent?) {
        val position = intent?.getIntExtra(GattServerActivity.EXTRA_GATT_SERVER_POSITION, -1)
        val gattServer = intent?.getParcelableExtra<GattServer>(GattServerActivity.EXTRA_GATT_SERVER)

        if (position != -1 && gattServer != null) {
            viewModel.replaceGattServerAt(position!!, gattServer)
        }
    }

    private fun onImportFileChosen(intent: Intent?) {
        getFileDescriptorFromIntent(intent)?.let {
            try {
                val importedServer = GattServerImporter(BufferedReader(FileReader(it))).readFile()
                viewModel.createGattServer(importedServer)
                showImportDialog(true)
            } catch (err: ImportException) {
                showImportDialog(false, err)
            }
        } ?: showFileNotFoundToast()
    }

    private fun onExportLocationChosen(intent: Intent?) {
        intent?.data?.let {
            DocumentFile.fromTreeUri(applicationContext, it)?.let { location ->
                GattServerExporter().export(
                        viewModel.gattServers.value?.filter { it.isCheckedForExport }!!
                ).forEach { server ->
                    location.createFile("text/xml", server.key)?.let { singleFile ->
                        contentResolver.openOutputStream(singleFile.uri)?.write(server.value.toByteArray())
                    }
                }
                Toast.makeText(this, getString(R.string
                        .gatt_configurator_toast_export_successful), Toast.LENGTH_LONG).show()
            } ?: showWrongLocationToast()
        } ?: showWrongLocationToast()

        toggleExportMode(false)
    }

    private fun showFileNotFoundToast() {
        Toast.makeText(this, getString(R.string.gatt_configurator_toast_import_file_not_found), Toast.LENGTH_LONG).show()
    }

    private fun showWrongLocationToast() {
        Toast.makeText(this, getString(R.string
                .gatt_configurator_toast_export_wrong_location_chosen), Toast.LENGTH_LONG).show()
    }

    private fun getFileDescriptorFromIntent(intent: Intent?) : FileDescriptor? {
        return intent?.data?.let {
            try {
                contentResolver.openFileDescriptor(it, "r")
            } catch (err: FileNotFoundException) {
                showFileNotFoundToast()
                null
            }?.fileDescriptor
        }
    }

    private fun showImportDialog(isSuccessful: Boolean, err: ImportException? = null) {
        AlertDialog.Builder(this).let {
            if (isSuccessful) {
                it.setTitle(R.string.gatt_configurator_popup_import_successful_title)
                  .setMessage(R.string.gatt_configurator_popup_import_successful_content)

            } else {
                it.setTitle(R.string.gatt_configurator_popup_import_unsuccessful_title)
                  .setMessage(convertImportErrorMessage(err))
            }
            it.setNeutralButton(R.string.button_ok) { dialog, _ -> dialog.dismiss() }
              .show()
        }
    }

    companion object {
        fun startActivity(context: Context) {
            val intent = Intent(context, GattConfiguratorActivity::class.java)
            startActivity(context, intent, null)
        }

        private const val IMPORT_GATT_SERVER_CODE = 3012
        private const val EXPORT_GATT_SERVER_CODE = 3013
    }

    private fun convertImportErrorMessage(err: ImportException?) : String {
        val message = StringBuffer()
        message.append(err?.errorType.toString().first())
        message.append(err?.errorType.toString().substring(1)
                .toLowerCase(Locale.getDefault()).replace("_", " "))
        message.append(". ")

        when (err?.errorType) {
            ImportException.ErrorType.WRONG_TAG_NAME,
            ImportException.ErrorType.WRONG_ATTRIBUTE_NAME,
            ImportException.ErrorType.WRONG_ATTRIBUTE_VALUE,
            ImportException.ErrorType.WRONG_CAPABILITY_LISTED,
            ImportException.ErrorType.WRONG_INCLUDE_ID_DECLARED,
            ImportException.ErrorType.PROPERTY_NOT_SUPPORTED_BY_DESCRIPTOR -> {
                message.append(getString(
                        R.string.gatt_configurator_popup_import_unsuccessful_content_forbidden_value,
                        err.provided, err.expected))
            }

            ImportException.ErrorType.ATTRIBUTE_NAME_DUPLICATED,
            ImportException.ErrorType.TAG_MAXIMUM_OCCURRENCE_EXCEEDED,
            ImportException.ErrorType.MANDATORY_ATTRIBUTE_MISSING -> {
                message.append(getString(
                        R.string.gatt_configurator_popup_import_unsuccessful_content_occurrence_error,
                        err.provided))
            }

            ImportException.ErrorType.WRONG_TAG_VALUE -> {
                message.append(getString(
                        R.string.gatt_configurator_popup_import_unsuccessful_content_regex_mismatch,
                        err.provided, err.expected))
            }

            else -> { }
        }
        return message.toString()
    }
}
