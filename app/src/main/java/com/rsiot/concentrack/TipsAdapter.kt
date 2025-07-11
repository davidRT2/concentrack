package com.rsiot.concentrack // Sesuaikan dengan package Anda

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TipsAdapter(private val tipsList: List<Tip>) : RecyclerView.Adapter<TipsAdapter.TipViewHolder>() {

    class TipViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.iv_tip_icon)
        val title: TextView = itemView.findViewById(R.id.tv_tip_title)
        val description: TextView = itemView.findViewById(R.id.tv_tip_description)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TipViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tip, parent, false)
        return TipViewHolder(view)
    }

    override fun onBindViewHolder(holder: TipViewHolder, position: Int) {
        val tip = tipsList[position]
        holder.icon.setImageResource(tip.iconResId)
        holder.title.text = tip.title
        holder.description.text = tip.description
    }

    override fun getItemCount() = tipsList.size
}