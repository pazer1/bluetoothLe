package com.example.bb_temperature

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecyclerAdapter(var items: List<BluetoothDevice>?)
    : RecyclerView.Adapter<RecyclerAdapter.ViewHolder>(){

    private val TAG = "RecyclerAdapter"
    lateinit var mContext: Context
    private var clickItem:ClickItem? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            : ViewHolder {
        val v: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.ble_list_item,parent,false)
        mContext = parent.context
        Log.d(TAG,"onCreateViewHolder")
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d(TAG,"onBindViewHolder")
        holder.bind(items!!.get(position),position)
        Log.d(TAG,"onBindViewHolder item =  ${items} position = ${position}")
    }

    override fun getItemCount(): Int {
        return items!!.size
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        var index: Int? = null
        fun bind(item:BluetoothDevice, position: Int) {
            index = position
            var nameTextView:TextView = itemView.findViewById(R.id.device_name)
            var textView:TextView = itemView.findViewById(R.id.device_address)
            var linearLayout:LinearLayout = nameTextView.parent as LinearLayout
            nameTextView.text = item.name?:"noName"
            textView.text= item.address
            if(nameTextView.text.contains("RN")){
                nameTextView.textSize = 30.toFloat()
                nameTextView.setTextColor(Color.RED)
            }else{
                nameTextView.textSize = 15.toFloat()
                nameTextView.setTextColor(Color.BLACK)
            }
            linearLayout.setOnClickListener {
                Log.d(TAG,"버튼 클릭됨!!!")
                clickItem?.onClick()
                var intent = Intent(mContext,DeviceControlActivity::class.java)
                intent.putExtra("deviceAddress",item.address)
                intent.putExtra("deviceName",item.name?:"noName")
                intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                mContext.startActivity(intent)
            }
            Log.d(TAG,"text =${textView.text}")
        }
    }

    public fun setClickItem(click: () -> Unit){
        this.clickItem = clickItem
    }

    public interface ClickItem{
        fun onClick()
    }
}