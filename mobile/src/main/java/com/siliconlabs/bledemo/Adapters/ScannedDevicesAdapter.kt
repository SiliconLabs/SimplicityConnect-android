package com.siliconlabs.bledemo.Adapters

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Handler
import android.util.Log
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.Bluetooth.BLE.BluetoothDeviceInfo
import com.siliconlabs.bledemo.Bluetooth.BLE.Discovery.DeviceContainer
import com.siliconlabs.bledemo.Browser.Fragment.SortMode
import com.siliconlabs.bledemo.utils.FilterDeviceParams
import com.siliconlabs.bledemo.utils.SharedPrefUtils
import com.siliconlabs.bledemo.utils.StringUtils.getStringWithoutColons
import java.util.*

open class ScannedDevicesAdapter(private val generator: DeviceInfoViewHolder.Generator, context: Context) : RecyclerView.Adapter<DeviceInfoViewHolder>(), DeviceContainer<BluetoothDeviceInfo> {
    private val sharedPrefUtils = SharedPrefUtils(context)
    private val handler = Handler()
    private var timer = Timer()

    private var updatePending = false
    private var runUpdater = true
    private var comp = 5

    private var filterDeviceParams: FilterDeviceParams? = null

    private lateinit var favoriteDevices: LinkedHashSet<String>
    private val mostRecentInfoAge: MutableMap<BluetoothDeviceInfo, Long> = HashMap()
    private val mostRecentDevicesInfo: MutableList<BluetoothDeviceInfo> = ArrayList()
    private val devicesInfo: MutableList<BluetoothDeviceInfo> = ArrayList()
    private var currentDevicesInfo: List<BluetoothDeviceInfo>? = null


    private val reverseItemsComparator = Comparator<BluetoothDeviceInfo> { lhs, rhs ->
        val lFav = favoriteDevices.contains(lhs?.address)
        val rFav = favoriteDevices.contains(rhs?.address)
        val sComp = java.lang.Boolean.compare(rFav, lFav)
        if (sComp != 0) {
            return@Comparator sComp
        }
        val lName = lhs?.scanInfo?.getDisplayName()!!
        val rName = rhs?.scanInfo?.getDisplayName()
        rName?.compareTo(lName)!!
    }

    private val rssiComparator = Comparator<BluetoothDeviceInfo> { lhs, rhs ->
        val lFav = favoriteDevices.contains(lhs?.address)
        val rFav = favoriteDevices.contains(rhs?.address)
        val sComp = java.lang.Boolean.compare(rFav, lFav)
        if (sComp != 0) {
            return@Comparator sComp
        }
        val lrssi = lhs?.scanInfo?.rssi
        val rrssi = rhs?.scanInfo?.rssi!!
        lrssi?.compareTo(rrssi)!!
    }

    private val reverseRssiComparator = Comparator<BluetoothDeviceInfo> { lhs, rhs ->
        val lFav = favoriteDevices.contains(lhs?.address)
        val rFav = favoriteDevices.contains(rhs?.address)
        val sComp = java.lang.Boolean.compare(rFav, lFav)
        if (sComp != 0) {
            return@Comparator sComp
        }
        val lrssi = lhs?.scanInfo?.rssi!!
        val rrssi = rhs?.scanInfo?.rssi
        rrssi?.compareTo(lrssi)!!
    }

    private val itemsComparator = Comparator<BluetoothDeviceInfo> { lhs, rhs ->
        val lFav = favoriteDevices.contains(lhs?.address)
        val rFav = favoriteDevices.contains(rhs?.address)
        val sComp = java.lang.Boolean.compare(rFav, lFav)
        if (sComp != 0) {
            return@Comparator sComp
        }
        val lName = lhs?.scanInfo?.getDisplayName()
        val rName = rhs?.scanInfo?.getDisplayName()!!
        lName?.compareTo(rName)!!
    }

    private val timeComparator = Comparator<BluetoothDeviceInfo> { lhs, rhs ->
        val lFav = favoriteDevices.contains(lhs?.address)
        val rFav = favoriteDevices.contains(rhs?.address)
        val sComp = java.lang.Boolean.compare(rFav, lFav)
        if (sComp != 0) {
            return@Comparator sComp
        }
        val lTimestampNanos = lhs?.scanInfo?.timestampNanos
        val rTimestampNanos = rhs?.scanInfo?.timestampNanos!!
        lTimestampNanos?.compareTo(rTimestampNanos)!!
    }

    private val onlyFavoriteComparator = Comparator<BluetoothDeviceInfo> { lhs, rhs ->
        val lFav = favoriteDevices.contains(lhs?.address)
        val rFav = favoriteDevices.contains(rhs?.address)
        java.lang.Boolean.compare(rFav, lFav)
    }

    private val delayedUpdater = Runnable { updateDevicesInfo() }

    private fun sort(comparator: Int, resetDeviceList: Boolean) {
        favoriteDevices = if (comparator == 5) {
            sharedPrefUtils.favoritesDevices
        } else {
            sharedPrefUtils.temporaryFavoritesDevices
        }

        comp = comparator
        when (comparator) {
            0 -> sortDevices(itemsComparator, resetDeviceList)
            1 -> sortDevices(reverseItemsComparator, resetDeviceList)
            2 -> sortDevices(rssiComparator, resetDeviceList)
            3 -> sortDevices(reverseRssiComparator, resetDeviceList)
            4 -> sortDevices(timeComparator, resetDeviceList)
            5 -> sortDevices(onlyFavoriteComparator, resetDeviceList)
            else -> {
            }
        }
    }

    fun setSortMode(mode: SortMode) {
        comp = when (mode) {
            SortMode.NAME_A_TO_Z -> 0
            SortMode.NAME_Z_TO_A -> 1
            SortMode.RSSI_ASC -> 2
            SortMode.RSSI_DESC -> 3
            else -> 5
        }
        updateDevicesInfo()
    }

    override fun getItemCount(): Int {
        return devicesInfo.size
    }

    override fun getItemId(position: Int): Long {
        val info: BluetoothDeviceInfo = devicesInfo[position]
        return info.device.address.hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceInfoViewHolder {
        return generator.generate(parent)
    }

    override fun onBindViewHolder(holder: DeviceInfoViewHolder, position: Int) {
        if (BluetoothAdapter.getDefaultAdapter() != null && !BluetoothAdapter.getDefaultAdapter().isEnabled) {
            return
        }
        val info = devicesInfo[position]
        holder.setData(info, position, itemCount)
    }

    fun setThermometerMode() {
        timer.cancel()
        timer = Timer()
        // This timertask is used to ensure that discovered devices that have exceeded MAX_AGE
        // will be removed, even if the updatedevices method doesn't receive a proper callback
        timer.schedule(object : TimerTask() {
            override fun run() {
                removeOldDevices()
            }
        }, 0, PERIOD_UPDATE_REMOVE_OUTDATED_HTM)
    }

    fun updateWith(devicesInfo: List<BluetoothDeviceInfo>) {
        currentDevicesInfo = devicesInfo
        if (this.devicesInfo.isEmpty()) {
            if (filterDeviceParams == null || filterDeviceParams?.isEmptyFilter!!) {
                updateDevicesInfo()
            } else if (!updatePending) {
                updatePending = true
                handler.postDelayed(delayedUpdater, 1000)
            }
        } else if (!updatePending && runUpdater) {
            updatePending = true
            handler.postDelayed(delayedUpdater, DISCOVERY_UPDATE_PERIOD)
        }
    }

    private fun removeOldDevices() {
        val now = System.currentTimeMillis()
        val iter: MutableIterator<Map.Entry<BluetoothDeviceInfo, Long>> = mostRecentInfoAge.entries.iterator()
        while (iter.hasNext()) {
            val ageEntry = iter.next()
            val age = now - ageEntry.value
            if (age > MAX_AGE) {
                mostRecentDevicesInfo.remove(ageEntry.key)
                iter.remove()
            }
        }
        handler.post { updateDevicesInfo() }
    }

    fun setRunUpdater(runUpdater: Boolean) {
        this.runUpdater = runUpdater
    }

    private fun updateDevicesInfo() {
        prepareDevicesInfo(currentDevicesInfo)
        updatePending = false
        resetDeviceList()
        sort(comp, false)

        filterDeviceParams?.let { fdp ->
            filterDevices(fdp, false)
        }

        notifyDataSetChanged()
    }

    private fun prepareDevicesInfo(devicesInfo: List<BluetoothDeviceInfo>?) {
        val now = System.currentTimeMillis()
        if (devicesInfo != null && devicesInfo.isNotEmpty()) {
            for (devInfo in devicesInfo) {
                val clone = devInfo.clone()
                clone.isOfInterest = true
                clone.isNotOfInterest = false
                clone.serviceDiscoveryFailed = false
                val index = mostRecentDevicesInfo.indexOf(clone)
                if (index >= 0) {
                    val cachedInfo: BluetoothDeviceInfo = mostRecentDevicesInfo[index]
                    val timestampDiff = clone.scanInfo?.timestampNanos!! - cachedInfo.scanInfo?.timestampNanos!!
                    if (timestampDiff != 0L) {
                        cachedInfo.scanInfo?.getDisplayName()
                        mostRecentDevicesInfo[index] = clone
                        mostRecentInfoAge[devInfo] = now
                    }
                } else {
                    mostRecentDevicesInfo.add(devInfo)
                    mostRecentInfoAge[devInfo] = now
                }
            }
        }

        //Cleaning duplicated items ----------------------------------------------------------------------
        val dedupedDeviceInfo: Set<BluetoothDeviceInfo> = HashSet(mostRecentDevicesInfo)
        mostRecentDevicesInfo.clear()
        mostRecentDevicesInfo.addAll(dedupedDeviceInfo)
        //-----------------------------------------------------------------------------------------------------
    }

    private fun resetDeviceList() {
        if (devicesInfo.isEmpty()) {
            devicesInfo.addAll(mostRecentDevicesInfo)
        } else {
            for (device in mostRecentDevicesInfo) {
                val btinfo = mostRecentDevicesInfo[mostRecentDevicesInfo.indexOf(device)]
                if (devicesInfo.contains(btinfo)) {
                    val index = devicesInfo.indexOf(device)
                    devicesInfo.removeAt(index)
                    devicesInfo.add(index, btinfo)
                } else {
                    devicesInfo.add(devicesInfo.size, btinfo)
                    Log.d("filter", "" + device.address + " added")
                }
            }
            val iter = devicesInfo.iterator()
            while (iter.hasNext()) {
                val btinfo = iter.next()
                if (!mostRecentDevicesInfo.contains(btinfo)) {
                    iter.remove()
                }
            }
        }
    }

    private fun sortDevices(comparator: Comparator<BluetoothDeviceInfo>?, resetDeviceList: Boolean) {
        if (resetDeviceList) {
            resetDeviceList()
        }

        if (devicesInfo.size > 0) {
            Collections.sort(devicesInfo, comparator)
        }
    }

    private fun isNameOrAddressContain(filterDeviceParams: FilterDeviceParams, device: BluetoothDeviceInfo): Boolean {
        return (device.address?.toLowerCase(Locale.getDefault())?.contains(filterDeviceParams.name?.toLowerCase(Locale.getDefault())!!)!!
                || getStringWithoutColons(device.address?.toLowerCase(Locale.getDefault())).contains(filterDeviceParams.name?.toLowerCase(Locale.getDefault())!!)
                || device.name != null && device.name?.toLowerCase(Locale.getDefault())?.contains(filterDeviceParams.name?.toLowerCase(Locale.getDefault())!!)!!)
    }

    fun filterDevices(filterDeviceParams: FilterDeviceParams, resetDeviceList: Boolean) {
        this.filterDeviceParams = filterDeviceParams

        if (resetDeviceList) {
            resetDeviceList()
        }

        if (this.filterDeviceParams?.isEmptyFilter!!) return
        val favorites = sharedPrefUtils.favoritesDevices
        val tmpFavorites = sharedPrefUtils.temporaryFavoritesDevices
        val deviceIterator = devicesInfo.iterator()

        while (deviceIterator.hasNext()) {
            val device = deviceIterator.next()
            if (!isNameOrAddressContain(filterDeviceParams, device)) {
                deviceIterator.remove()
                continue
            }

            if (filterDeviceParams.isRssiFlag && device.rssi < filterDeviceParams.rssiValue) {
                deviceIterator.remove()
                continue
            }

            if (filterDeviceParams.bleFormats?.isNotEmpty()!! && !filterDeviceParams.bleFormats?.contains(device.getBleFormat())!!) {
                deviceIterator.remove()
                continue
            }

            if (filterDeviceParams.advertising != null && filterDeviceParams.advertising != "") {
                if (device.scanInfo != null && device.scanInfo?.advertData != null && device.scanInfo?.advertData?.isNotEmpty()!!) {
                    var containText = false

                    if (device.rawData?.toLowerCase(Locale.getDefault())?.contains(filterDeviceParams.advertising!!.toLowerCase(Locale.getDefault()))!!) {
                        containText = true
                    }

                    if (device.address != null && device.address != "" && device.address?.toLowerCase(Locale.getDefault())?.contains(filterDeviceParams.advertising?.toLowerCase(Locale.getDefault())!!)!!) {
                        containText = true
                    }

                    if (!containText) {
                        deviceIterator.remove()
                        continue
                    }
                }
            }
            if (filterDeviceParams.isOnlyFavourite && !(favorites.contains(device.address) || tmpFavorites.contains(device.address))) {
                deviceIterator.remove()
                continue
            }
            if (filterDeviceParams.isOnlyConnectable && !device.isConnectable) {
                deviceIterator.remove()
                continue
            }
            if (filterDeviceParams.isOnlyBonded && device.device.bondState != BluetoothDevice.BOND_BONDED) {
                deviceIterator.remove()
                continue
            }
        }
    }

    fun clear() {
        mostRecentDevicesInfo.clear()
        mostRecentInfoAge.clear()
        if (devicesInfo.isNotEmpty()) {
            Log.d("clear_devicesInfo", "Called")
            devicesInfo.clear()
            notifyDataSetChanged()
        }
    }

    override fun flushContainer() {
        clear()
    }

    override fun updateWithDevices(devices: List<BluetoothDeviceInfo>) {
        updateWith(devices)
    }

    fun getDevicesInfo(): List<BluetoothDeviceInfo> {
        return devicesInfo
    }

    companion object {
        private const val MAX_AGE: Long = 16000 //Original 15000
        private const val PERIOD_UPDATE_REMOVE_OUTDATED_HTM: Long = 3000
        private const val DISCOVERY_UPDATE_PERIOD: Long = 2000 //10000
    }

}
