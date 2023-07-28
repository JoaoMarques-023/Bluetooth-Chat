package com.example.onoffbasedados

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.lang.Exception
import java.util.*


private val uuid: UUID = UUID.fromString("06ae0a74-7bd4-43aa-ab5d-2511f3f6bab1")
private lateinit var mySelectedBluetoothDevice: BluetoothDevice
private lateinit var bluetoothAdapter: BluetoothAdapter
private lateinit var socket: BluetoothSocket
private lateinit var myHandler: Handler


class MainActivity : AppCompatActivity() {
    private lateinit var bluetoothNameTextView: TextView
    private lateinit var bluetoothAddressTextView: TextView
    private lateinit var connectedOrNotTextView: TextView
    private lateinit var connectToDeviceButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var sendMessageButton: Button
    private lateinit var writeMessageEditText: EditText
    private lateinit var receivedMessageUserTextView: TextView
    private lateinit var receivedMessageTextView: TextView
    lateinit var bluetoothAdapter: BluetoothAdapter
    private val REQUEST_CODE_ENABLE_BT = 1
    private val REQUEST_CODE_BLUETOOTH_PERMISSION = 2

    private val REQUEST_CODE_MAKE_DISCOVERABLE = 3

    private  val BLUETOOTH_PERMISSION_REQUEST_CODE = 100
    private  val BLUETOOTH_ADMIN_PERMISSION_REQUEST_CODE = 101
    private  val BLUETOOTH_CONNECT_PERMISSION_REQUEST_CODE = 102
    private  val BLUETOOTH_ENABLE_REQUEST_CODE = 103
    private  val REQUEST_CODE_DISCOVERABLE_BT = 6


    private lateinit var db: AppDatabase

    private lateinit var discoverableTv: TextView // Declare the discoverableTv TextView
    private val enableBluetoothLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "Bluetooth is on", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Could not turn on Bluetooth", Toast.LENGTH_SHORT).show()
        }
    }

    private val disableBluetoothLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "Bluetooth turned off", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Could not turn off Bluetooth", Toast.LENGTH_SHORT).show()
        }
    }

    private val makeDiscoverableLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Dispositivo está discoverable
            Toast.makeText(this, "Seu dispositivo está visível para outros dispositivos.", Toast.LENGTH_SHORT).show()

        } else {
            // Falha em tornar o dispositivo discoverable ou o usuário recusou a solicitação
            Toast.makeText(this, "Não foi possível tornar o dispositivo visível.", Toast.LENGTH_SHORT).show()
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bluetoothManager: BluetoothManager =
            getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "bluetooth_database"
        ).build()

        connectedOrNotTextView = findViewById(R.id.connectedOrNotTextView)
        connectToDeviceButton = findViewById(R.id.connectToDeviceButton)
        disconnectButton = findViewById(R.id.disconnectButton)
        sendMessageButton = findViewById(R.id.sendMessageButton)
        writeMessageEditText = findViewById(R.id.writeMessageEditText)
        receivedMessageUserTextView = findViewById(R.id.receivedMessageUserTextView)
        receivedMessageTextView = findViewById(R.id.receivedMessageTextView)

        val turnOnBtn = findViewById<Button>(R.id.turnOnBtn)
        val turnOffBtn = findViewById<Button>(R.id.turnOffBtn)
        val discoverableBtn = findViewById<Button>(R.id.discoverableBtn)
        val pairedBtn = findViewById<Button>(R.id.pairedBtn)
        val pairedTv = findViewById<TextView>(R.id.pairedTv)


        turnOnBtn.setOnClickListener {
            if (bluetoothAdapter.isEnabled) {
                Toast.makeText(this, "Already ON", Toast.LENGTH_SHORT).show()
            } else {
                if (checkBluetoothPermission()) {
                    val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    enableBluetoothLauncher.launch(intent)
                } else {
                    requestBluetoothPermission()
                }
            }
        }

        turnOffBtn.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                redirectToBluetoothSettings()
            } else {
                turnOffBluetooth()
            }
        }





        discoverableBtn.setOnClickListener {
            if(!bluetoothAdapter.isDiscovering){
                Toast.makeText(this, "Making device discoverable", Toast.LENGTH_SHORT).show()
                val intent = Intent(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE))
                startActivityForResult(intent, REQUEST_CODE_DISCOVERABLE_BT)
            }
        }
        //   val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        // registerReceiver(discoveryReceiver, filter)



        pairedBtn.setOnClickListener {
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
                pairedTv.text = "Paired Devices"
                val devices = bluetoothAdapter.bondedDevices
                for (device in devices) {
                    val deviceName = device.name
                    val deviceAddress = device.address
                    pairedTv.append("\nDevice: $deviceName, $deviceAddress")
                }
            } else {
                Toast.makeText(this, "Turn on Bluetooth first", Toast.LENGTH_SHORT).show()
            }
        }





        if (!isBluetoothEnabled()) {
            // If Bluetooth is not enabled, request the user to enable it.
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, BLUETOOTH_ENABLE_REQUEST_CODE)


        } else {
            // Bluetooth is already enabled, continue with your existing code.
            initBluetooth()
        }

        // Bluetooth is enabled, proceed with your existing cod6<


    }



    private fun turnOffBluetooth() {
        if (bluetoothAdapter.isEnabled) {
            //bluetoothAdapter.disable()
            Toast.makeText(this, "Bluetooth turned off", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Bluetooth is already off", Toast.LENGTH_SHORT).show()
        }
    }

    private fun redirectToBluetoothSettings() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Close the database helper
    }

    // Função para inicializar a funcionalidade Bluetooth
    private fun initBluetooth() {



        AcceptThread().start()
        myHandler = Handler()


        connectToDeviceButton.setOnClickListener {
            if (bluetoothAdapter.isEnabled) {
                val pairedDevices = bluetoothAdapter.bondedDevices.toList()
                val deviceList = pairedDevices.map { it.name + "\n" + it.address }.toTypedArray()

                val dialogBuilder = AlertDialog.Builder(this)
                dialogBuilder.setTitle("Select a device to connect")
                dialogBuilder.setItems(deviceList) { dialog, which ->
                    val selectedDevice = pairedDevices[which]

                    Log.d("Bluetooth", "Selected device: ${selectedDevice.name}, ${selectedDevice.address}")
                    mySelectedBluetoothDevice = selectedDevice

                    connectToDevice(selectedDevice)
                    dialog.dismiss()
                }
                dialogBuilder.show()
            } else {
                Toast.makeText(applicationContext, "Turn on Bluetooth first", Toast.LENGTH_SHORT).show()
            }
        }
        disconnectButton.setOnClickListener {
            Log.d("Other phone", "Closing socket and connection")
            socket.close()
            connectedOrNotTextView.text = "Not connected"
            connectToDeviceButton.isEnabled = true
            disconnectButton.isEnabled = false
            sendMessageButton.isEnabled = false
        }

        sendMessageButton.setOnClickListener {
            val message = writeMessageEditText.text.toString()
            if (message.isNotEmpty()) {
                if (checkPermission(Manifest.permission.BLUETOOTH)) {
                    val connectThreadInstance = ConnectThread(mySelectedBluetoothDevice)
                    connectThreadInstance.writeMessage(message)

                    // Get the name of the sender device
                    val senderDeviceName = mySelectedBluetoothDevice.name ?: "Unknown Device"

                    val messageEntity = MessageEntity(senderName = senderDeviceName, message = message)

                    // Insert the sent message to the database using coroutines
                    GlobalScope.launch(Dispatchers.IO) {
                        val db = Room.databaseBuilder(
                            applicationContext,
                            AppDatabase::class.java,
                            "bluetooth_database"
                        ).build()
                        db.messageDao().insert(messageEntity)

                        // Logging the inserted data
                        Log.d("Database", "Inserted: SenderName=$senderDeviceName, Message=$message")                    }
                } else {
                    requestPermission(Manifest.permission.BLUETOOTH, BLUETOOTH_PERMISSION_REQUEST_CODE)
                }
            } else {
                Toast.makeText(applicationContext, "Empty message", Toast.LENGTH_SHORT).show()
            }
        }
    }





    private fun connectToDevice(device: BluetoothDevice) {
        if (checkPermission(Manifest.permission.BLUETOOTH_ADMIN)) {
            ConnectThread(device).start()
        } else {
            requestPermission(Manifest.permission.BLUETOOTH_ADMIN, BLUETOOTH_ADMIN_PERMISSION_REQUEST_CODE)
        }
    }



    private fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter.isEnabled
    }


    private fun checkPermission(permission: String): Boolean {
        return PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, permission)
    }

    private fun requestPermission(permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
    }




    private inner class AcceptThread : Thread() {
        private var cancelled: Boolean = false
        private val serverSocket: BluetoothServerSocket? = try {
            if (checkPermission(Manifest.permission.BLUETOOTH_ADMIN) && bluetoothAdapter.isEnabled) {
                bluetoothAdapter.listenUsingRfcommWithServiceRecord("test", uuid)
            } else {
                null
            }
        } catch (e: SecurityException) {
            null
        }

        override fun run() {
            var socket: BluetoothSocket
            while (true) {
                if (cancelled) {
                    break
                }
                try {
                    socket = serverSocket?.accept() ?: break
                } catch (e: IOException) {
                    break
                }
                if (!cancelled && socket != null) {
                    Log.d("Other phone", "Connecting")
                    ConnectedThread(socket).start()
                }
            }
        }

        fun cancel() {
            cancelled = true
            serverSocket?.close()
        }
    }

    private inner class ConnectThread(private val device: BluetoothDevice) : Thread() {
        private var newSocket: BluetoothSocket? = null

        override fun run() {
            try {
                Log.d("You", "Connecting socket")
                myHandler.post {
                    connectedOrNotTextView.text = "Connecting..."
                    connectToDeviceButton.isEnabled = false
                }

                if (checkPermission(Manifest.permission.BLUETOOTH_ADMIN)) {
                    newSocket = device.createRfcommSocketToServiceRecord(uuid)
                    socket = newSocket!!
                    socket.connect()
                    Log.d("You", "Socket connected")
                    myHandler.post {
                        connectedOrNotTextView.text = "Connected"
                        connectToDeviceButton.isEnabled = false
                        disconnectButton.isEnabled = true
                        sendMessageButton.isEnabled = true
                    }
                } else {
                    requestPermission(Manifest.permission.BLUETOOTH_ADMIN, BLUETOOTH_ADMIN_PERMISSION_REQUEST_CODE)
                }
            } catch (e1: SecurityException) {
                Log.e("You", "Permission denied: BLUETOOTH_ADMIN")
                myHandler.post {
                    connectedOrNotTextView.text = "Connection failed: Permission denied"
                    connectToDeviceButton.isEnabled = true
                    disconnectButton.isEnabled = false
                    sendMessageButton.isEnabled = false
                }
            } catch (e2: Exception) {
                Log.e("You", "Error connecting socket, $e2")
                myHandler.post {
                    connectedOrNotTextView.text = "Connection failed"
                    connectToDeviceButton.isEnabled = true
                    disconnectButton.isEnabled = false
                    sendMessageButton.isEnabled = false
                }
            }
        }

        fun writeMessage(newMessage: String) {
            if (checkPermission(Manifest.permission.BLUETOOTH)) {
                Log.d("You", "Sending")
                val outputStream = socket.outputStream
                try {
                    outputStream.write(newMessage.toByteArray())
                    outputStream.flush()
                    Log.d("You", "Sent $newMessage")

                    myHandler.post {
                        receivedMessageUserTextView.text = "Me: "
                        receivedMessageTextView.text = newMessage
                    }
                } catch (e: Exception) {
                    Log.e("You", "Cannot send, $e")
                }
            } else {
                requestPermission(Manifest.permission.BLUETOOTH, BLUETOOTH_PERMISSION_REQUEST_CODE)
            }
        }
    }





    private inner class ConnectedThread(private val socket: BluetoothSocket) : Thread() {
        override fun run() {
            val inputStream = socket.inputStream
            var buffer = ByteArray(1024)
            var bytes = 0
            while (true) {
                try {
                    bytes = inputStream.read(buffer, bytes, 1024 - bytes)
                    val receivedMessage = String(buffer).substring(0, bytes)

                    Log.d("Other phone", "New received message: $receivedMessage")
                    myHandler.post {
                        try {
                            receivedMessageUserTextView.text = "${mySelectedBluetoothDevice.name}: "
                        } catch (e: SecurityException) {
                            receivedMessageUserTextView.text = "Unknown Device: "
                        }
                        receivedMessageTextView.text = receivedMessage
                    }
                    bytes = 0
                } catch (e: IOException) {
                    e.printStackTrace()
                    Log.d("Other phone", "Error reading")
                    break
                }
            }
        }
    }




    private fun checkLocationPermissions(): Boolean {
        val fineLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val backgroundLocationPermission =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            } else {
                PackageManager.PERMISSION_GRANTED
            }

        return fineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                backgroundLocationPermission == PackageManager.PERMISSION_GRANTED
    }




    // Function to check if BLUETOOTH permission is granted
    private fun checkBluetoothPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH
        ) == PackageManager.PERMISSION_GRANTED
    }
    // Function to request BLUETOOTH permission
    private fun requestBluetoothPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.BLUETOOTH),
            REQUEST_CODE_BLUETOOTH_PERMISSION
        )
    }




    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            BLUETOOTH_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission to access Bluetooth granted, you can proceed with Bluetooth operations.
                } else {
                    // Permission to access Bluetooth denied, handle this case (e.g., show an error message).
                    Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            BLUETOOTH_ADMIN_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission to access Bluetooth admin features granted, you can proceed with enabling or disabling Bluetooth.

                } else {
                    // Permission to access Bluetooth admin features denied, handle this case (e.g., show an error message).
                    Toast.makeText(this, "Bluetooth admin permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            // Handle other permission request codes if needed.
        }
    }




    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_ENABLE_BT -> {
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is enabled successfully
                    Toast.makeText(this, "Bluetooth is on", Toast.LENGTH_SHORT).show()
                    initBluetooth() // Call the initBluetooth() function here
                } else {
                    // Failed to enable Bluetooth or user declined the request
                    Toast.makeText(this, "Could not turn on Bluetooth", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_CODE_MAKE_DISCOVERABLE -> {
                if (resultCode == Activity.RESULT_OK) {
                    // Dispositivo está discoverable
                    Toast.makeText(this, "Seu dispositivo está visível para outros dispositivos.", Toast.LENGTH_SHORT).show()
                } else {
                    // Falha em tornar o dispositivo discoverable ou o usuário recusou a solicitação
                    Toast.makeText(this, " foi possível tornar o dispositivo visível.", Toast.LENGTH_SHORT).show()
                }
            }
            BLUETOOTH_ENABLE_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    // User granted Bluetooth enable permission
                    Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show()
                    initBluetooth()

                } else {
                    // User denied Bluetooth enable permission
                    Toast.makeText(this, "Bluetooth enable permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }



}
