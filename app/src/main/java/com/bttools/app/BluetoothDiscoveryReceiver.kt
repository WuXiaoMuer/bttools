package com.bttools.app

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 * 经典蓝牙发现广播接收器。通过回调上报，由调用方决定如何存储。
 */
class BluetoothDiscoveryReceiver(
    private val onFound: (name: String, address: String) -> Unit,
    private val onStarted: () -> Unit = {},
    private val onFinished: () -> Unit = {}
) : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            BluetoothDevice.ACTION_FOUND -> {
                @Suppress("DEPRECATION")
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    val name = try { it.name } catch (e: SecurityException) { null } ?: "未知设备"
                    onFound(name, it.address)
                }
            }
            BluetoothAdapter.ACTION_DISCOVERY_STARTED -> onStarted()
            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> onFinished()
        }
    }

    companion object {
        fun makeFilter(): IntentFilter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
    }
}
