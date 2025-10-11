package com.example.birthday_reminder.ui.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.birthday_reminder.R
import com.example.birthday_reminder.data.model.Message
import com.example.birthday_reminder.data.model.MessageType

class MessageAdapter(
    private var messages: List<Message>,
    private val onMessageClick: (Message) -> Unit,
    private val onDeleteClick: (Message) -> Unit
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFrom: TextView = view.findViewById(R.id.tvMessageFrom)
        val tvMessage: TextView = view.findViewById(R.id.tvMessageContent)
        val tvTime: TextView = view.findViewById(R.id.tvMessageTime)
        val tvTypeIcon: TextView = view.findViewById(R.id.tvMessageTypeIcon)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteMessage)
        val unreadIndicator: View = view.findViewById(R.id.unreadIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]

        holder.tvFrom.text = "Dari: ${message.from}"
        holder.tvMessage.text = message.message
        holder.tvTime.text = message.getFormattedTime()

        // Icon berdasarkan tipe pesan
        holder.tvTypeIcon.text = when (message.type) {
            MessageType.GREETING -> "🎉"
            MessageType.PERSONAL -> "💬"
            MessageType.ANNOUNCEMENT -> "📢"
        }

        // Tampilkan indicator jika belum dibaca
        if (!message.isRead) {
            holder.unreadIndicator.visibility = View.VISIBLE
            holder.tvFrom.setTypeface(null, Typeface.BOLD)
            holder.tvMessage.setTypeface(null, Typeface.BOLD)
        } else {
            holder.unreadIndicator.visibility = View.GONE
            holder.tvFrom.setTypeface(null, Typeface.NORMAL)
            holder.tvMessage.setTypeface(null, Typeface.NORMAL)
        }

        // Click listener
        holder.itemView.setOnClickListener {
            onMessageClick(message)
        }

        // Delete button
        holder.btnDelete.setOnClickListener {
            onDeleteClick(message)
        }
    }

    override fun getItemCount() = messages.size

    fun updateMessages(newMessages: List<Message>) {
        messages = newMessages
        notifyDataSetChanged()
    }
}