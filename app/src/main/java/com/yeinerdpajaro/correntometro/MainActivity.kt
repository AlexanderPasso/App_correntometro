@file:Suppress("DEPRECATION")

package com.yeinerdpajaro.correntometro

import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.yeinerdpajaro.correntometro.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.util.*




//val bufferedWriter = null
private var shouldSaveData = true // Bandera para controlar si se deben guardar los datos o no



class MainActivity : AppCompatActivity() {
    companion object {
        var m_myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        lateinit var m_bluetoothSocket: BluetoothSocket
        lateinit var m_progress: ProgressDialog
        val m_bluetoothAdapter: BluetoothAdapter?= BluetoothAdapter.getDefaultAdapter()
        lateinit var inputStream: InputStream
        var m_isConnected: Boolean = false
        lateinit var m_address: String
        val fileWriter  = null

    }


    private lateinit var mainBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_main)

        val BtnDesconectar = findViewById<Button>(R.id.btnDesconectar)
        val BtnHistorial = findViewById<Button>(R.id.BtnHistorial)
        val BtnGuardarDatos = findViewById<Button>(R.id.BtnGuardarDatos)


        m_address = intent.getStringExtra(BluetoothActivity.EXTRA_ADDRESS).toString()

        val device: BluetoothDevice? = m_bluetoothAdapter?.getRemoteDevice(m_address)

        if(device != null){
            //Realizar conexion
            ConnectThread(device).start()
        }



        BtnDesconectar.setOnClickListener{
            disconnect()
        }

        /*BtnGuardarDatos.setOnClickListener{
            receiveDataAsync()
        }*/

        BtnHistorial.setOnClickListener{
            val intent = Intent(this, DatesHistorical::class.java)
            startActivity(intent)
        }

        // Inicializar el spinner del menú
        var menuSpinner = findViewById<Spinner>(R.id.menu_spinner)
        val adapter = ArrayAdapter.createFromResource(this, R.array.menu_items, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        menuSpinner.adapter = adapter

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.about_item -> {
                // Realizar las acciones cuando se seleccione "Acerca de"
                // Por ejemplo, mostrar un diálogo o abrir una nueva actividad
                val intent = Intent(this, Acerca_de::class.java)
                startActivity(intent)

                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }


    /*Permite la recepción de datos de forma sincronica*/
    private fun receiveDataAsync() {



        if (m_bluetoothSocket != null) {
            val inputStream = m_bluetoothSocket!!.inputStream
            val textVelocidad = findViewById<TextView>(R.id.Textvelocidad)
            val textCaudal = findViewById<TextView>(R.id.Textcaudal)
            val textPulsos = findViewById<TextView>(R.id.textPulsos)

            // Utilizar una corutina para la lectura asíncrona
            CoroutineScope(Dispatchers.IO).launch {
                val buffer = ByteArray(256)
                while (isActive) {
                    try {
                        val bytesRead = inputStream.read(buffer)
                        val receivedData = buffer.copyOfRange(0, bytesRead)

                        // Actualizar la interfaz de usuario en el hilo principal
                        withContext(Dispatchers.Main) {
                            val data = String(receivedData)
                            val dataArray = data.split(",")
                            save_data_csv(data)

                            // Verificar que hay al menos tres datos separados por comas
                            if (dataArray.size >= 3) {
                                val dato1 = dataArray[0]    //Pulso
                                val dato2 = dataArray[1]    //velocidad
                                val dato3 = dataArray[2]    //Caudal

                                // Actualizar la interfaz de usuario en el hilo principal
                                withContext(Dispatchers.Main) {
                                    // Utilizar los datos extraídos como desees

                                    textPulsos.text = dato1
                                    textVelocidad.text = dato2
                                    textCaudal.text = dato3
                                }
                            }

                            //textVelocidad.text = data
                            //Toast.makeText("Dato recibido", Toast.LENGTH_SHORT).show()
                            //Toast.makeText(this, "Dato recibido: $data", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        break
                    }
                }
            }
        }
    }


    private fun save_data_csv(buffer: String) {
        if (!shouldSaveData) return // Verificar si se deben guardar los datos
        val DatosGuardados = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + "datos_guardados.csv"
        val file = File(DatosGuardados)
        val isNewFile = !file.exists()

        val fileWriter = FileWriter(file, true)
        val bufferedWriter = BufferedWriter(fileWriter)

        if (isNewFile) {
            // Agregar nombres de columnas solo si el archivo es nuevo
            val columnNames = "Pulsos (p/seg) , Velocidad (m/seg) , Caudal (m³/seg)" // Reemplaza con los nombres de tus columnas
            bufferedWriter.write(columnNames)
            bufferedWriter.newLine()
        }

        bufferedWriter.write(buffer) // Agregar contenido de los datos
        bufferedWriter.close()
    }

    /*Permite realizar el cambio del estado a conectado para mostrar al usuario*/
    fun conectedState(texto: String){
        val color = ContextCompat.getColor(this, R.color.green_up)
        val text = findViewById<TextView>(R.id.texEstado)

        text.text = texto
        text.setTextColor(color)

    }


    /*Funcion que me permite desconectar del bluetooth*/
     fun disconnect() {
        if (m_bluetoothSocket != null) {
            try {
                m_bluetoothSocket!!.close()
               // m_bluetoothSocket = null
                m_isConnected = false
                shouldSaveData = false // Establecer la bandera en falso para dejar de guardar los datos
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        finish()
    }


    private inner class  ConnectThread(val device: BluetoothDevice) : Thread(){

        override fun run(){

            try {
                m_bluetoothSocket = device.createRfcommSocketToServiceRecord(m_myUUID)
                m_bluetoothSocket.connect()

                conectedState("Conectado")

                val connectedThread = ConnectedThread(m_bluetoothSocket)
                connectedThread.start()

            }catch (e: IOException){
                e.printStackTrace()
            }
        }
    }

    private inner class ConnectedThread(private val mmSocket: BluetoothSocket): Thread() {

        private val mmInputStream: InputStream = mmSocket.inputStream
        private val buffer: ByteArray = ByteArray(1024) // Tamaño del buffer para recibir datos
        val textVelocidad = findViewById<TextView>(R.id.Textvelocidad)
        val textCaudal = findViewById<TextView>(R.id.Textcaudal)
        val textPulsos = findViewById<TextView>(R.id.textPulsos)


        override fun run() {
            var bytes: Int

            // Mantén el hilo en ejecución mientras la conexión esté activa
            while (true) {
                try {
                    // Lee los datos del InputStream
                    bytes = mmInputStream.read(buffer)

                    // Convierte los bytes recibidos en un String
                    val receivedData = String(buffer, 0, bytes)

                    // Procesa los datos recibidos como sea necesario
                    //processData(receivedData)
                    //val data = String(receivedData)
                    val dataArray = receivedData.split(",")
                    save_data_csv(receivedData)

                    // Verificar que hay al menos tres datos separados por comas
                    if (dataArray.size >= 3) {
                        val dato1 = dataArray[0]    //Pulso
                        val dato2 = dataArray[1]    //velocidad
                        val dato3 = dataArray[2]    //Caudal

                        // Actualizar la interfaz de usuario en el hilo principal
                        CoroutineScope(Dispatchers.IO).launch {
                            withContext(Dispatchers.Main) {
                                // Utilizar los datos extraídos como desees

                                textPulsos.text = dato1
                                textVelocidad.text = dato2
                                textCaudal.text = dato3
                            }
                        }

                    }

                }catch (e: IOException) {
                    e.printStackTrace()
                    // Manejar la excepción (por ejemplo, conexión perdida)
                    break
                }

            }
        }

    }
}