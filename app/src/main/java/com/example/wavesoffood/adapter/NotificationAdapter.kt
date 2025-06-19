package com.example.wavesoffood.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.wavesoffood.databinding.NotificationitemBinding

class NotificationAdapter(
    private val notificationList: List<String>,
    private val notificationImages: List<Int>
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = NotificationitemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = notificationList.size

    inner class NotificationViewHolder(private val binding: NotificationitemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int) {
            binding.notificationtextview.text = notificationList[position]
            binding.notificationinmage.setImageResource(notificationImages[position])
        }
    }
}
