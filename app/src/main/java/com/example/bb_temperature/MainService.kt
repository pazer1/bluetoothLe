package com.example.bb_temperature

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat.startActivityForResult

class MainService : Service() {

    private val CHANNEL_ID = "foregroundService"

    private val TAG = javaClass.simpleName
    private var serviceLooper: Looper? = null
    private var serviceHandler: ServiceHandler? = null

    //bluetooth 연결 코드
    private val BluetoothAdapter.isDisabled: Boolean get()=!isEnabled
    private val REQUEST_ENABLE_BT = 1000

    private inner class ServiceHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            try {
                bluetoothAdapter?.takeIf {it.isDisabled}?.apply {

                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
            stopSelf(msg.arg1)
        }
    }


    override fun onCreate() {
        HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND).apply {
            start()
            serviceLooper = looper
            serviceHandler = ServiceHandler(looper)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let{
            Log.d(TAG,it!!.action?:"onStartCommand Intent Action = noAction")
        }
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
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
        startForeground(1, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        Toast.makeText(this, "service done.", Toast.LENGTH_SHORT).show()
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE){
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
}