package com.example.bb_temperature

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.timerTask


class DeviceControlActivity : AppCompatActivity(){

    private val TAG = DeviceControlActivity::class.simpleName
    private var deviceAddress:String = ""
    private var bluetoothService:BluetoothLeService? = null
    private val DATA_ACTION = "TEMPERATURE_DATA_ACTION"
    private var deviceName = ""
    private var disconnectTextView:TextView? = null
    var connected:Boolean = false
    private var tv:TextView? = null
    private var isConnected:String? =""
    private var simpleDataFormat:SimpleDateFormat? = null

    private val serviceConnection = object:ServiceConnection{
        override fun onServiceDisconnected(name: ComponentName?) {
            bluetoothService = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG,"bindService find")
            bluetoothService = (service as BluetoothLeService.LocalBinder).service
            if(deviceAddress == "")deviceAddress= sharedPreference?.getString("deviceAddress", "").toString()
            bluetoothService?.connect(deviceAddress)
            Log.d(TAG, "blueToothService Connected = ${bluetoothService}")
        }
    }

    private val sharedPreference:SharedPreferences? by lazy(LazyThreadSafetyMode.NONE){
        getSharedPreferences("bleConnected", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment_result)
        simpleDataFormat = SimpleDateFormat("hh:mm:ss")
        deviceAddress = intent.getStringExtra("deviceAddress")
        Log.d(TAG, "deviceAddress = ${deviceAddress}")
        deviceName = intent.getStringExtra("deviceName")
        disconnectTextView = findViewById<TextView>(R.id.disconnect_btn)
        disconnectTextView!!.setOnClickListener {
            bluetoothService?.bluetoothGatt?.disconnect()
            Log.d(
                TAG,
                "bluetoothService = ${bluetoothService} bluetoothGatt = ${bluetoothService?.bluetoothGatt}"
            )
            bluetoothService?.bluetoothGatt?.close()
            sharedPreference?.edit {this.putString("isConnected", "false")}
            sharedPreference?.edit { this.putString("deviceAddress", "")}
            sharedPreference?.edit { this.putString("deviceName", "")}
            try{
                bluetoothService?.unbindService(serviceConnection)
            }catch (e: Exception){
                e.printStackTrace()
            }
            bluetoothService?.stopForeground(true)
            finish()
        }
        isConnected = sharedPreference?.getString("isConnected", "false")
        Log.d(TAG, "isConnected = $isConnected")
        tv = findViewById(R.id.result_tv)
        tv!!.movementMethod = ScrollingMovementMethod()
        findViewById<TextView>(R.id.device_name).also {
            it.text = deviceName
        }

        var turnOnBtn = findViewById<View>(R.id.turnBtn)
        turnOnBtn.setOnClickListener {
            Log.d(TAG, "deviceAddress = ${deviceAddress}")
            var byteString = "02 52 54 31 03 34"
            bluetoothService?.let {
                    it.send(
                        byteString
                    )
            }
        }
        var readBtn = findViewById<View>(R.id.readBtn)
        readBtn.setOnClickListener {
            Log.d(TAG, "readBtn")
            var byteString = "02 52 44 03 15"
            bluetoothService?.let {
                Timer().schedule(timerTask {
                    Log.d(TAG, "timerTask")
                    it.send(
                        byteString
                    )
                }, 10000, 3000)
            }
        }
        var turnOffBtn = findViewById<View>(R.id.turnoffBtn)
        turnOffBtn.setOnClickListener {
            var byteString = "02 52 54 30 03 35"
            bluetoothService?.let {
                Log.d(TAG,"bluetoothTurnOff btn Pressed")
                it.send(
                    byteString
                )
            }
        }
        var filter = IntentFilter()
        filter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
        filter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
        filter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
        filter.addAction(DATA_ACTION)
        registerReceiver(gattUpdateReceiver, filter)
    }
    private var gattUpdateReceiver = object:BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "bradcastAction = ${intent?.action}")
            when(intent?.action){
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    connected = true
                    Toast.makeText(context, "연결 되었습니다.", Toast.LENGTH_SHORT).show()
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    connected = false
                    Toast.makeText(this@DeviceControlActivity, "블루투스 연결이 끊겼습니다", Toast.LENGTH_SHORT)
                        .show()
                }
                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED -> {
                    Toast.makeText(context, "준비됬습니다.", Toast.LENGTH_SHORT).show()
                }
                DATA_ACTION -> {
                    Log.d(TAG, "dataAction = ${intent.getStringExtra("data")}")
                    tv?.text = "[${simpleDataFormat?.format(System.currentTimeMillis())}]" +intent.getStringExtra("data")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        bindService(
            Intent(this, BluetoothLeService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(gattUpdateReceiver)
        }catch (e: Exception){
            e.printStackTrace()
        }
    }
}

