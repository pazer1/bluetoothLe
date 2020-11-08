package com.example.bb_temperature

import TextUtil
import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import java.util.*

class BluetoothLeService:Service()  {
    private val TAG:String = "BluetoothLeService"
    val STATE_DISCONNECTED = 0
    val STATE_CONNECTING = 1
    val STATE_CONNECTED = 2
    var connectionState = STATE_DISCONNECTED
    var bluetoothGatt:BluetoothGatt? = null
    val binder = LocalBinder()
    private var writeCharacteristic:BluetoothGattCharacteristic? = null
    private var readCharacteristic:BluetoothGattCharacteristic? = null
    private var writeBuffer = arrayListOf<ByteArray>()



    val bluetoothAdapter:BluetoothAdapter by lazy(LazyThreadSafetyMode.NONE){
        val bluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        Log.d(TAG, "BluetoothManager=  $bluetoothManager")
        bluetoothManager.adapter
    }

    private val BLUETOOTH_LE_RN4870_SERVICE =
        UUID.fromString("49535343-FE7D-4AE5-8FA9-9FAFD205E455")
    private val BLUETOOTH_LE_CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private var DEFAULT_MTU = 23
    private var payloadSize = DEFAULT_MTU-3
    private val BLUETOOTH_LE_RN4870_CHAR_RW =
        UUID.fromString("49535343-1e4d-4bd9-ba61-23c647249616")

    inner class LocalBinder : Binder(){val service = this@BluetoothLeService}

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "onBind 호출됨")
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "온스트 커맨드 서비스 시작됨")
        return super.onStartCommand(intent, flags, startId)
        
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "BluetoothLeService Started")
    }
    
    fun connect(address: String):Boolean{
        var device = bluetoothAdapter.getRemoteDevice(address)
        bluetoothGatt = device.connectGatt(this, false, gattCallback, 2)
        bluetoothGatt?.let{
                return if(it.connect()){
                    connectionState = STATE_CONNECTING
                    true
                }else false
        }
        connectionState = STATE_CONNECTING
        Log.d(TAG, "connectionState = $connectionState")
        return true
    }
    companion object {
        val ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED"
        val ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED"
        val ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_DISCOVERED"
    }

    private fun connectCharacteristics1(gatt: BluetoothGatt):Boolean{
        for(gattService in gatt.services){
            Log.d(TAG, "gattCharacteristics service = ${gattService.uuid}")
            var gattCharacteristics:List<BluetoothGattCharacteristic> = gattService.characteristics
            when(gattService.uuid){
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
    fun connectCharacteristics3(gatt: BluetoothGatt){
        gatt.setCharacteristicNotification(readCharacteristic,true);
        gatt.setCharacteristicNotification(readCharacteristic,true);
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
        var bluetoothDescriptor:BluetoothGattDescriptor = readCharacteristic!!.getDescriptor(
            BLUETOOTH_LE_CCCD
        )
        Log.d(TAG, "bluetoothDescriptor ${bluetoothDescriptor}")
        if (bluetoothDescriptor == null) {
            Log.d(TAG, "writeProperties erorr occured 3")
            return
        }
        var readProperties = readCharacteristic!!.properties
        Log.d(TAG,"readChracter ")
        if (readProperties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
            Log.d(TAG, "enable read indication")
            bluetoothDescriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
        }
        if (readProperties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
            Log.d(TAG, "enable read notification")
            bluetoothDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
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
            Log.d(TAG, "sb toString = ${sb.toString()}")
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

    public fun write(data: ByteArray){
        var data0:ByteArray
        synchronized(writeBuffer){
            if(data.size <= payloadSize){
                data0 = data
            }else{
                data0 = data.copyOfRange(0, payloadSize)
            }
            for (dataByte in data0) {
                Log.d(
                    TAG,
                    "writeCharacteristic = " + String.format("%02x ", dataByte)
                )
            }
            writeCharacteristic!!.setValue(data0)
            Log.d(TAG, "writeCharacteristic = " + writeCharacteristic!!.uuid.toString())
            bluetoothGatt!!.writeCharacteristic(writeCharacteristic)
        }
    }

    val gattCallback = object:BluetoothGattCallback(){
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            var intentAction = ""
            when(newState){
                BluetoothProfile.STATE_CONNECTED -> {
                    connectionState = STATE_CONNECTED
                    broadcastUpdate(ACTION_GATT_CONNECTED)
                    var isGattServiceConnect = gatt!!.discoverServices()
                    Log.d(TAG, "isGattServicecon = $isGattServiceConnect")

//                    bluetoothManager.openGattServer(this@BluetoothLeService,bluetoothGatt)
//                    gatt.also {
//                        it?.let{it.writeCharacteristic()}
                    Log.d(
                        TAG,
                        "device Connected gattDevice = ${gatt!!.device} gattService = ${gatt.services} "
                    )
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectionState = STATE_DISCONNECTED
                    broadcastUpdate(ACTION_GATT_DISCONNECTED)
                    Log.d(TAG, "device disConnected")
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            Log.d(TAG, "serviceDed")
            connectCharacteristics1(gatt!!)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            Log.d(
                TAG,
                "onCharacteristicRead = ${characteristic!!.descriptors} characteris value = ${characteristic.value} characet = ${characteristic.properties} charater = ${characteristic.descriptors.get(0)}"
            )
            when(status){
                BluetoothGatt.GATT_SUCCESS -> run {
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {

            Log.d(
                TAG,
                "characteristic onCharacteristicChanged = ${characteristic!!.descriptors} " +
                        "characteris value = ${characteristic.value} characet = ${characteristic.properties}  charater = ${characteristic.descriptors.get(0)}" +
                        "charateris get String value = ${characteristic.getStringValue(0)}" +
                        ""
            )
            super.onCharacteristicChanged(gatt, characteristic)
            var data:ByteArray = characteristic.value
            var stringData = TextUtil.create().toHexString(data)
            broadcastUpdate("DATA_ACTION",stringData)
            Log.d(TAG,TextUtil.create().toHexString(data))
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

//    private val gattServerCallback = object : BluetoothGattServerCallback() {
//        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
//            if(newState == BluetoothProfile.STATE_CONNECTED){
//                Log.d(TAG, "BluetoothDevice Connect =  $device")
//            }else if (newState == BluetoothProfile.STATE_DISCONNECTED){
//                Log.d(TAG, "BluetoothDevice Disconnecte =  $device")
//            }
//        }
//
//        override fun onCharacteristicReadRequest(
//            device: BluetoothDevice?,
//            requestId: Int,
//            offset: Int,
//            characteristic: BluetoothGattCharacteristic?
//        ) {
//            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
//            Log.d(TAG, "BluetoothDevice onCharacteristicReadRequest Character =  $characteristic")
//        }
//
//        override fun onDescriptorReadRequest(
//            device: BluetoothDevice?,
//            requestId: Int,
//            offset: Int,
//            descriptor: BluetoothGattDescriptor?
//        ) {
//            super.onDescriptorReadRequest(device, requestId, offset, descriptor)
//            Log.d(
//                TAG,
//                "BluetoothDevice onCharacteristicDescriptionRequest Character =  ${descriptor!!.characteristic}"
//            )
//        }
//    }
//
//    public fun getSupportedGattServices():MutableList<BluetoothGattService> = bluetoothGattServiceList
//
//
//    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, data: ByteArray){
//        Log.d(TAG, "writeCharacteristic data = $data")
//        Log.d(TAG, "writeCharacteristic BluetoothGattCharacteristic = ${characteristic.uuid}")
//        characteristic.value = data
//        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
//        bluetoothGatt?.writeCharacteristic(characteristic)
//        for(bluetoothService in bluetoothGattServiceList){
//            for(characteristic in bluetoothService.characteristics){
//            }
//        }
//    }
//
//    private fun sendData(data: String){
//
//    }


    private fun broadcastUpdate(action: String){
        sendBroadcast(Intent(action))
    }
    private fun broadcastUpdate(action: String,data:String){
        sendBroadcast(Intent(action).putExtra("data",data))
    }
}

