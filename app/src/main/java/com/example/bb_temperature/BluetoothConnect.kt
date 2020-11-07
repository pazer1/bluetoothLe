package com.example.bb_temperature

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService

class BluetoothConnect (context:Context){

    companion object BleFactory{
        fun makeBleConnector(context: Context):BluetoothConnect{
            return BluetoothConnect(context)
        }
    }

    private val TAG = BluetoothConnect.javaClass.simpleName
    private var mScanning:Boolean = false
    private var arrayDevices = ArrayList<BluetoothDevice>()
    private val handler = Handler()

    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE){
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val scanCallback = object:ScanCallback(){
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Toast.makeText(context, "블루투스 스캔을 실패했습니다.", Toast.LENGTH_SHORT).show()
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let {
                if(!arrayDevices.contains(it.device)){
                    Log.d(TAG,it.device.name?:"NONAME")
                    arrayDevices.add(it.device)
                    Log.d(TAG,arrayDevices.toString())
                }
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.let{
                for(result in it){
                    if(!arrayDevices.contains(result.device)) {
                        Log.d(TAG,"onBatchResult deviceName= ${result.device.name?:"Noname"}")
                        arrayDevices.add(result.device)
                        Log.d(TAG,result.toString())
                    }
                }
            }
        }
    }

    private val SCAN_PERIOD = 10000

    public fun scanLeDevice(enable: Boolean){
        when(enable){
            true->{
                handler.postDelayed({
                    mScanning = false
                    bluetoothAdapter!!.bluetoothLeScanner.stopScan(scanCallback)
                }, SCAN_PERIOD.toLong())
                mScanning = true
                arrayDevices.clear()
                bluetoothAdapter!!.bluetoothLeScanner.startScan(scanCallback)
            }
            else->{
                mScanning = false
                bluetoothAdapter!!.bluetoothLeScanner.stopScan(scanCallback)
            }
        }
    }
}