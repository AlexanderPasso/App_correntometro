package com.yeinerdpajaro.correntometro

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID
import java.util.*



class BluetoothActivity : AppCompatActivity() {


    private lateinit var listView: ListView
    private lateinit var deviceListAdapter: ArrayAdapter<String>
    private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val REQUEST_BLUETOOTH_PERMISSION = 1
    private val REQUEST_ENABLE_BLUETOOTH = 1




    companion object {
        val EXTRA_ADDRESS: String = "Device_adress"
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        // If Bluetooth is not enabled, request permission to enable it
        if (!bluetoothAdapter.isEnabled) {
            requestBluetoothPermission()
        }else{
            pairedDeviceList()
        }


        //Fragmento de codigo para extraer fecha y hora para guardar en el archivo estos datos
        /*val now = LocalDateTime.now()

        // Save the date and time in a variable
        val dia = now.dayOfMonth
        val mes = now.monthValue
        val hora = now.hour
        val min = now.minute

        Toast.makeText(this, "Fecha es: $dia:$mes:$hora:$min ", Toast.LENGTH_LONG)
            .show()*/

    }


    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Bluetooth Enabled!", Toast.LENGTH_SHORT).show()
            pairedDeviceList()
        } else {
            Toast.makeText(this, "Debe Activar Bluetooth", Toast.LENGTH_LONG).show()
            this.finish()
        }
    }


     fun requestBluetoothPermission() {
        // Check if we have permission to use Bluetooth
        val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED

        // If we don't have permission, request it
        if (!hasPermission) {
            // On Android 9 and lower, we can't use ActivityCompat.requestPermissions()
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                // Show a dialog to ask the user for permission
                val dialog = AlertDialog.Builder(this)
                    .setTitle("Bluetooth Permission")
                    .setMessage("This app needs permission to use Bluetooth.")
                    .setPositiveButton("OK", DialogInterface.OnClickListener { dialog, which ->
                        // Request permission
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                            REQUEST_BLUETOOTH_PERMISSION
                        )
                    })
                    .setNegativeButton("Cancel", null)
                    .create()
                dialog.show()
            } else {
                // On Android 10 and higher, we can use ActivityCompat.requestPermissions()
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    REQUEST_BLUETOOTH_PERMISSION
                )
            }
        }
        activityResultLauncher.launch(enableBluetoothIntent)

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Bluetooth permission granted
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pairedDeviceList()
                } else {
                    // On Android 9 and lower, we need to manually enable Bluetooth
                    val enBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return
                    }
                    startActivityForResult(enBluetoothIntent, REQUEST_ENABLE_BLUETOOTH)
                }
            } else {
                // Bluetooth permission denied
                Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun pairedDeviceList() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val m_pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        val deviceList: ArrayList<String> = ArrayList()

        val deviceListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceList)
        listView = findViewById<ListView>(R.id.list_device_bluetooth)
        listView.adapter = deviceListAdapter

        if (m_pairedDevices != null) {
            if (m_pairedDevices.isNotEmpty()) {
                m_pairedDevices
                    .forEach { device ->
                        val deviceName = device.name
                        val deviceHardwareAddress = device.address // MAC address
                        val deviceInfo: String = "$deviceName\n$deviceHardwareAddress"
                        deviceList.add(deviceInfo)
                    }
            } else {
                Toast.makeText(this, "No se encontraron dispositivos emparejados", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            Toast.makeText(this, "Bluetooth no esta activado", Toast.LENGTH_SHORT)
                .show()
        }

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val deviceInfo: String = deviceListAdapter.getItem(position) as String
            val address = deviceInfo.substringAfterLast("\n")

            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra(EXTRA_ADDRESS, address)
            startActivity(intent)
        }
    }

}
