package com.yeinerdpajaro.correntometro

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import java.util.*



class BluetoothActivity : AppCompatActivity() {


    private lateinit var listView: ListView
    private var btPermission = false

    companion object {
        val EXTRA_ADDRESS: String = "Device_adress"
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth)

        val btConnect: Button = findViewById(R.id.btConectar)

        btConnect.setOnClickListener {
            scanBt()
        }

        // If Bluetooth is not enabled, request permission to enable it
      /*  if (!bluetoothAdapter.isEnabled) {
            requestBluetoothPermission()
        }else{
            pairedDeviceList()
        }*/


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

    fun scanBt(){
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter:BluetoothAdapter ?= bluetoothManager.adapter

        if(bluetoothAdapter ==null){
            Toast.makeText(this, "Este dispositivo no soporta Bluetooth", Toast.LENGTH_LONG)
        }else{
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                blueToothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }else{
                blueToothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_ADMIN)
            }

        }


    }

    private val blueToothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){isGranted:Boolean ->
        if (isGranted){
            val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
            val bluetoothAdapter:BluetoothAdapter ?= bluetoothManager.adapter
            btPermission = true
            if(bluetoothAdapter?.isEnabled == false){
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                btActivityResultLauncher.launch(enableBtIntent)
            }else{
                btScan()
            }

        }else{
             btPermission = false
        }

    }

    private val btActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){result: ActivityResult ->
        if(result.resultCode == RESULT_OK){
            btScan()
        }

    }

    private fun btScan(){
        Toast.makeText(this, "Bluetooth conectado", Toast.LENGTH_LONG).show()
        pairedDeviceList()

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
