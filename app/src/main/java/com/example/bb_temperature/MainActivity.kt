package com.example.bb_temperature

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
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
    private var requiredPermission = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    private val SCAN_PERIOD = 10000
    private val REQUEST_ENABLE_BT = 1011
    private val BluetoothAdapter.isDisabled:Boolean get() = !isEnabled

    private var mScanning:Boolean = false


    private val singlePermissionCode = 99
    private val multiplePermissionCode = 100
    private var recyclerView:RecyclerView?=null
    private var recyclerAdapter:RecyclerAdapter?=null;
    private var devices = ArrayList<BluetoothDevice>()
    private val handler = Handler()

    private val bluetoothAdapter:BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE){
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val sharedPreference: SharedPreferences? by lazy(LazyThreadSafetyMode.NONE){
        getSharedPreferences("bleConnected", MODE_PRIVATE)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity Created")
        setContentView(R.layout.activity_main)

        packageManager.takeIf { it.missingSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) }?.also {
            Toast.makeText(this, "이 앱은 블루투스를 지원하지 않습니다", Toast.LENGTH_SHORT).show()
            finish()
        }
        checkSelfPermission()
        val button : Button = findViewById(R.id.service_btn)
        button.setOnClickListener {
            bluetoothAdapter?.takeIf { it.isDisabled }?.apply {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
            scanBluetooth(true)
        }
        recyclerView = findViewById(R.id.device_recycler)
        recyclerAdapter = RecyclerAdapter(devices)
        recyclerView!!.adapter = recyclerAdapter
        button.setOnClickListener {
            recyclerAdapter?.setClickItem(object:RecyclerAdapter.ClickItem{
                override fun onClick() {
                    bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
                }
            })
            scanBluetooth(true)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK){
            scanBluetooth(true)
        }
    }

    override fun onResume() {
        super.onResume()
        var isConnected = sharedPreference?.getString("isConnected","false")
        var deviceName = sharedPreference?.getString("deviceName","noName")
        var deviceAddress = sharedPreference?.getString("deviceAddress","")
        if(isConnected.equals("true") && !deviceAddress.isNullOrEmpty()){
            startActivity(Intent(this,DeviceControlActivity::class.java).putExtra("deviceName",deviceName).putExtra("deviceAddress",deviceAddress).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }
    }

    private fun scanBluetooth(enable: Boolean){
        Log.d(TAG, "scanLeDevice Enable = ${enable}")
        when(enable){
            true -> {
                handler.postDelayed({
                    mScanning = false
                    bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
                }, SCAN_PERIOD.toLong())
                mScanning = true
                recyclerView?.let { it ->
                    devices.clear()
                    it.recycledViewPool.clear()
                    recyclerAdapter?.notifyDataSetChanged()
                }
                bluetoothAdapter?.bluetoothLeScanner?.startScan(scanCallback)
            }
            else->{
                mScanning = false
                bluetoothAdapter!!.bluetoothLeScanner.stopScan(scanCallback)
            }
        }
    }

    private val scanCallback = object:ScanCallback(){
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Toast.makeText(this@MainActivity, "스캔에 실패했습니다.", Toast.LENGTH_SHORT).show()
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let{
                Log.d(TAG, "onScanResult = ${result.toString()}")
                if(!devices.contains(it.device)){
                    Log.d(TAG, "device Inserted ${result.device}")
                    devices.add(it.device)
                    recyclerAdapter!!.notifyDataSetChanged()
                }
            }
        }
        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.let{
                Log.d(TAG, "onBatchScanResults = ${results}")
                for(result in it){
                    if(!devices.contains(result.device))devices.add(result.device)
                }
            }
        }
    }

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
}