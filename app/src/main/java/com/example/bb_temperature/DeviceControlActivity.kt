package com.example.bb_temperature

import android.bluetooth.BluetoothGattCharacteristic
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import java.lang.Exception
import java.util.*

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

    private val serviceConnection = object:ServiceConnection{
        override fun onServiceDisconnected(name: ComponentName?) {
            bluetoothService = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            bluetoothService = (service as BluetoothLeService.LocalBinder).service
            if(isConnected.equals("false")){
                bluetoothService?.connect(deviceAddress)
            }
            Log.d(TAG, "blueToothService Connected")
        }
    }

    private val sharedPreference:SharedPreferences? by lazy(LazyThreadSafetyMode.NONE){
        getSharedPreferences("bleConnected", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment_result)
        deviceAddress = intent.getStringExtra("deviceAddress")
        Log.d(TAG,"deviceAddress = ${deviceAddress}")
        deviceName = intent.getStringExtra("deviceName")
        disconnectTextView = findViewById<TextView>(R.id.disconnect_btn)
        disconnectTextView!!.setOnClickListener {
            bluetoothService?.bluetoothGatt?.disconnect()
            Log.d(TAG,"bluetoothService = ${bluetoothService} bluetoothGatt = ${bluetoothService?.bluetoothGatt}")
            bluetoothService?.bluetoothGatt?.close()
            sharedPreference?.edit {this.putString("isConnected","false")}
            sharedPreference?.edit { this.putString("deviceAddress","")}
            sharedPreference?.edit { this.putString("deviceName","")}
            bluetoothService?.stopSelf()
            finish()
        }
        isConnected = sharedPreference?.getString("isConnected","false")
        Log.d(TAG,"isConnected = $isConnected")
        var isServiceRunnig:String? = sharedPreference?.getString("isServiceRunning","false")
        val gattServiceIntent = Intent(
            this@DeviceControlActivity,
            BluetoothLeService::class.java
        )
        if(isServiceRunnig.equals("false")){
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(gattServiceIntent)
            }else{
                startService(gattServiceIntent)
            }
        }
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        tv = findViewById(R.id.result_tv)
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
                it.send(
                    byteString
                )
            }
            var turnOffBtn = findViewById<View>(R.id.turnoffBtn)
            turnOffBtn.setOnClickListener {
                var byteString = "02 52 54 30 03 35"
                bluetoothService?.let {
                    it.send(
                        byteString
                    )
                }
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
            Log.d(TAG,"bradcastAction = ${intent?.action}")
            when(intent?.action){
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    connected = true
                    disconnectTextView?.text = "연결 끊기"
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    connected = false
                    disconnectTextView?.text = "연결 하기"
                    Toast.makeText(this@DeviceControlActivity, "블루투스 연결이 끊겼습니다", Toast.LENGTH_SHORT)
                        .show()
                }
                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED -> {
                    Toast.makeText(context, "준비됬습니다.", Toast.LENGTH_SHORT).show()
                }
                DATA_ACTION->{
                    Log.d(TAG,"dataAction = ${intent.getStringExtra("data")}")
                    var tvText = "${tv?.text}\n"
                    tv?.text = tvText + intent.getStringExtra("data")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(gattUpdateReceiver)
        }catch (e:Exception){
            e.printStackTrace()
        }
    }
}

