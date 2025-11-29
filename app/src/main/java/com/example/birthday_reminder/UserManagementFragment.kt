package com.example.birthday_reminder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.birthday_reminder.auth.UserManager
import com.example.birthday_reminder.data.model.Member
import com.example.birthday_reminder.databinding.FragmentUserManagementBinding
import com.example.birthday_reminder.ui.adapter.UserManagementAdapter
import com.google.firebase.database.*

class UserManagementFragment : Fragment() {
    private var _binding: FragmentUserManagementBinding? = null
    private val binding get() = _binding!!

    private lateinit var userAdapter: UserManagementAdapter
    private val allUsers = mutableListOf<Member>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Cek apakah user adalah admin
        if (!UserManager.isAdmin()) {
            Toast.makeText(requireContext(), "❌ Akses ditolak! Hanya admin yang bisa mengakses.", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        setupRecyclerView()
        loadUsers()
    }

    private fun setupRecyclerView() {
        userAdapter = UserManagementAdapter(
            users = allUsers,
            onEditRoleClick = { user -> showEditRoleDialog(user) },
            onDeleteClick = { user -> showDeleteConfirmDialog(user) }
        )

        binding.rvUsers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userAdapter
        }
    }

    private fun loadUsers() {
        binding.progressBar.visibility = View.VISIBLE

        val database = FirebaseDatabase.getInstance(
            "https://birthday-reminder-fa6fb-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).reference

        database.child("birthdays").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allUsers.clear()

                for (data in snapshot.children) {
                    val name = data.child("name").getValue(String::class.java) ?: continue
                    val date = data.child("date").getValue(String::class.java) ?: ""
                    val phone = data.child("phone").getValue(String::class.java) ?: ""
                    val email = data.child("email").getValue(String::class.java) ?: ""
                    val role = data.child("role").getValue(String::class.java) ?: "member"

                    val user = Member(
                        id = data.key ?: "",
                        name = name,
                        phone = phone,
                        email = email,
                        birthDate = date,
                        role = role
                    )
                    allUsers.add(user)
                }

                allUsers.sortWith(compareBy({ it.role != "admin" }, { it.name }))
                userAdapter.notifyDataSetChanged()
                updateUI()
                binding.progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Gagal memuat data: ${error.message}", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
            }
        })
    }

    private fun updateUI() {
        val adminCount = allUsers.count { it.role == "admin" }
        val memberCount = allUsers.count { it.role == "member" }

        binding.tvTotalUsers.text = "${allUsers.size} Total"
        binding.tvAdminCount.text = "$adminCount Admin"
        binding.tvMemberCount.text = "$memberCount Anggota"

        if (allUsers.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.rvUsers.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.rvUsers.visibility = View.VISIBLE
        }
    }

    private fun showEditRoleDialog(user: Member) {
        val currentUser = UserManager.getCurrentUser()

        if (user.name == currentUser && user.role == "admin") {
            Toast.makeText(requireContext(), "⚠️ Anda tidak bisa mengubah role diri sendiri!", Toast.LENGTH_SHORT).show()
            return
        }

        val roles = arrayOf("👤 Member", "👑 Admin")
        val currentIndex = if (user.role == "admin") 1 else 0

        AlertDialog.Builder(requireContext())
            .setTitle("Ubah Role: ${user.name}")
            .setSingleChoiceItems(roles, currentIndex) { dialog, which ->
                val newRole = if (which == 1) "admin" else "member"
                updateUserRole(user, newRole)
                dialog.dismiss()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun updateUserRole(user: Member, newRole: String) {
        val database = FirebaseDatabase.getInstance(
            "https://birthday-reminder-f26d8-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).reference

        database.child("birthdays").child(user.id).child("role").setValue(newRole)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "✅ ${user.name} sekarang menjadi ${if (newRole == "admin") "Admin" else "Member"}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "❌ Gagal update role: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteConfirmDialog(user: Member) {
        val currentUser = UserManager.getCurrentUser()

        if (user.name == currentUser) {
            Toast.makeText(requireContext(), "⚠️ Anda tidak bisa menghapus diri sendiri!", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Hapus User")
            .setMessage("Apakah Anda yakin ingin menghapus ${user.name}?\n\nData ulang tahun dan semua informasi akan dihapus permanen.")
            .setPositiveButton("Hapus") { _, _ -> deleteUser(user) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteUser(user: Member) {
        val database = FirebaseDatabase.getInstance(
            "https://birthday-reminder-f26d8-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).reference

        database.child("birthdays").child(user.id).removeValue()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "✅ ${user.name} berhasil dihapus", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "❌ Gagal menghapus: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
