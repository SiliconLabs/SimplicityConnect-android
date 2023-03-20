package com.siliconlabs.bledemo.features.configure.advertiser.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.siliconlabs.bledemo.features.configure.advertiser.activities.AdvertiserConfigActivity
import com.siliconlabs.bledemo.features.configure.advertiser.adapters.AdvertiserAdapter
import com.siliconlabs.bledemo.features.configure.advertiser.viewmodels.AdvertiserViewModel
import com.siliconlabs.bledemo.features.configure.advertiser.dialogs.DeviceNameDialog
import com.siliconlabs.bledemo.features.configure.advertiser.dialogs.RemoveAdvertiserDialog
import com.siliconlabs.bledemo.features.configure.advertiser.models.Advertiser
import com.siliconlabs.bledemo.features.configure.advertiser.models.AdvertiserData
import com.siliconlabs.bledemo.features.configure.advertiser.services.AdvertiserService
import com.siliconlabs.bledemo.features.configure.advertiser.utils.AdvertiserStorage
import com.siliconlabs.bledemo.bluetooth.services.BluetoothService
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.FragmentAdvertiserBinding
import com.siliconlabs.bledemo.home_screen.activities.MainActivity
import com.siliconlabs.bledemo.common.other.CardViewListDecoration
import com.siliconlabs.bledemo.home_screen.base.BaseServiceDependentMainMenuFragment
import com.siliconlabs.bledemo.home_screen.base.BluetoothDependent


class AdvertiserFragment : BaseServiceDependentMainMenuFragment(), AdvertiserAdapter.OnItemClickListener {

    private var advertiserAdapter: AdvertiserAdapter? = null
    private var viewModel: AdvertiserViewModel? = null
    private lateinit var viewBinding: FragmentAdvertiserBinding
    private lateinit var service: BluetoothService


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        setHasOptionsMenu(true)
        viewBinding = FragmentAdvertiserBinding.inflate(inflater)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
                viewModelStore,
                AdvertiserViewModel.Factory(AdvertiserStorage(requireContext()))
        ).get(AdvertiserViewModel::class.java)
        service = (activity as MainActivity).bluetoothService!!

        initMainViewValues()
        setUiListeners()
        observeChanges()
        initAdapter()
    }

    private fun initMainViewValues() {
        viewBinding.fragmentMainView.fullScreenInfo.apply {
            image.setImageResource(R.drawable.redesign_ic_main_view_advertiser)
            textPrimary.text = getString(R.string.text_advertiser_purpose_explanation)
            textSecondary.text = getString(R.string.advertiser_label_no_configured_advertisers)
        }
        viewBinding.fragmentMainView.extendedFabMainView.text = getString(R.string.btn_create_new)
    }

    private fun setUiListeners() {
        viewBinding.fragmentMainView.extendedFabMainView.setOnClickListener { viewModel?.createAdvertiser() }
    }

    private fun observeChanges() {
        viewModel?.areAnyAdvertisersOn?.observe(viewLifecycleOwner, Observer { areAnyAdvertisersOn ->
            toggleAdvertisingService(areAnyAdvertisersOn)
        })
        viewModel?.areAnyAdvertisers?.observe(viewLifecycleOwner, Observer { areAnyAdvertisers ->
            toggleMainView(areAnyAdvertisers)
        })
        viewModel?.insertedPosition?.observe(viewLifecycleOwner, Observer { position ->
            advertiserAdapter?.notifyItemInserted(position)
            advertiserAdapter?.notifyItemRangeChanged(position - 1, 2, Unit)
        })
        viewModel?.removedPosition?.observe(viewLifecycleOwner, Observer { position ->
            advertiserAdapter?.notifyItemRemoved(position)
            advertiserAdapter?.notifyItemRangeChanged(position - 1, 2, Unit)
        })
        viewModel?.changedPosition?.observe(viewLifecycleOwner, Observer { position ->
            advertiserAdapter?.notifyItemChanged(position, Unit)
        })
        viewModel?.errorMessage?.observe(viewLifecycleOwner, Observer { message ->
            showToastLengthShort(message)
        })
    }

    override val bluetoothDependent = object : BluetoothDependent {

        override fun onBluetoothStateChanged(isBluetoothOn: Boolean) {
            toggleBluetoothBar(isBluetoothOn, viewBinding.bluetoothEnable)
            advertiserAdapter?.toggleIsBluetoothOperationPossible(isBluetoothOperationPossible())
            if (!isBluetoothOn) viewModel?.switchAllItemsOff()
        }
        override fun onBluetoothPermissionsStateChanged(arePermissionsGranted: Boolean) {
            toggleBluetoothPermissionsBar(arePermissionsGranted, viewBinding.bluetoothPermissionsBar)
            advertiserAdapter?.toggleIsBluetoothOperationPossible(isBluetoothOperationPossible())
            if (!arePermissionsGranted) viewModel?.switchAllItemsOff()
        }
        override fun refreshBluetoothDependentUi(isBluetoothOperationPossible: Boolean) {
            advertiserAdapter?.toggleIsBluetoothOperationPossible(isBluetoothOperationPossible)
        }
        override fun setupBluetoothPermissionsBarButtons() {
            viewBinding.bluetoothPermissionsBar.setFragmentManager(childFragmentManager)
        }
    }

    private fun initAdapter() {
        advertiserAdapter = AdvertiserAdapter(viewModel?.advertisers?.value ?: arrayListOf(), this)
        viewBinding.fragmentMainView.rvMainView.apply {
            layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
            addItemDecoration(CardViewListDecoration())
            adapter = advertiserAdapter
        }
        advertiserAdapter?.toggleIsBluetoothOperationPossible(isBluetoothOperationPossible())
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_advertiser, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.device_name -> {
                if (isBluetoothOperationPossible()) showDeviceNameDialog()
                else showWarningToast()

                true
            }
            R.id.switch_all_off -> {
                viewModel?.switchAllItemsOff()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCopyClick(item: Advertiser) { viewModel?.copyAdvertiser(item) }

    override fun onEditClick(position: Int, item: Advertiser) {
        viewModel?.switchItemOff(position)
        startAdvertiserConfigActivityForResult(item.data, position)
    }
    override fun onRemoveClick(position: Int) {
        if (AdvertiserStorage(requireContext()).shouldDisplayRemoveAdvertiserDialog()) {
            RemoveAdvertiserDialog(object : RemoveAdvertiserDialog.Callback {
                override fun onOkClicked() {
                    viewModel?.removeAdvertiserAt(position)
                }
            }).show(childFragmentManager, "dialog_remove_advertiser")
        } else viewModel?.removeAdvertiserAt(position)
    }

    override fun switchItemOn(position: Int) {
        viewModel?.switchItemOn(position)
    }

    override fun switchItemOff(position: Int) {
        viewModel?.switchItemOff(position)
    }

    private fun showWarningToast() {
        if (activityViewModel?.getIsBluetoothOn() == false) {
            showToastLengthShort(getString(R.string.bluetooth_disabled))
        } else if (activityViewModel?.getAreBluetoothPermissionsGranted() == false) {
            showToastLengthShort(getString(R.string.bluetooth_permissions_denied))
        }
    }

    private fun toggleMainView(areAnyAdvertisers: Boolean) {
        viewBinding.fragmentMainView.apply {
            if (areAnyAdvertisers) {
                fullScreenInfo.root.visibility = View.GONE
                rvMainView.visibility = View.VISIBLE
            } else {
                fullScreenInfo.root.visibility = View.VISIBLE
                rvMainView.visibility = View.GONE
            }
        }
    }

    private fun toggleAdvertisingService(areAnyAdvertisersOn: Boolean) {
        if (areAnyAdvertisersOn) AdvertiserService.startService(requireContext())
        else AdvertiserService.stopService(requireContext())
    }

    @SuppressLint("MissingPermission")
    private fun showDeviceNameDialog() {
        val currentAdapterName = service.bluetoothAdapter?.name ?: "Unknown name"
        DeviceNameDialog(currentAdapterName, object : DeviceNameDialog.DeviceNameCallback {
            override fun onDeviceRenamed(newName: String) {
                service.bluetoothAdapter?.let {
                    it.name = newName
                    viewModel?.switchAllItemsOff()
                    Handler(Looper.getMainLooper()).postDelayed({
                        advertiserAdapter?.notifyDataSetChanged()
                        showToastLengthLong(getString(R.string.local_bluetooth_name_changed, newName))
                    }, DELAY_NOTIFY_DATA_CHANGED)
                } ?: showToastLengthLong(getString(R.string.local_bluetooth_name_change_unsuccessful))
            }
        }).show(childFragmentManager, "dialog_device_name")
    }

    private fun startAdvertiserConfigActivityForResult(data: AdvertiserData, position: Int) {
        val intent = Intent(requireContext(), AdvertiserConfigActivity::class.java).apply {
            putExtra(AdvertiserConfigActivity.EXTRA_ADVERTISER_ITEM, data)
            putExtra(AdvertiserConfigActivity.EXTRA_ITEM_POSITION, position)
        }
        startActivityForResult(intent, REQUEST_CODE_EDIT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_EDIT) {
            val advertiserData = data?.extras?.getParcelable<AdvertiserData>(
                    AdvertiserConfigActivity.EXTRA_ADVERTISER_ITEM) as AdvertiserData
            val position = data.getIntExtra(EXTRA_ITEM_POSITION, 0)

            viewModel?.updateAdvertiser(position, advertiserData)
        }
    }

    private companion object {
        const val EXTRA_ITEM_POSITION = "EXTRA_ITEM_POSITION"
        const val REQUEST_CODE_EDIT = 1000
        const val DELAY_NOTIFY_DATA_CHANGED: Long = 100
    }

}
