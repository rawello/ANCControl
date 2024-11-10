package com.rawello.anccontrol

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = permissions.values.all { it }
        if (bluetoothAdapter?.isEnabled == false) {
            showAlertDialog("Need grant all permissions")
            requestBluetoothEnable()
        }
    }

    private val requestBluetoothEnableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (bluetoothAdapter?.isEnabled == false) {
            showAlertDialog("Need grant Bluetooth permission for work")
        }
    }

    private var permissionsGranted by mutableStateOf(false)

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannels()
        permissionsGranted = checkPermissions()
        setContent {
            Scaffold {
                ANCControlApp()
            }
        }
    }

    @Composable
    fun ANCControlApp() {
        var showDialog by remember { mutableStateOf(!checkPermissions()) }
        var showAddTileDialog by remember { mutableStateOf(false) }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Permissions") },
                text = { Text("For normal work need grant Bluetooth and Notification permissions") },
                confirmButton = {
                    Button(onClick = {
                        showDialog = false
                        if (checkPermissions()) {
                            showAddTileDialog = true
                            permissionsGranted = true
                        } else {
                            requestPermissions()
                        }
                    }) {
                        Text("OK")
                    }
                }
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (permissionsGranted) {
                Text("Permissions granted")
            } else {
                Text("Permissions not granted")
            }
        }
    }

    @SuppressLint("InlinedApi")
    private fun checkPermissions(): Boolean {
        val permissions = listOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.POST_NOTIFICATIONS
        )

        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("InlinedApi")
    private fun requestPermissions() {
        val permissions = listOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.POST_NOTIFICATIONS
        )

        requestPermissionsLauncher.launch(permissions.toTypedArray())
    }

    private fun requestBluetoothEnable() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        requestBluetoothEnableLauncher.launch(enableBtIntent)
    }

    private fun showAlertDialog(message: String) {
        setContent {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Attempt") },
                text = { Text(message) },
                confirmButton = {
                    Button(onClick = { }) {
                        Text("OK")
                    }
                }
            )
        }
    }

    private fun createNotificationChannels() {
        val channelId = "anc_control_channel"
        val channelName = "ANC Control"
        val channelDescription = "Notifications for ANC Control"
        val importance = NotificationManager.IMPORTANCE_DEFAULT

        val channel = NotificationChannel(channelId, channelName, importance).apply {
            description = channelDescription
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}