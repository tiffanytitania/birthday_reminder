package com.example.birthday_reminder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.birthday_reminder.ui.viewmodel.BirthdayItem

/**
 * BirthdayListAdapter - Buttons optional (hanya jika ada di layout)
 * HomeFragment: item_home_birthday.xml (tanpa buttons)
 * AddBirthdayFragment: item_birthday.xml (dengan buttons)
 */
class BirthdayListAdapter(
    private val items: MutableList<BirthdayItem> = mutableListOf(),
    private val isAdmin: Boolean = false,
    private val onDeleteClick: ((BirthdayItem) -> Unit)? = null,
    private val onEditClick: ((BirthdayItem) -> Unit)? = null,
    private val layoutResId: Int = R.layout.item_home_birthday
) : RecyclerView.Adapter<BirthdayListAdapter.BirthdayViewHolder>() {

    inner class BirthdayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView? = itemView.findViewById(R.id.tvBirthdayName)
        val tvDate: TextView? = itemView.findViewById(R.id.tvBirthdayDate)
        // ✅ OPTIONAL - Cari button tapi jangan error jika tidak ada
        val btnDelete: ImageButton? = itemView.findViewById(R.id.btnDelete)
        val btnEdit: ImageButton? = itemView.findViewById(R.id.btnEdit)

        fun bind(item: BirthdayItem) {
            tvName?.text = item.name
            tvDate?.text = item.date

            // ✅ Hanya set listener jika button ada di layout (gunakan safe call ?.)
            if (isAdmin && onEditClick != null && onDeleteClick != null) {
                btnEdit?.visibility = View.VISIBLE
                btnDelete?.visibility = View.VISIBLE
                btnEdit?.setOnClickListener {
                    onEditClick.invoke(item)
                }
                btnDelete?.setOnClickListener {
                    onDeleteClick.invoke(item)
                }
            } else {
                btnEdit?.visibility = View.GONE
                btnDelete?.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BirthdayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutResId, parent, false)
        return BirthdayViewHolder(view)
    }

    override fun onBindViewHolder(holder: BirthdayViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<BirthdayItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}