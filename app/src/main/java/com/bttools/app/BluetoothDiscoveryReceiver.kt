package com.bttools.app

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class BluetoothDiscoveryReceiver(
    private val availableDevices: MutableList<String>,
    private val onStarted: () -> Unit = {},
    private val onFinished: () -> Unit = {}
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            BluetoothDevice.ACTION_FOUND -> {
                @Suppress("DEPRECATION")
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    val deviceName = it.name ?: "未知设备"
                    val deviceAddress = it.address
                    val deviceInfo = "$deviceName\n$deviceAddress"
                    if (!availableDevices.contains(deviceInfo)) {
                        availableDevices.add(deviceInfo)
                    }
                }
            }
            BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                onStarted()
            }
            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                onFinished()
            }
        }
    }

    companion object {
        fun makeFilter(): IntentFilter {
            return IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
        }
    }
}
