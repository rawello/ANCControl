package com.rawello.anccontrol

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class ANCService : TileService() {
    private var currentState = 0
    private var bluetoothSocket: BluetoothSocket? = null
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }
    private var commandSentSuccessfully = false

    override fun onClick() {
        super.onClick()
        if (!checkPermissions()) {
            showPermissionNotification()
            return
        }
        currentState = (currentState + 1) % 3
        connectToDevice()
        updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        if (!checkPermissions()) {
            showPermissionNotification()
            return
        }
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile
        when (currentState) {
            0 -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "ANC Off"
            }
            1 -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "ANC On"
            }
            2 -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Awareness"
            }
        }
        if (commandSentSuccessfully) {
            tile.updateTile()
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice() {
        if (!checkPermissions()) {
            showPermissionNotification()
            return
        }

        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            return
        }

        val device = findDeviceByName("HUAWEI FreeBuds 5i")

        if (device != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb") // standard SPP UUID
                    if (bluetoothSocket == null || !bluetoothSocket!!.isConnected) {
                        bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                        bluetoothSocket?.connect()
                    }

                    setANCState(currentState)
                } catch (e: IOException) {
                    bluetoothSocket?.close()
                    bluetoothSocket = null
                    connectToDevice()
                }
            }
        } else {
            showDeviceNotFoundNotification()
        }
    }

    @SuppressLint("MissingPermission")
    private fun findDeviceByName(name: String): BluetoothDevice? {
        if (!checkPermissions()) {
            showPermissionNotification()
            return null
        }

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            if (device.name == name) {
                return device
            }
        }
        return null
    }

    private fun setANCState(state: Int) {
        if (!checkPermissions()) {
            showPermissionNotification()
            return
        }

        if (bluetoothSocket == null || !bluetoothSocket!!.isConnected) {
            return
        }

        val command = when (state) {
            0 -> MbbCommand(43, 4, mapOf(1 to byteArrayOf(0x00, 0x00))) // ANC Off
            1 -> MbbCommand(43, 4, mapOf(1 to byteArrayOf(0x01, 0xFF.toByte()))) // ANC Noise Cancel
            2 -> MbbCommand(43, 4, mapOf(1 to byteArrayOf(0x02, 0xFF.toByte()))) // ANC Awareness
            else -> MbbCommand(43, 4, mapOf(1 to byteArrayOf(0x00, 0x00))) // Default to ANC Off
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val outputStream: OutputStream = bluetoothSocket!!.outputStream
                outputStream.write(command.toBytes())
                commandSentSuccessfully = true
                updateTile()
            } catch (e: IOException) {
                bluetoothSocket?.close()
                bluetoothSocket = null
                connectToDevice()
            }
        }
    }

    @SuppressLint("InlinedApi")
    private fun checkPermissions(): Boolean {
        val permissions = listOf(
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.POST_NOTIFICATIONS
        )

        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    private fun showPermissionNotification() {
        val channelId = "anc_control_channel"
        val notificationId = 1

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_anc_icon)
            .setContentTitle("Permissions")
            .setContentText("Grant permissions for normal work")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(this)) {
            notify(notificationId, notification)
        }
    }

    @SuppressLint("MissingPermission")
    private fun showDeviceNotFoundNotification() {
        val channelId = "anc_control_channel"
        val notificationId = 2

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_anc_icon)
            .setContentTitle("Device not found")
            .setContentText("Device not found, pls check connection")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(this)) {
            notify(notificationId, notification)
        }
    }
}