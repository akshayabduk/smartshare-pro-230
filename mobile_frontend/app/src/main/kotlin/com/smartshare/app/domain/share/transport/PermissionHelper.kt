package com.smartshare.app.domain.share.transport

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * PUBLIC_INTERFACE
 * PermissionHelper wraps runtime permission requests for Bluetooth and Wi‑Fi Direct flows.
 */
class PermissionHelper(caller: ActivityResultCaller) {

    private var pendingCallback: ((Boolean) -> Unit)? = null

    private val launcher = caller.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it == true }
        pendingCallback?.invoke(granted)
        pendingCallback = null
    }

    /**
     * PUBLIC_INTERFACE
     * Request permissions needed for Bluetooth client operations.
     */
    fun requestBluetoothPermissions(activity: Activity, onResult: (Boolean) -> Unit) {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 31) {
            perms += listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            perms += listOf(
                Manifest.permission.ACCESS_FINE_LOCATION // required for BT discovery pre-12
            )
        }
        request(activity, perms.toTypedArray(), onResult)
    }

    /**
     * PUBLIC_INTERFACE
     * Request permissions needed for Wi‑Fi Direct operations.
     */
    fun requestWifiDirectPermissions(activity: Activity, onResult: (Boolean) -> Unit) {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 33) {
            perms += Manifest.permission.NEARBY_WIFI_DEVICES
        } else {
            perms += Manifest.permission.ACCESS_FINE_LOCATION
        }
        request(activity, perms.toTypedArray(), onResult)
    }

    private fun request(activity: Activity, permissions: Array<String>, onResult: (Boolean) -> Unit) {
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            onResult(true)
            return
        }
        pendingCallback = onResult
        launcher.launch(missing.toTypedArray())
    }
}
