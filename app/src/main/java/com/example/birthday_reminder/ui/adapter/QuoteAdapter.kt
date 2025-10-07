package com.example.birthday_reminder.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.birthday_reminder.R
import com.example.birthday_reminder.data.model.Quote

class QuoteAdapter(
    private val quotes: List<Quote>,
    private val onSendClick: (Quote) -> Unit
) : RecyclerView.Adapter<QuoteAdapter.QuoteViewHolder>() {

    class QuoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvText: TextView = view.findViewById(R.id.tvQuoteText)
        val tvAuthor: TextView = view.findViewById(R.id.tvAuthor)
        val ivImage: ImageView = view.findViewById(R.id.ivImage)
        val btnSend: Button = view.findViewById(R.id.btnSend)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quote, parent, false)
        return QuoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuoteViewHolder, position: Int) {
        val quote = quotes[position]
        holder.tvText.text = quote.text ?: "Tanpa isi"
        holder.tvAuthor.text = quote.author ?: "Anonim"

        if (!quote.image.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(quote.image)
                .into(holder.ivImage)
            holder.ivImage.visibility = View.VISIBLE
        } else {
            // fallback ke gambar acak
            val randomImageUrl = "https://picsum.photos/400?random=$position"
            Glide.with(holder.itemView.context)
                .load(randomImageUrl)
                .into(holder.ivImage)
            holder.ivImage.visibility = View.VISIBLE
        }

        holder.btnSend.setOnClickListener {
            onSendClick(quote)
        }
    }


    override fun getItemCount() = quotes.size
}
