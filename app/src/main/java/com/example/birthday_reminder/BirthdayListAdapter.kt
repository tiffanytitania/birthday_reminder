package com.example.birthday_reminder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.birthday_reminder.R
import com.example.birthday_reminder.data.model.Birthday


class BirthdayListAdapter : RecyclerView.Adapter<BirthdayListAdapter.BirthdayViewHolder>() {

    private var items: List<Birthday> = emptyList()

    fun setBirthdays(list: List<Birthday>) {
        items = list
        notifyDataSetChanged()
    }

    inner class BirthdayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvBirthdayName)
        val tvDate: TextView = itemView.findViewById(R.id.tvBirthdayDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BirthdayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_home_birthday, parent, false)
        return BirthdayViewHolder(view)
    }

    override fun onBindViewHolder(holder: BirthdayViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvDate.text = item.date
    }

    override fun getItemCount(): Int = items.size
}