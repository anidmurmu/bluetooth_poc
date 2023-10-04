package com.cubtale.bluetooth_connector

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import android.content.Intent
import android.os.Handler
import android.widget.Toast
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.os.Message
import com.nutspace.nut.api.BleDeviceManager
import io.flutter.plugin.common.PluginRegistry
import kotlinx.coroutines.flow.update
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception
import java.util.UUID

/** BluetoothConnectorPlugin  */
class BluetoothConnectorPlugin : FlutterPlugin, MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private var channel: MethodChannel? = null
    private var connectionStatus: EventChannel? = null
    private var receiveMessages: EventChannel? = null
    var bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    var instance: BleDeviceManager? = null
    private var sendRecieve: SendRecieve? = null
    var btEnabelingIntent: Intent? = null
    private var foundDeviceReceiver: FoundDeviceReceiver? = null;
    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "bluetooth_connector")
        channel?.setMethodCallHandler(this)
        connectionStatus = EventChannel(
            flutterPluginBinding.flutterEngine.dartExecutor, "connection_status"
        )
        connectionStatus?.setStreamHandler(connectionStatusStreamHandler)
        receiveMessages = EventChannel(
            flutterPluginBinding.flutterEngine.dartExecutor, "recieved_message_events"
        )
        receiveMessages?.setStreamHandler(receivedMessagesStreamHandler)
        foundDeviceReceiver = FoundDeviceReceiver(flutterPluginBinding.applicationContext)
    }

    /*private val foundDeviceReceiver = FoundDeviceReceiver { device ->
        _scannedDevices.update { devices ->
            val newDevice = device.toBluetoothDeviceDomain()
            if(newDevice in devices) devices else devices + newDevice
        }
    }*/

    var handler: Handler = Handler { msg ->
        when (msg.what) {
            STATE_LISTENING -> if (eventSink != null) {
                eventSink?.success("Listening")
            }

            STATE_CONNECTING -> if (eventSink != null) {
                eventSink?.success("Connecting")
            }

            STATE_CONNECTED -> if (eventSink != null) {
                eventSink?.success("Connected")
            }

            STATE_CONNECTION_FAILED -> if (eventSink != null) {
                eventSink?.success("Connection Failed")
            }

            STATE_MESSAGE_RECEIVED -> {
                val readBuffer = msg.obj as ByteArray
                val tempMsg = String(readBuffer, 0, msg.arg1)
                if (receiveMessageSink != null) {
                    receiveMessageSink?.success(tempMsg)
                }
            }

            DISCONNECTED -> if (eventSink != null) {
                eventSink?.success("Disconnected")
            }

            else -> if (eventSink != null) {
                eventSink?.success("LISTENER ON STAND-BY")
            }
        }
        true
    }

    @SuppressLint("MissingPermission")
    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "startScanBtDevices" -> {
                context?.registerReceiver(
                    foundDeviceReceiver!!,
                    IntentFilter(BluetoothDevice.ACTION_FOUND)
                )
                bluetoothAdapter?.startDiscovery()
                android.util.Log.d("apple", "invoked")
                result.success(true)
            }

            "stopScanBtDevices" -> {
                try {
                    context?.unregisterReceiver(foundDeviceReceiver)
                    bluetoothAdapter?.cancelDiscovery()
                    result.success(true)
                } catch (ex: Exception) {
                    result.success(false)
                }
            }

            "toggleConnection" -> if (bluetoothAdapter?.isEnabled == true) {
                bluetoothAdapter?.disable()
                Toast.makeText(context, "Bluetooth disabled", Toast.LENGTH_SHORT).show()
                result.success(true)
            } else {
                bluetoothAdapter?.enable()
                Toast.makeText(context, "Bluetooth enabled", Toast.LENGTH_SHORT).show()
                result.success(true)
            }

            "initBlutoothConnection" -> try {
                val uuid: String? = call.argument("uuid")
                MY_UUID = UUID.fromString(uuid)
                result.success(true)
            } catch (err: Exception) {
                result.success(false)
            }

            "getBtDevices" -> {
                val devices: Set<BluetoothDevice> = bluetoothAdapter?.bondedDevices ?: emptySet()
                btDevices = mutableListOf<BluetoothDevice>()
                val strings: ArrayList<HashMap<String, String>> = ArrayList()
                devices.forEachIndexed { index, bluetoothDevice ->
                    val deviceData: HashMap<String, String> = HashMap()
                    deviceData["name"] = bluetoothDevice.name
                    deviceData["address"] = bluetoothDevice.address
                    strings.add(deviceData)
                    btDevices?.add(bluetoothDevice)
                }
                result.success(strings)
            }

            "getBtDevice" -> if (btDevices != null || btDevices?.isNotEmpty() == true) {
                val address: String? = call.argument("address")
                val deviceData = HashMap<String, String>()

                for (device in btDevices!!) {
                    if (device.address == address) {
                        deviceData["name"] = device.name
                        deviceData["address"] = device.address
                        break
                    }
                }

                if (deviceData.isEmpty()) {
                    result.success(null)
                } else {
                    result.success(deviceData)
                }
            } else {
                result.success(null)
            }


            "checkBt" -> checkBluetoothOnOff(result)
            "startServer" -> try {
                val serverClass: ServerClass = ServerClass()
                serverClass.start()
                result.success(true)
            } catch (except: Exception) {
                result.success(false)
            }

            "startClient" -> try {
                val index: Int? = call.argument("index")
                var isSecure: Boolean? = call.argument("isSecure")
                if (isSecure == null) {
                    isSecure = false
                }
                val clientClass: ClientClass = ClientClass(btDevices?.get(index!!), isSecure)
                clientClass.start()
                result.success(true)
            } catch (except: Exception) {
                result.success(false)
            }

            "sendMessage" -> try {
                val message: String? = call.argument("message")
                var sendByteByByte: Boolean? = call.argument("sendByteByByte")
                if (sendByteByByte == null) {
                    sendByteByByte = false
                }
                val string: String = message.toString()
                val res: Boolean? = if (sendByteByByte) {
                    sendRecieve?.writeByteByByte(string.toByteArray())
                } else {
                    sendRecieve?.writeAsBytes(string.toByteArray())
                }
                result.success(res)
            } catch (except: Exception) {
                result.success(false)
            }

            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel?.setMethodCallHandler(null)
    }

    private fun checkBluetoothOnOff(result: Result?): String {
        if (bluetoothAdapter == null) {
            // Bluetooth is not supported
            Toast.makeText(context, "Device does not support Bluetooth", Toast.LENGTH_LONG).show()
            result?.success(false)
        } else {
            if (!(bluetoothAdapter?.isEnabled == true)) {
                btEnabelingIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                Toast.makeText(context, "Turn on your Bluetooth", Toast.LENGTH_LONG).show()
                result?.success(false)
            } else {
                Toast.makeText(context, "Bluetooth is on", Toast.LENGTH_LONG).show()
                result?.success(true)
            }
        }
        return "Unknown status"
    }

    // Thread Classes for communication
    @SuppressLint("MissingPermission")
    private inner class ServerClass : Thread() {
        private var serverSocket: BluetoothServerSocket? = null

        init {
            try {
                serverSocket =
                    bluetoothAdapter?.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun run() {
            var socket: BluetoothSocket? = null
            while (socket == null) {
                try {
                    val message: Message = Message.obtain()
                    message.what = STATE_CONNECTING
                    handler.sendMessage(message)
                    socket = serverSocket?.accept()
                } catch (e: IOException) {
                    e.printStackTrace()
                    val message: Message = Message.obtain()
                    message.what = STATE_CONNECTION_FAILED
                    handler.sendMessage(message)
                }
                if (socket != null) {
                    val message: Message = Message.obtain()
                    message.what = STATE_CONNECTED
                    handler.sendMessage(message)
                    // write some code for send recieve message here
                    sendRecieve = SendRecieve(socket)
                    sendRecieve?.start()
                    break
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private inner class ClientClass(device1: BluetoothDevice?, isSecureConnection: Boolean?) :
        Thread() {
        private val device: BluetoothDevice?
        private var socket: BluetoothSocket? = null

        init {
            device = device1
            try {
                socket = if (isSecureConnection == true) device?.createRfcommSocketToServiceRecord(
                    MY_UUID
                ) else device?.createInsecureRfcommSocketToServiceRecord(
                    MY_UUID
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        @SuppressLint("MissingPermission")
        override fun run() {
            try {
                socket?.connect()
                val message: Message = Message.obtain()
                message.what = STATE_CONNECTED
                handler.sendMessage(message)
                sendRecieve = SendRecieve(socket)
                sendRecieve?.start()
            } catch (e: Exception) {
                e.printStackTrace()
                val message: Message = Message.obtain()
                message.what = STATE_CONNECTION_FAILED
                handler.sendMessage(message)
            }
        }
    }

    private inner class SendRecieve(socket: BluetoothSocket?) : Thread() {
        private val bluetoothSocket: BluetoothSocket?
        private val inputStream: InputStream?
        private val outputStream: OutputStream?

        init {
            bluetoothSocket = socket
            var tempInputStr: InputStream? = null
            var tempOutputStr: OutputStream? = null
            try {
                tempInputStr = bluetoothSocket?.inputStream
                tempOutputStr = bluetoothSocket?.outputStream
            } catch (e: Exception) {
                e.printStackTrace()
            }
            inputStream = tempInputStr
            outputStream = tempOutputStr
        }

        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int?
            while (true) {
                try {
                    bytes = inputStream?.read(buffer)
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED, bytes!!, -1, buffer)
                        .sendToTarget()
                } catch (e: Exception) {
                    val message: Message = Message.obtain()
                    message.what = DISCONNECTED
                    handler.sendMessage(message)
                    e.printStackTrace()
                }
            }
        }

        fun writeAsBytes(bytes: ByteArray?): Boolean? {
            return try {
                outputStream?.write(bytes)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        fun writeByteByByte(bytes: ByteArray?): Boolean? {
            return try {
                if (bytes != null) {
                    for (b in bytes) {
                        outputStream?.write(b.toInt())
                    }
                }
                // byte b = (byte) '.';
                // outputStream.write((int) b);
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    companion object {
        var btDevices: MutableList<BluetoothDevice>? = null
        const val STATE_LISTENING = 1
        const val STATE_CONNECTING = 2
        const val STATE_CONNECTED = 3
        const val STATE_CONNECTION_FAILED = 4
        const val STATE_MESSAGE_RECEIVED = 5
        const val DISCONNECTED = 6
        private val APP_NAME: String = "BtChat"
        private var MY_UUID: UUID? = UUID.fromString("20585adb-d260-445e-934b-032a2c8b2e14")
        private var eventSink: EventChannel.EventSink? = null
        private var receiveMessageSink: EventChannel.EventSink? = null
        var context: Context? = null
        fun registerWith(registrar: PluginRegistry.Registrar) {
            context = registrar.context()
            val flutterbluetoothConnectorPlugin = BluetoothConnectorPlugin()
            val channel = MethodChannel(registrar.messenger(), "bluetoothadapter")
            channel.setMethodCallHandler(flutterbluetoothConnectorPlugin)
            val connectionStatusEventChannel =
                EventChannel(registrar.messenger(), "connection_status")
            connectionStatusEventChannel.setStreamHandler(connectionStatusStreamHandler)
            val recievedMessagesEventChannel = EventChannel(
                registrar.messenger(), "recieved_message_events"
            )
            recievedMessagesEventChannel.setStreamHandler(receivedMessagesStreamHandler)
        }

        private val connectionStatusStreamHandler: EventChannel.StreamHandler =
            object : EventChannel.StreamHandler {
                override fun onListen(arguments: Any, events: EventChannel.EventSink?) {
                    events?.success("LISTENING")
                    eventSink = events
                }

                override fun onCancel(arguments: Any) {
                    eventSink = null
                }
            }
        private val receivedMessagesStreamHandler: EventChannel.StreamHandler =
            object : EventChannel.StreamHandler {
                override fun onListen(arguments: Any, events: EventChannel.EventSink?) {
                    events?.success("READY TO RECEIVE MESSAGES")
                    receiveMessageSink = events
                }

                override fun onCancel(arguments: Any) {
                    receiveMessageSink = null
                }
            }
    }
}