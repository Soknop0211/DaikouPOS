package com.eazy.daikoupos

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class BluetoothItemAdapter(private val bluetoothAddress : String, private val list : List<String>, private val onClickListener : OnClickCallBackLister) : RecyclerView.Adapter<BluetoothItemAdapter.ViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.item_connection_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item : String = list[position]
        holder.itemNameTv.text = item
        holder.line.visibility = if((list.size - 1) == position) View.GONE else View.VISIBLE
        holder.mainLayout.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(
                holder.mainLayout.context,
                if (bluetoothAddress.equals(item, true)) R.color.light_grey else R.color.white
            )
        )

        holder.itemView.setOnClickListener { onClickListener.onClickCallBack(item) }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemNameTv : TextView = itemView.findViewById(R.id.item_name)
        val line : View = itemView.findViewById(R.id.line)
        val mainLayout : View = itemView.findViewById(R.id.mainLayout)
    }

    interface OnClickCallBackLister{
        fun onClickCallBack(bluetooth : String?)
    }
}