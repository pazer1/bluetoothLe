package com.example.bb_temperature

import android.Manifest
import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView


class MainActivity : AppCompatActivity() {

    private fun PackageManager.missingSystemFeature(name: String):Boolean = !hasSystemFeature(name)

    private val TAG = MainActivity::class.java.simpleName
    private var requiredPermission = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    private var currentTime:Long?=0
    private var isServiceRunning:Boolean? = false
    private val SCAN_STOP_ID = 99
    private val REQUEST_ENABLE_BT = 1011
    private val BluetoothAdapter.isDisabled:Boolean get() = !isEnabled
    private val singlePermissionCode = 99
    private val multiplePermissionCode = 100
    private var recyclerView:RecyclerView?=null
    private var recyclerAdapter:RecyclerAdapter?=null
    private var devices = ArrayList<BluetoothDevice>()
    private var bleCustomService:BluetoothLeService? = null
    private var button:Button? = null
    private val myServiceName = ".BluetoothLeService"

    //변수에 접근할 때 객체생성해서 참조
    private val bluetoothAdapter:BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE){
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private val sharedPreference: SharedPreferences? by lazy(LazyThreadSafetyMode.NONE){getSharedPreferences("bleConnected", MODE_PRIVATE)}

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "[onCreate]MainActivity Created")
        setContentView(R.layout.activity_main)

        packageManager.takeIf { it.missingSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) }?.also {
            Toast.makeText(this, "이 앱은 블루투스를 지원하지 않습니다", Toast.LENGTH_SHORT).show()
            finish()
        }
        checkSelfPermission()

        button = findViewById(R.id.service_btn)
        recyclerView = findViewById(R.id.device_recycler)
        button?.setOnClickListener {
            bluetoothAdapter?.takeIf { it.isDisabled }.let {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
            bleCustomService?.cancelHandler(SCAN_STOP_ID)
            devices.clear()
            recyclerAdapter?.notifyDataSetChanged()
            startCustomBleService();
        }
        recyclerAdapter = RecyclerAdapter(devices)
        recyclerAdapter!!.setClickItem{
            bleCustomService?.scanStart(false,devices)
        }
        recyclerView!!.adapter = recyclerAdapter
    }



    private fun startCustomBleService(){
        var isServiceRunnig = sharedPreference?.getString("isServiceRunning", "false")
        if(isServiceRunnig.equals("false") || !isMyServiceRunning()){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(Intent(this@MainActivity, BluetoothLeService::class.java))
            else startService(Intent(this@MainActivity, BluetoothLeService::class.java))
        }
        bindService(Intent(this@MainActivity, BluetoothLeService::class.java),serviceConnection,Context.BIND_AUTO_CREATE)
        takeIf { isServiceRunning!!}?:bleCustomService?.let { it.scanStart(true,devices)}
    }

    private val serviceConnection = object: ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            bleCustomService = null
            isServiceRunning = false
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "customBleService Connected")
            bleCustomService = (service as BluetoothLeService.LocalBinder).service
            bleCustomService?.registerCallback(iCallback)
            bleCustomService?.scanStart(true, devices)
            isServiceRunning = true
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK){
            startCustomBleService()
        }
    }

    override fun onResume() {
        super.onResume()
        devices.takeIf { it.size>0 }?: run {
            devices.clear()
            recyclerAdapter?.notifyDataSetChanged()
        }
        var isConnected = sharedPreference?.getString("isConnected", "false")
        var deviceName = sharedPreference?.getString("deviceName", "noName")
        var deviceAddress = sharedPreference?.getString("deviceAddress", "")
        Log.d(TAG,"[onResume] isMyserviceRunning = ${isMyServiceRunning()}")
        if(isConnected.equals("true") && !deviceAddress.isNullOrEmpty() && isMyServiceRunning()){
            startActivity(
                Intent(this, DeviceControlActivity::class.java).putExtra("deviceName",deviceName).putExtra("deviceAddress", deviceAddress).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
        }
    }

    private val iCallback = (object:BluetoothLeService.ICallback{
        override fun addRecyclerView() {
            recyclerAdapter?.notifyDataSetChanged()
        }
    })

    private fun checkSelfPermission(){
        var rejectPermissionList = ArrayList<String>()
        for(permission in requiredPermission){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                rejectPermissionList.add(permission)
            }
        }
        if(ActivityCompat.shouldShowRequestPermissionRationale(
                this@MainActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            )){
            Toast.makeText(this@MainActivity, "위치권한을 허가하지 않으면 앱을 실행시킬수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }else if(rejectPermissionList.isNotEmpty()){
            Log.d(TAG, "rejectedPermission List = " + rejectPermissionList.get(0))
            var array = arrayOfNulls<String>(rejectPermissionList.size)
            ActivityCompat.requestPermissions(
                this,
                rejectPermissionList.toArray(array),
                multiplePermissionCode
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode){
            multiplePermissionCode -> {
                if (grantResults.isNotEmpty()) {
                    for ((i, permission) in permissions.withIndex()) {
                        ActivityCompat.requestPermissions(
                            this,
                            requiredPermission,
                            singlePermissionCode
                        )
                    }
                }
            }
        }
    }

    private fun isMyServiceRunning(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        Log.d(TAG,"[isMyServiceRunning] myServiceName = $myServiceName")
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if(service.service.shortClassName == myServiceName){
                return true
            }
        }
        return false
    }
}