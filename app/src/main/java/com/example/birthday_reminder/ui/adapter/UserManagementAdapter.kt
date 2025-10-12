package com.example.birthday_reminder.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.birthday_reminder.R
import com.example.birthday_reminder.data.model.Member

class UserManagementAdapter(
    private val users: List<Member>,
    private val onEditRoleClick: (Member) -> Unit,
    private val onDeleteClick: (Member) -> Unit
) : RecyclerView.Adapter<UserManagementAdapter.UserViewHolder>() {

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvUserName)
        val tvRole: TextView = view.findViewById(R.id.tvUserRole)
        val tvEmail: TextView = view.findViewById(R.id.tvUserEmail)
        val btnEditRole: ImageButton = view.findViewById(R.id.btnEditRole)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteUser)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_management, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]

        holder.tvName.text = user.name

        if (user.role == "admin") {
            holder.tvRole.text = "👑 Administrator"
            holder.tvRole.setBackgroundResource(R.drawable.badge_admin)
        } else {
            holder.tvRole.text = "👤 Member"
            holder.tvRole.setBackgroundResource(R.drawable.badge_member)
        }

        holder.tvEmail.text = if (user.email.isNotEmpty()) {
            user.email
        } else {
            "Email tidak tersedia"
        }

        // Click listeners
        holder.btnEditRole.setOnClickListener {
            onEditRoleClick(user)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(user)
        }
    }

    override fun getItemCount() = users.size
}