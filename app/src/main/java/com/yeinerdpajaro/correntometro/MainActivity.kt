
package com.yeinerdpajaro.correntometro

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import java.text.SimpleDateFormat
import java.util.*
import java.time.LocalDateTime



//val bufferedWriter = null
private var shouldSaveData = true // Bandera para controlar si se deben guardar los datos o no



class MainActivity : AppCompatActivity() {
    companion object {
        var m_myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        lateinit var m_bluetoothSocket: BluetoothSocket
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


        m_address = intent.getStringExtra(BluetoothActivity.EXTRA_ADDRESS).toString()

        val device: BluetoothDevice? = m_bluetoothAdapter?.getRemoteDevice(m_address)

        if(device != null){
            //Realizar conexion
            ConnectThread(device).start()
        }


        BtnDesconectar.setOnClickListener{
            disconnect()
        }


        BtnHistorial.setOnClickListener{
            val intent = Intent(this, DatesHistorical::class.java)
            startActivity(intent)
        }

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


    //Funcion para guardar los datos en el archivo csv
    private fun save_data_csv(buffer: String) {
        if (!shouldSaveData) return // Verificar si se deben guardar los datos
        val DatosGuardados = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + "datos_guardados.csv"
        val file = File(DatosGuardados)
        val isNewFile = !file.exists()

        val fileWriter = FileWriter(file, true)
        val bufferedWriter = BufferedWriter(fileWriter)

        Log.i("Activity", buffer)

        if (isNewFile) {
            // Agregar nombres de columnas solo si el archivo es nuevo
            val columnNames = "Pulsos (p/seg), Velocidad (m/seg), Caudal (m³/seg)" // Reemplaza con los nombres de tus columnas
            bufferedWriter.write(columnNames)
            bufferedWriter.newLine()
        }

        bufferedWriter.write(buffer)        // Agregar contenido de los datos
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

    private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {

        private val mmInputStream: InputStream = mmSocket.inputStream
        private val buffer: ByteArray = ByteArray(1024) // Tamaño del buffer para recibir datos
        val textVelocidad = findViewById<TextView>(R.id.Textvelocidad)
        val textCaudal = findViewById<TextView>(R.id.Textcaudal)
        val textPulsos = findViewById<TextView>(R.id.textPulsos)

        override fun run() {
            var bytes: Int
            val charBuffer = StringBuilder()  // Utilizaremos un StringBuilder para acumular los caracteres

            // Mantén el hilo en ejecución mientras la conexión esté activa
            while (true) {
                try {
                    // Lee los datos del InputStream
                    bytes = mmInputStream.read(buffer)

                    var receivedData = String(buffer, 0, bytes)
                    save_data_csv(receivedData)

                    // Convierte los bytes recibidos en un String y agrégalo al charBuffer
                    charBuffer.append(String(buffer, 0, bytes))

                    // Busca los separadores de línea en el charBuffer
                    val delimiter = "\n"
                    val rawData = charBuffer.toString()
                    val lines = rawData.split(delimiter)


                    // Procesa las líneas completas
                    for (line in lines) {
                        // Divide los datos por comas
                        val dataArray = line.split(",")

                        // Verificar que hay al menos tres datos separados por comas
                        if (dataArray.size >= 3) {
                            val dato1 = dataArray[0].trim()  // Pulso
                            val dato2 = dataArray[1].trim()  // Velocidad
                            val dato3 = dataArray[2].trim()  // Caudal

                            // Actualizar la interfaz de usuario en el hilo principal
                            runOnUiThread {
                                textPulsos.text = dato1
                                textVelocidad.text = dato2
                                textCaudal.text = dato3
                            }
                        }
                    }

                    // Elimina las líneas procesadas del charBuffer
                    if (lines.isNotEmpty()) {
                        charBuffer.delete(0, lines.lastIndex + delimiter.length)
                    }

                } catch (e: IOException) {
                    e.printStackTrace()
                    break
                }
            }
        }
    }
}