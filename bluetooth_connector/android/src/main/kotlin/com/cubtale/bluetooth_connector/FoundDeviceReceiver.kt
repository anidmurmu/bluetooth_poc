package com.cubtale.bluetooth_connector

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import io.flutter.plugin.common.EventChannel

/*
class FoundDeviceReceiver(private val events: EventChannel.EventSink?): BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when(intent?.action) {
            BluetoothDevice.ACTION_FOUND -> {
                */
/*val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(
                        BluetoothDevice.EXTRA_DEVICE,
                        BluetoothDevice::class.java
                    )
                } else {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }*//*

                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val deviceName = device?.name ?: "Unknown Device"
                val deviceAddress = device?.address ?: "Unknown Address"
                Log.d("apple", deviceName)
                Log.d("apple", "events : " + events)
                val map = mapOf("address" to deviceAddress, "name" to deviceName)
                events?.success(map)
            }
        }
    }
}*/
