package com.example.bb_temperature

import TextUtil
import android.app.*
import android.bluetooth.*
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.*
import android.os.*
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import com.example.bb_temperature.util.CommVal
import com.example.bb_temperature.util.CommVal.DATA_ACTION
import java.util.*
import kotlin.collections.ArrayList

class BluetoothLeService:Service() {
    private val TAG: String = "BluetoothLeService"
    private val STATE_DISCONNECTED = 0
    private val STATE_CONNECTING = 1
    private val STATE_CONNECTED = 2
    private var connectionState = STATE_DISCONNECTED
    var bluetoothGatt: BluetoothGatt? = null
    private val binder = LocalBinder()
    private var icallBack: ICallback? = null
    private var scanStopHandler: Handler? = null
    private val DIGITALCOMPAION_MANAGER_TEMPDATA = "digitalcompanion.temp.data"
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var readCharacteristic: BluetoothGattCharacteristic? = null
    private var writeBuffer = arrayListOf<ByteArray>()
    private var CHANNEL_ID = "FOREGROUND_CHANNEL"
    private val SCAN_PERIOD = 10000
    private val SCAN_STOP_ID = 99
    private var devices: ArrayList<BluetoothDevice>? = null
    private var tryCount: Int = 0
    private val sharedPreference: SharedPreferences? by lazy(LazyThreadSafetyMode.NONE) {
        getSharedPreferences("bleConnected", MODE_PRIVATE)
    }

    val bluetoothAdapter: BluetoothAdapter by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        Log.d(TAG, "BluetoothManager=  $bluetoothManager")
        bluetoothManager.adapter
    }

    private val BLUETOOTH_LE_RN4870_SERVICE =
        UUID.fromString("49535343-FE7D-4AE5-8FA9-9FAFD205E455")
    private val BLUETOOTH_LE_CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private var DEFAULT_MTU = 23
    private var PAYLOADSIZE = DEFAULT_MTU - 3
    private val BLUETOOTH_LE_RN4870_CHAR_RW =
        UUID.fromString("49535343-1e4d-4bd9-ba61-23c647249616")

    inner class LocalBinder : Binder() {
        val service = this@BluetoothLeService
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "onBind 호출됨")
        return binder
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "온스타트 커맨드 서비스 시작됨")
        when (intent?.action) {
            "RemoteStart" -> {
                Log.d(TAG, "RemoteStat 시작됨")
                var deviceAddress = sharedPreference?.getString("deviceAddress", "")
                if (deviceAddress != "" ) {
                    connect(deviceAddress)
                    Toast.makeText(this, "연결이 완료 되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "디바이스 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        startForeground(1, makeNotification())
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "BluetoothLeService Started")
        sharedPreference?.edit { this.putString("isServiceRunning", "true") }
        var br: BroadcastReceiver = serviceBoradcast
        var filter = IntentFilter()
        filter.addAction(DIGITALCOMPAION_MANAGER_TEMPDATA)
        filter.addAction("android.intent.action.ACTION_POWER_CONNECTED")
        registerReceiver(br, filter)
    }

    fun connect(address: String?): Boolean {
        Log.d(TAG, "device Connecting")
        if(address.isNullOrEmpty()){
          Toast.makeText(this, "디바이스 주소가 없습니다", Toast.LENGTH_SHORT).show()
          return false
        }
        var device = bluetoothAdapter.getRemoteDevice(address)
        connectionState = STATE_CONNECTING
        Log.d(TAG, "connectionState = $connectionState")
        bluetoothGatt = device.connectGatt(this, false, gattCallback, TRANSPORT_LE)
        return true
    }

    companion object {
        val ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED"
        val ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED"
        val ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_DISCOVERED"
    }

    fun registerCallback(cb: ICallback) {
        icallBack = cb
    }

    fun scanStart(enable: Boolean, devices: ArrayList<BluetoothDevice>) {
        Log.d(TAG, "bluetoothService scanStart enable = $enable")
        when (enable) {
            true -> {
                this.devices = devices
                devices?.clear()
                scanStopHandler = object : Handler() {
                    override fun handleMessage(msg: Message) {
                        when (msg.what) {
                            99 -> {
                                Log.d(TAG, "scan Stop Message")
                                bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
                            }
                        }
                    }
                }
                scanStopHandler?.sendEmptyMessageDelayed(SCAN_STOP_ID, SCAN_PERIOD.toLong())
                bluetoothAdapter?.bluetoothLeScanner?.startScan(scanCallback)
            }
            else -> {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            }
        }
    }

    fun cancelHandler(id: Int) {
        when (id) {
            SCAN_STOP_ID -> scanStopHandler?.removeMessages(SCAN_STOP_ID)
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d(TAG, "onScanFailed scanError = ${errorCode}")
            when (errorCode) {
                1 -> {
                    Log.d(TAG, "이미 디바이스 서치가 시작됬습니다.")
                }
                else -> {
                    Toast.makeText(this@BluetoothLeService, "스캔에 실패했습니다.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let {
                Log.d(TAG, "onScanResult = ${result}")
                if (!devices!!.contains(it.device)) {
                    Log.d(TAG, "device inserted ${result.device}")
                    devices?.add(it.device)
                    icallBack?.addRecyclerView()
                }
            }
        }
    }

    private fun connectCharacteristics1(gatt: BluetoothGatt): Boolean {
        for (gattService in gatt.services) {
            Log.d(TAG, "gattCharacteristics service = ${gattService.uuid}")
            var gattCharacteristics: List<BluetoothGattCharacteristic> = gattService.characteristics
            when (gattService.uuid) {
                BLUETOOTH_LE_RN4870_SERVICE -> {
                    writeCharacteristic = gattService.getCharacteristic(BLUETOOTH_LE_RN4870_CHAR_RW)
                    readCharacteristic = gattService.getCharacteristic(BLUETOOTH_LE_RN4870_CHAR_RW)
                    Log.d(TAG, "gattCharacteristic uuid = ${writeCharacteristic!!.uuid}")
                    connectCharacteristics3(gatt)
                }
            }
        }
        return false
    }

    fun connectCharacteristics3(gatt: BluetoothGatt) {
        gatt.setCharacteristicNotification(readCharacteristic, true);
        gatt.setCharacteristicNotification(readCharacteristic, true);
        var writeProperties = writeCharacteristic!!.properties
        Log.d(TAG, "writeProperties $writeProperties")
        if (writeProperties and BluetoothGattCharacteristic.PROPERTY_WRITE +  // Microbit,HM10-clone have WRITE
            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE == 0
        ) { // HM10,TI uart,Telit have only WRITE_NO_RESPONSE
            Log.d(TAG, "writeProperties erorr occured 1")
            return
        }
        if (!gatt.setCharacteristicNotification(readCharacteristic, true)) {
            Log.d(TAG, "writeProperties erorr occured 2")
            return
        }
        var bluetoothDescriptor: BluetoothGattDescriptor = readCharacteristic!!.getDescriptor(
            BLUETOOTH_LE_CCCD
        )
        Log.d(TAG, "bluetoothDescriptor ${bluetoothDescriptor}")
        if (bluetoothDescriptor == null) {
            Log.d(TAG, "writeProperties erorr occured 3")
            return
        }
        var readProperties = readCharacteristic!!.properties
        Log.d(TAG, "readChracter ")
        if (readProperties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
            Log.d(TAG, "enable read indication")
            bluetoothDescriptor.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        }
        if (readProperties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
            Log.d(TAG, "enable read notification")
            bluetoothDescriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        } else {
            Log.d(TAG, "writeProperties erorr occured 4")
            return
        }
        gatt.writeDescriptor(bluetoothDescriptor)
    }

    fun send(str: String) {
        Log.d(TAG, "send str = $str")
        try {
            val msg: String
            val data: ByteArray
            val sb = StringBuilder()
            var textUtil = TextUtil.create()
            textUtil!!.toHexString(sb, textUtil.fromHexString(str)!!)
            Log.d(TAG, "sb toString = ${sb}")
            textUtil!!.toHexString(sb, textUtil.newline_crlf.toByteArray())
            msg = sb.toString()
            Log.d(TAG, "msg = $msg")
            data = textUtil!!.fromHexString(msg)!!
            val spn = SpannableStringBuilder(
                """
                     $msg
                     
                     """.trimIndent()
            )
            spn.setSpan(
                ForegroundColorSpan(resources.getColor(R.color.purple_700)),
                0,
                spn.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            Log.d(TAG, "sp = ${data}")
            write(data)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun write(data: ByteArray) {
        if(connectionState == STATE_CONNECTED){
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            var data0: ByteArray
            synchronized(writeBuffer) {
                if (data.size <= PAYLOADSIZE) {
                    data0 = data
                } else {
                    data0 = data.copyOfRange(0, PAYLOADSIZE-1)
                }
                for (dataByte in data0) {
                    Log.d(
                        TAG,
                        "writeCharacteristic = " + String.format("%02x ", dataByte)
                    )
                }
                writeCharacteristic?.value = data0
                Log.d(TAG, "writeCharacteristic = " + writeCharacteristic?.uuid?.toString())
                bluetoothGatt?.writeCharacteristic(writeCharacteristic)
            }
        }else{
            Log.d(TAG,"[wirte] 연결안됨")
            sendBroadcast(Intent(CommVal.DATA_ACTION).putExtra("data","notConnected"))
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            Log.d(TAG, "newState = $newState")
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    sharedPreference?.edit { this.putString("isConnected", "true") }
                    sharedPreference?.edit {
                        this.putString(
                            "deviceAddress",
                            gatt!!.device.address
                        )
                    }
                    sharedPreference?.edit { this.putString("deviceName", gatt!!.device.name) }
                    Log.d(TAG, "gatt device address =  ${gatt!!.device.address}")
                    connectionState = STATE_CONNECTED
                    broadcastUpdate(ACTION_GATT_CONNECTED)
                    var isGattServiceConnect = gatt!!.discoverServices()
                    Log.d(TAG, "isGattServicecon = $isGattServiceConnect")
                    tryCount = 0
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG,"Bluetooth STATE DISCONNECTED")
                    connectionState = STATE_DISCONNECTED
                    bluetoothGatt?.disconnect()
                    bluetoothGatt?.close()
                    bluetoothGatt = null
                    var reconnectDevcie = sharedPreference?.getString("deviceAddress", "")
                    if(!reconnectDevcie.isNullOrEmpty()){connect(reconnectDevcie)    }

                    broadcastUpdate(ACTION_GATT_DISCONNECTED)
                    //버튼을 눌러서 직접 끊는 것과 여러 다른 상황으로 끊기는 걸 구분해야 한다.
                    //그냥 끊길때 일단 conn을 정리하자.
                    //블루투스 연결이 끊기면 다시 connect try 를하자//
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            Log.d(TAG, "serviceDiscovered")
            connectCharacteristics1(gatt!!)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {Log.d(TAG,"onCharacteristicRead = ${characteristic!!.descriptors} characteris value = ${characteristic.value} characet = ${characteristic.properties} charater = ${characteristic.descriptors.get(0)}")}

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            Log.d(
                TAG,
                "characteristic onCharacteristicChanged = ${characteristic!!.descriptors} " +
                        "characteris value = ${characteristic.value} characet = ${characteristic.properties}  charater = ${
                            characteristic.descriptors.get(
                                0
                            )
                        }" +
                        "charateris get String value = ${characteristic.getStringValue(0)}" +
                        ""
            )
            super.onCharacteristicChanged(gatt, characteristic)
            var data: ByteArray = characteristic.value
            var stringData = TextUtil.create().toHexString(data)
            Log.d(TAG,"stringData length = ${stringData.length}")
            if(stringData.length > 20){
                broadcastUpdate(DATA_ACTION, stringData)
            }
            Log.d(TAG, TextUtil.create().toHexString(data))
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            Log.d(
                TAG,
                "characteristic write gatt = ${gatt} characteristic = ${characteristic} status = ${status}"
            )
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            Log.d(TAG, "characteristic onMuteChange gatt = ${gatt} mtu = ${mtu} status = ${status}")

            super.onMtuChanged(gatt, mtu, status)
        }
    }

    private fun broadcastUpdate(action: String) {
        sendBroadcast(Intent(action))
    }

    private fun broadcastUpdate(action: String, data: String) {
        sendBroadcast(Intent(action).putExtra("data", data))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun makeNotification(): Notification {
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "myService",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        var notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Foreground Service Kotlin Example")
            .setContentText("MainService")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    0
                )
            )
            .build()
        return notification
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "bluetoothServcie onDestory")
        sharedPreference?.edit { this.putString("isServiceRunning", "false") }
        try {
            unregisterReceiver(serviceBoradcast)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }


    var serviceBoradcast = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG,"intent action = $intent?.action")
            when (intent?.action) {
                DIGITALCOMPAION_MANAGER_TEMPDATA -> {
                    if (connectionState != BluetoothGatt.STATE_CONNECTED) {
                        Log.d(TAG,"connectionState = "+connectionState)
                        var reconnectDevcie = sharedPreference?.getString("deviceAddress", "")
                        if(!reconnectDevcie.isNullOrEmpty())connect(reconnectDevcie)
                    }
                    when (intent.getStringExtra("request")) {
                        "getTemperature" -> {send(CommVal.read)}
                        "turnOn" -> {send(CommVal.turnOn)}
                        "turnOff" -> {send(CommVal.turnOff)}
                    }
                }
            }
        }
    }

    interface ICallback {
        fun addRecyclerView()
    }
}

