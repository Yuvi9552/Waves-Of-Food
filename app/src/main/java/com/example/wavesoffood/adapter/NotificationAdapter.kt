package com.example.wavesoffood.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.wavesoffood.R
import com.example.wavesoffood.databinding.NotificationitemBinding
import com.example.wavesoffood.model.AppNotification
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private val notificationList: MutableList<AppNotification>
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    // Deletes item at position and returns it (used for swipe-to-delete)
    fun deleteItem(position: Int): AppNotification? {
        return if (position in notificationList.indices) {
            val item = notificationList.removeAt(position)
            notifyItemRemoved(position)
            item
        } else null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = NotificationitemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notificationList[position])
    }

    override fun getItemCount(): Int = notificationList.size

    inner class NotificationViewHolder(private val binding: NotificationitemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: AppNotification) = with(binding) {
            notificationtextview.text = notification.title.orEmpty()
            notificationdescription.text = notification.message.orEmpty()
            notificationtime.text = formatTimestamp(notification.timestamp)

            // ✅ Fix: Avoid misuse of `it` by using a local variable
            val titleLower = notification.title?.lowercase(Locale.getDefault()).orEmpty()
            val imageRes = when {
                "cancel" in titleLower -> R.drawable.sademoji
                "placed" in titleLower -> R.drawable.group_805
                "accepted" in titleLower -> R.drawable.shopping_bag
                "dispatched" in titleLower -> R.drawable.car
                else -> R.drawable.group_805
            }

            notificationinmage.setImageResource(imageRes)
        }

        private fun formatTimestamp(timestamp: String?): String {
            val timeMillis = timestamp?.toLongOrNull() ?: return ""
            return try {
                val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                sdf.format(Date(timeMillis))
            } catch (e: Exception) {
                ""
            }
        }
    }
}
