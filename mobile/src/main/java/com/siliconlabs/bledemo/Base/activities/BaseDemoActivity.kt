package com.siliconlabs.bledemo.base.activities

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import com.siliconlabs.bledemo.bluetooth.services.BluetoothService
import com.siliconlabs.bledemo.R

abstract class BaseDemoActivity : BaseActivity() {

    private lateinit var bluetoothBinding: BluetoothService.Binding
    protected var service: BluetoothService? = null

    private val bluetoothAdapterStateChangeListener: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_OFF) finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindBluetoothService()
        setupActionBar()
        registerReceiver(bluetoothAdapterStateChangeListener, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    override fun onDestroy() {
        super.onDestroy()
        val isGattConnected = service?.isGattConnected() ?: false

        service?.clearConnectedGatt()
        unregisterReceiver(bluetoothAdapterStateChangeListener)
        bluetoothBinding.unbind()

        if (isGattConnected) {
            Toast.makeText(this, getString(R.string.device_has_disconnected), Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindBluetoothService() {
        bluetoothBinding = object : BluetoothService.Binding(this) {
            override fun onBound(service: BluetoothService?) {
                this@BaseDemoActivity.service = service
                onBluetoothServiceBound()
            }
        }
        bluetoothBinding.bind()
    }

    private fun setupActionBar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {

                service?.connectedGatt?.disconnect()
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    abstract fun onBluetoothServiceBound()
}