package com.example.bb_temperature

import android.bluetooth.BluetoothGattCharacteristic
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class DeviceControlActivity : AppCompatActivity(),View.OnClickListener  {

    companion object {
        val UUID_DATA_WRITE = UUID.fromString("49535343-1e4d-4bd9-ba61-23c647249616")
    }

    private val BLUETOOTH_LE_RN4870_SERVICE =
        UUID.fromString("49535343-FE7D-4AE5-8FA9-9FAFD205E455")
    private val BLUETOOTH_LE_CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private var DEFAULT_MTU = 23
    private var payloadSize = DEFAULT_MTU-3
    private val BLUETOOTH_LE_RN4870_CHAR_RW =
        UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616")
    private val writeUUid = "49535343-fe7d-4ae5-8fa9-9fafd205e455"
    private val readUUid = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616")

    private var writeCharacteristic:BluetoothGattCharacteristic? = null
    private var readCharacteristic:BluetoothGattCharacteristic? = null

    private val TAG = DeviceControlActivity::class.simpleName
    private var deviceAddress:String = ""
    private var bluetoothService:BluetoothLeService? = null
    var connected:Boolean = false

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
        val gattServiceIntent = Intent(
            this@DeviceControlActivity,
            BluetoothLeService::class.java
        )
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        var turnOnBtn = findViewById<View>(R.id.turnBtn)

        turnOnBtn.setOnClickListener {
            Log.d(TAG, "deviceAddress = ${deviceAddress}")
            var byteString = "02 52 44 03 15"
            bluetoothService?.let {
                bluetoothService!!.send(
                    byteString
                )
            }

        }
        var readBtn = findViewById<View>(R.id.readBtn)
        readBtn.setOnClickListener {
            Log.d(TAG, "readBtn")
            var byteString = "02 52 54 31 03 34"
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

//        Log.d(TAG,"isBindService = ${isBindService}")
            var br: BroadcastReceiver = gattUpdateReceiver
            var filter = IntentFilter()
            filter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            filter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            filter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
            registerReceiver(br, filter)
        }
    }

    var gattUpdateReceiver = object:BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
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
            }
        }
    }





    override fun onClick(v: View?) {
        TODO("Not yet implemented")
    }
}