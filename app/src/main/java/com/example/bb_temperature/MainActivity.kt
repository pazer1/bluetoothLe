package com.example.bb_temperature

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private fun PackageManager.missingSystemFeature(name:String):Boolean = !hasSystemFeature(name)

    private val TAG = MainActivity::class.java.simpleName
    private var requiredPermission = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    private val singlePermissionCode = 99
    private val multiplePermissionCode = 100
    private var recyclerView:RecyclerView?=null
    private var recyclerAdapter:RecyclerAdapter?=null;
    private var mScanning:Boolean = false
    private var arrayDevices = ArrayList<BluetoothDevice>()
    private val handler = Handler()
    private val SCAN_PERIOD = 10000
    private val scanCallback = object:ScanCallback(){
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Toast.makeText(this@MainActivity, "스캔에 실패했습니다.", Toast.LENGTH_SHORT).show()
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let{
                Log.d(TAG,"onScanResult = ${result.toString()}")
                if(!arrayDevices.contains(it.device)){
                    Log.d(TAG,"device Inserted ${result.device}")
                    arrayDevices.add(it.device)
                    recyclerAdapter!!.notifyDataSetChanged()
                }
            }
        }
        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.let{
                Log.d(TAG,"onBatchScanResults = ${results}")
                for(result in it){
                    if(!arrayDevices.contains(result.device))arrayDevices.add(result.device)
                }
            }
        }
    }

    private fun scanLeDevice(enable:Boolean){
        Log.d(TAG,"scanLeDevice Enable = ${enable}")
        when(enable){
            true->{
                handler.postDelayed({
                    mScanning = false
                    bluetoothAdapter!!.bluetoothLeScanner.stopScan(scanCallback)
                },SCAN_PERIOD.toLong())
                mScanning = true
                arrayDevices.clear()
                recyclerView?.let{
                    Log.d(TAG,"scanLeDevice recycler not null")
                    it.recycledViewPool.clear()
                    recyclerAdapter?.let {
                        Log.d(TAG,"scanLeDevice recyclerAdapter not null")
                        it.notifyDataSetChanged() }
                }
                bluetoothAdapter!!.bluetoothLeScanner.startScan(scanCallback)
            }
            else->{
                mScanning = false
                bluetoothAdapter!!.bluetoothLeScanner.stopScan(scanCallback)
            }
        }
    }

    private val bluetoothAdapter:BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE){
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG,"MainActivity Created")
        setContentView(R.layout.activity_main)
        checkSelfPermission()
        val button : Button = findViewById(R.id.service_btn)
        recyclerView = findViewById(R.id.device_recycler)
        recyclerAdapter = RecyclerAdapter(arrayDevices)
        recyclerView!!.adapter = recyclerAdapter
        button.setOnClickListener {
            Intent(this,MainService::class.java).also {
//                startForegroundService(it)
//                var customBleConnector = BluetoothConnect.makeBleConnector(this@MainActivity)
//                customBleConnector.scanLeDevice(true)
//                recyclerView = findViewById(R.id.device_recycler)
//                val hashMap:HashMap<String,String> = HashMap()
//                val list = arrayListOf<HashMap<String,String>>()
//                list.add(hashMap)
//                hashMap["test"] = "testValue"
//                recyclerAdapter = RecyclerAdapter(list)
//                recyclerView!!.adapter = recyclerAdapter
//                recyclerAdapter!!.notifyDataSetChanged()
                scanLeDevice(true)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        //블루투스 미지원 기기 확인
        packageManager.takeIf {it.missingSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)}?.also {
            Toast.makeText(this, "블루투스가 지원되지 않는 기기입니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }


    private fun checkSelfPermission(){
        var rejectPermissionList = ArrayList<String>()
        for(permission in requiredPermission){
            if(ContextCompat.checkSelfPermission(this,permission) != PackageManager.PERMISSION_GRANTED){
                rejectPermissionList.add(permission)
            }
        }
        if(ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity,Manifest.permission.ACCESS_FINE_LOCATION)){
            Toast.makeText(this@MainActivity,"위치권한을 허가하지 않으면 앱을 실행시킬수 없습니다.",Toast.LENGTH_SHORT).show()
            finish()
        }else if(rejectPermissionList.isNotEmpty()){
            Log.d(TAG,"rejectedPermission List = "+rejectPermissionList.get(0))
            var array = arrayOfNulls<String>(rejectPermissionList.size)
            ActivityCompat.requestPermissions(this,rejectPermissionList.toArray(array),multiplePermissionCode)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode){
            multiplePermissionCode ->{
                if(grantResults.isNotEmpty()){
                    for((i,permission) in permissions.withIndex()){
                        ActivityCompat.requestPermissions(this,requiredPermission,singlePermissionCode)
                    }
                }
            }
        }
    }
}