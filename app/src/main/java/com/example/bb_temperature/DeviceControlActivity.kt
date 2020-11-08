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
import java.util.*

class DeviceControlActivity : AppCompatActivity(){

    private val TAG = DeviceControlActivity::class.simpleName
    private var deviceAddress:String = ""
    private var bluetoothService:BluetoothLeService? = null
    private val DATA_ACTION = "DATA_ACTION"
    var connected:Boolean = false
    private var tv:TextView? = null

    private val serviceConnection = object:ServiceConnection{
        override fun onServiceDisconnected(name: ComponentName?) {
            bluetoothService = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            bluetoothService = (service as BluetoothLeService.LocalBinder).service
            bluetoothService?.connect(deviceAddress)
            Log.d(TAG, "blueToothService Connected")
            Log.d(TAG, "blueToothService Connected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment_result)
        deviceAddress = intent.getStringExtra("address")
        tv = findViewById(R.id.result_tv)
        val gattServiceIntent = Intent(
            this@DeviceControlActivity,
            BluetoothLeService::class.java
        )
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        var turnOnBtn = findViewById<View>(R.id.turnBtn)

        turnOnBtn.setOnClickListener {
            Log.d(TAG, "deviceAddress = ${deviceAddress}")

            var byteString = "02 52 54 31 03 34"
            bluetoothService?.let {
                bluetoothService!!.send(
                    byteString
                )
            }

        }
        var readBtn = findViewById<View>(R.id.readBtn)
        readBtn.setOnClickListener {
            Log.d(TAG, "readBtn")
            var byteString = "02 52 44 03 15"
            bluetoothService?.let {
                bluetoothService!!.send(
                    byteString
                )
            }
            var turnOffBtn = findViewById<View>(R.id.turnoffBtn)
            turnOffBtn.setOnClickListener {
                var byteString = "02 52 54 30 03 35"
                bluetoothService?.let {
                    bluetoothService!!.send(
                        byteString
                    )
                }
            }
            var br: BroadcastReceiver = gattUpdateReceiver
            var filter = IntentFilter()
            filter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            filter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            filter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
            filter.addAction(DATA_ACTION)
            registerReceiver(br, filter)
        }
    }
    var gattUpdateReceiver = object:BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG,"bradcastAction = ${intent?.action}")
            when(intent?.action){
                BluetoothLeService.ACTION_GATT_CONNECTED -> connected = true
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    connected = false
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
}

