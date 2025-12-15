package com.example.birthday_reminder.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.birthday_reminder.R
import com.example.birthday_reminder.data.model.Member

class MemberAdapter(
    private val members: List<Member>,
    private val onMemberClick: (Member) -> Unit,
    private val onWhatsAppClick: (Member) -> Unit,
    private val onCallClick: (Member) -> Unit,
    private val onSmsClick: (Member) -> Unit
) : RecyclerView.Adapter<MemberAdapter.MemberViewHolder>() {

    class MemberViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPhoto: ImageView = view.findViewById(R.id.ivMemberPhoto)
        val tvName: TextView = view.findViewById(R.id.tvMemberName)
        val tvPhone: TextView = view.findViewById(R.id.tvMemberPhone)
        val tvBirthDate: TextView = view.findViewById(R.id.tvMemberBirthDate)
        val btnWhatsApp: ImageButton = view.findViewById(R.id.btnWhatsApp)
        val btnCall: ImageButton = view.findViewById(R.id.btnCall)
        val btnSms: ImageButton = view.findViewById(R.id.btnSms)
        val tvRoleBadge: TextView = view.findViewById(R.id.tvRoleBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = members[position]

        // Load photo
        if (member.profilePhotoUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(member.profilePhotoUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_person)
                .into(holder.ivPhoto)
        } else {
            holder.ivPhoto.setImageResource(R.drawable.ic_person)
        }

        // Basic info
        holder.tvName.text = member.name

        val phoneText = if (member.phone.isNotEmpty()) {
            member.phone
        } else {
            "Nomor tidak tersedia"
        }
        holder.tvPhone.text = phoneText

        val birthDateText = if (member.birthDate.isNotEmpty()) {
            val age = member.getAge()
            if (age != null) {
                "${member.getFormattedBirthDate()} ($age tahun)"
            } else {
                member.getFormattedBirthDate()
            }
        } else {
            "Tanggal lahir tidak tersedia"
        }
        holder.tvBirthDate.text = birthDateText

        // Role badge
        if (member.role == "admin") {
            holder.tvRoleBadge.visibility = View.VISIBLE
            holder.tvRoleBadge.text = "👑 Admin"
        } else {
            holder.tvRoleBadge.visibility = View.GONE
        }

        val hasPhone = member.phone.isNotEmpty()
        holder.btnWhatsApp.isEnabled = hasPhone
        holder.btnCall.isEnabled = hasPhone
        holder.btnSms.isEnabled = hasPhone

        holder.btnWhatsApp.alpha = if (hasPhone) 1.0f else 0.3f
        holder.btnCall.alpha = if (hasPhone) 1.0f else 0.3f
        holder.btnSms.alpha = if (hasPhone) 1.0f else 0.3f

        // Click listeners
        holder.itemView.setOnClickListener {
            onMemberClick(member)
        }

        holder.btnWhatsApp.setOnClickListener {
            if (hasPhone) onWhatsAppClick(member)
        }

        holder.btnCall.setOnClickListener {
            if (hasPhone) onCallClick(member)
        }

        holder.btnSms.setOnClickListener {
            if (hasPhone) onSmsClick(member)
        }
    }

    override fun getItemCount() = members.size
}