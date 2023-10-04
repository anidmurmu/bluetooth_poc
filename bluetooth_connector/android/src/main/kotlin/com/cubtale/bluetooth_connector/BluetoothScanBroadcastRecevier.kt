package com.cubtale.bluetooth_connector

import android.annotation.SuppressLint
import com.cubtale.bluetooth_connector.BluetoothConnectorPlugin

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

class BluetoothScanner(context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val context: Context = context.applicationContext

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            // Bluetooth is not enabled, request user to enable it
            // You might want to handle this case according to your app's requirements
            return
        }

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(bluetoothReceiver, filter)

        bluetoothAdapter.startDiscovery()
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    // Do something with the discovered device
                    // For example, you can get device name and address
                    val deviceName = device?.name ?: "Unknown Device"
                    val deviceAddress = device?.address ?: "Unknown Address"
                    Log.d("apple", deviceName)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        context.unregisterReceiver(bluetoothReceiver)
        bluetoothAdapter?.cancelDiscovery()
    }
}
