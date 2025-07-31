package com.example.chatx

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val messages: List<Message>,
    private val currentUserId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val MSG_SENT = 1
        private const val MSG_RECEIVED = 2
    }
    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) MSG_SENT else MSG_RECEIVED
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == MSG_SENT) {
            val view = inflater.inflate(R.layout.item_message_sent, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_message_received, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(msg.timestamp)

        if (holder is SentMessageViewHolder) {
            holder.msgText.text = msg.message
            holder.msgTime.text = time

            // Show ticks
            if (msg.seen) {
                holder.tickIcon.setImageResource(R.drawable.ic_double_tick)
            } else {
                holder.tickIcon.setImageResource(R.drawable.ic_single_tick)
            }

        } else if (holder is ReceivedMessageViewHolder) {
            holder.msgText.text = msg.message
            holder.msgTime.text = time
        }
    }

    override fun getItemCount(): Int = messages.size

    inner class SentMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val msgText: TextView = view.findViewById(R.id.tvSentMessage)
        val msgTime: TextView = view.findViewById(R.id.tvSentTime)
        val tickIcon: ImageView = view.findViewById(R.id.ivTick)
    }

    inner class ReceivedMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val msgText: TextView = view.findViewById(R.id.tvReceivedMessage)
        val msgTime: TextView = view.findViewById(R.id.tvReceivedTime)
    }
}
