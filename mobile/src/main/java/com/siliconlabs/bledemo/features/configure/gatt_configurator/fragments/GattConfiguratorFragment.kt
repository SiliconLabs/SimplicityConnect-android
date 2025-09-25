package com.siliconlabs.bledemo.features.configure.gatt_configurator.fragments

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.bluetooth.services.BluetoothService
import com.siliconlabs.bledemo.features.configure.gatt_configurator.adapters.GattServerAdapter
import com.siliconlabs.bledemo.features.configure.gatt_configurator.adapters.GattServerAdapter.OnClickListener
import com.siliconlabs.bledemo.features.configure.gatt_configurator.models.GattServer
import com.siliconlabs.bledemo.features.configure.gatt_configurator.viewmodels.GattConfiguratorViewModel
import com.siliconlabs.bledemo.features.configure.gatt_configurator.views.ExportBar
import com.siliconlabs.bledemo.features.configure.gatt_configurator.activities.GattServerActivity
import com.siliconlabs.bledemo.databinding.FragmentGattConfiguratorBinding
import com.siliconlabs.bledemo.features.configure.gatt_configurator.import_export.GattServerExporter
import com.siliconlabs.bledemo.features.configure.gatt_configurator.import_export.GattServerImporter
import com.siliconlabs.bledemo.features.configure.gatt_configurator.import_export.ImportException
import com.siliconlabs.bledemo.features.configure.gatt_configurator.utils.removeAsking
import com.siliconlabs.bledemo.home_screen.activities.MainActivity
import com.siliconlabs.bledemo.common.other.CardViewListDecoration
import com.siliconlabs.bledemo.common.other.LinearLayoutManagerWithHidingUIElements
import com.siliconlabs.bledemo.common.other.WithHidableUIElements
import com.siliconlabs.bledemo.home_screen.base.BaseMainMenuFragment
import com.siliconlabs.bledemo.utils.CustomToastManager
import dagger.hilt.android.AndroidEntryPoint
import java.io.*
import java.util.*

@AndroidEntryPoint
class GattConfiguratorFragment : BaseMainMenuFragment(), OnClickListener {
    private lateinit var viewModel: GattConfiguratorViewModel
    private lateinit var viewBinding: FragmentGattConfiguratorBinding
    private lateinit var adapter: GattServerAdapter
    private var service: BluetoothService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        service = (activity as MainActivity).bluetoothService
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        setHasOptionsMenu(true)
        viewBinding = FragmentGattConfiguratorBinding.inflate(inflater)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(GattConfiguratorViewModel::class.java)
        hidableActionButton = viewBinding.fragmentMainView.extendedFabMainView

        initMainViewValues()
        setUiListeners()
        observeChanges()
        initExportBar()
        initAdapter()
    }

    private fun initMainViewValues() {
        viewBinding.fragmentMainView.fullScreenInfo.apply {
            image.setImageResource(R.drawable.redesign_ic_main_view_gatt_configurator)
            textPrimary.text = getString(R.string.text_gatt_configurator_purpose_explanation)
            textSecondary.text = getString(R.string.text_gatt_configurator_no_configured_gatt_servers)
        }
        viewBinding.fragmentMainView.extendedFabMainView.text = getString(R.string.btn_create_new)
    }

    private fun setUiListeners() {
        viewBinding.fragmentMainView.extendedFabMainView.setOnClickListener {
            viewModel.createGattServer()
        }
    }

    private fun observeChanges() {
        viewModel.removedPosition.observe(viewLifecycleOwner, Observer { position ->
            adapter.notifyItemRemoved(position)
            adapter.notifyItemRangeChanged(position - 1, 2, Unit)
        })

        viewModel.insertedPosition.observe(viewLifecycleOwner, Observer { position ->
            adapter.notifyItemInserted(position)
            adapter.notifyItemRangeChanged(position - 1, 2, Unit)
        })

        viewModel.changedPosition.observe(viewLifecycleOwner, Observer { position ->
            adapter.notifyItemChanged(position, Unit)
        })

        viewModel.areAnyGattServers.observe(viewLifecycleOwner, Observer { areAnyGattServers ->
            toggleNoGattServersInfo(areAnyGattServers)
        })

        viewModel.switchedOffPosition.observe(viewLifecycleOwner, Observer { position ->
            viewBinding.fragmentMainView.rvMainView.post {
                adapter.notifyItemChanged(position, Unit)
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_gatt_configurator, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
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

    private fun toggleNoGattServersInfo(areAnyGattServers: Boolean) {
        viewBinding.fragmentMainView.apply {
            if (areAnyGattServers) {
                fullScreenInfo.root.visibility = View.GONE
                rvMainView.visibility = View.VISIBLE
            } else {
                fullScreenInfo.root.visibility = View.VISIBLE
                rvMainView.visibility = View.GONE
                restoreHiddenUI()
            }
        }
    }

    private fun toggleExportMode(enable: Boolean) {
        adapter.setExportMode(enable)
        viewBinding.fragmentMainView.extendedFabMainView.visibility = if (enable) View.GONE else View.VISIBLE
        toggleExportBarVisibility(enable)
        if (!enable) {
            viewModel.gattServers.value?.forEach { it.isCheckedForExport = false }
        }
        viewBinding.bottomBarExport.setExportBtnEnabled(viewModel.isAnyGattServerCheckedForExport())
    }

    private fun toggleExportBarVisibility(makeVisible: Boolean) {
        viewBinding.bottomBarExport.visibility =
                if (makeVisible) View.VISIBLE
                else View.GONE
    }

    private fun initExportBar() {
        viewBinding.bottomBarExport.init(object : ExportBar.Listener {
            override fun onExportClick() {
                openLocationChooser()
                /*Toast.makeText(
                        activity,
                        getString(R.string.gatt_configurator_toast_export_location_choice),
                        Toast.LENGTH_SHORT).show()*/
                CustomToastManager.show(
                    requireContext(),getString(R.string.gatt_configurator_toast_export_location_choice),
                    5000
                )
            }

            override fun onCancelClick() {
                toggleExportMode(false)
            }
        })
    }

    private fun initAdapter() {
        adapter = GattServerAdapter(viewModel.gattServers.value!!, this)
        viewBinding.fragmentMainView.rvMainView.apply {
            layoutManager = getLayoutManagerWithHidingUIElements(activity)
            addItemDecoration(CardViewListDecoration())
            adapter = this@GattConfiguratorFragment.adapter
        }
    }

    override fun onCopyClick(gattServer: GattServer) {
        viewModel.copyGattServer(gattServer)
    }

    override fun onEditClick(position: Int, gattServer: GattServer) {
        viewModel.switchGattServerOffAt(position)
        startGattServerActivityForResult(requireActivity(), position, gattServer)
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
        viewBinding.bottomBarExport.setExportBtnEnabled(viewModel.isAnyGattServerCheckedForExport())
    }

    private fun openFileChooser() {
        Intent(ACTION_GET_CONTENT)
            .apply { type = "text/xml" }
            .also { startActivityForResult(createChooser(it,
                getString(R.string.gatt_configurator_choose_file_for_import)), IMPORT_GATT_SERVER_CODE) }
    }

    private fun openLocationChooser() {
        Intent(ACTION_OPEN_DOCUMENT_TREE).also {
            startActivityForResult(it, EXPORT_GATT_SERVER_CODE)
        }
    }

    private fun startGattServerActivityForResult(activity: FragmentActivity, position: Int, gattServer: GattServer) {
        val intent = Intent(activity, GattServerActivity::class.java).apply {
            putExtra(GattServerActivity.EXTRA_GATT_SERVER_POSITION, position)
            putExtra(GattServerActivity.EXTRA_GATT_SERVER, gattServer)
        }
        startActivityForResult(intent, REQUEST_CODE_EDIT_GATT_SERVER, null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_EDIT_GATT_SERVER -> refreshGattServerInfo(intent)
                IMPORT_GATT_SERVER_CODE -> onImportFileChosen(intent)
                EXPORT_GATT_SERVER_CODE -> onExportLocationChosen(intent)
            }
        }
    }

    private fun refreshGattServerInfo(intent: Intent?) {
        val position = intent?.getIntExtra(GattServerActivity.EXTRA_GATT_SERVER_POSITION, -1)
        val gattServer = intent?.getParcelableExtra<GattServer>(
                GattServerActivity.EXTRA_GATT_SERVER)

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
            DocumentFile.fromTreeUri(activity?.applicationContext!!, it)?.let { location ->
                GattServerExporter().export(viewModel.gattServers.value?.filter { server ->
                    server.isCheckedForExport
                }!!
                ).forEach { server ->
                    location.createFile("text/xml", server.key)?.let { singleFile ->
                        activity?.contentResolver?.openOutputStream(singleFile.uri)?.let { stream ->
                            stream.write(server.value.toByteArray())
                            stream.close()
                        }
                    }
                }
                /*Toast.makeText(activity, getString(R.string
                        .gatt_configurator_toast_export_successful), Toast.LENGTH_LONG).show()*/
                CustomToastManager.show(
                    requireContext(),getString(R.string
                        .gatt_configurator_toast_export_successful),
                    5000
                )
            } ?: showWrongLocationToast()
        } ?: showWrongLocationToast()

        toggleExportMode(false)
    }

    private fun showFileNotFoundToast() {
        /*Toast.makeText(activity, getString(R.string.gatt_configurator_toast_import_file_not_found),
                Toast.LENGTH_LONG).show()*/
        CustomToastManager.show(requireContext(),getString(R.string.gatt_configurator_toast_import_file_not_found),5000)
    }

    private fun showWrongLocationToast() {
        /*Toast.makeText(activity, getString(R.string
                .toast_export_wrong_location_chosen), Toast.LENGTH_LONG).show()*/
        CustomToastManager.show(requireContext(),getString(R.string
            .toast_export_wrong_location_chosen),5000)
    }

    private fun getFileDescriptorFromIntent(intent: Intent?) : FileDescriptor? {
        return intent?.data?.let {
            try {
                activity?.contentResolver?.openFileDescriptor(it, "r")
            } catch (err: FileNotFoundException) {
                showFileNotFoundToast()
                null
            }?.fileDescriptor
        }
    }

    private fun showImportDialog(isSuccessful: Boolean, err: ImportException? = null) {
        AlertDialog.Builder(activity).let {
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
            val intent = Intent(context, GattConfiguratorFragment::class.java)
            startActivity(context, intent, null)
        }

        private const val REQUEST_CODE_EDIT_GATT_SERVER = 8765
        private const val IMPORT_GATT_SERVER_CODE = 3012
        private const val EXPORT_GATT_SERVER_CODE = 3013
    }

    private fun convertImportErrorMessage(err: ImportException?) : String {
        val message = StringBuffer()
        message.append(err?.errorType.toString().first())
        message.append(err?.errorType.toString().substring(1)
                .lowercase(Locale.getDefault()).replace("_", " "))
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
