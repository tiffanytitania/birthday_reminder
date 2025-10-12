package com.example.birthday_reminder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.birthday_reminder.auth.UserManager
import com.google.android.material.card.MaterialCardView
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

/**
 * Panel Admin untuk mengelola komunitas
 * Hanya bisa diakses oleh user dengan role ADMIN
 */
class AdminPanelFragment : Fragment() {

    private lateinit var database: DatabaseReference
    private lateinit var tvAdminWelcome: TextView
    private lateinit var cardCommunityInfo: MaterialCardView
    private lateinit var etCommunityName: EditText
    private lateinit var etCommunityDesc: EditText
    private lateinit var btnSaveCommunity: Button
    private lateinit var btnSendAnnouncement: Button
    private lateinit var btnUserManagement: Button
    private lateinit var btnUserSettings: Button // ganti dari btnExportData

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_admin_panel, container, false)

        // Cek apakah user adalah admin
        if (!UserManager.isAdmin()) {
            showAccessDenied()
            return view
        }

        initViews(view)
        setupDatabase()
        loadCommunityData()
        setupListeners()

        return view
    }

    private fun initViews(view: View) {
        tvAdminWelcome = view.findViewById(R.id.tvAdminWelcome)
        cardCommunityInfo = view.findViewById(R.id.cardCommunityInfo)
        etCommunityName = view.findViewById(R.id.etCommunityName)
        etCommunityDesc = view.findViewById(R.id.etCommunityDesc)
        btnSaveCommunity = view.findViewById(R.id.btnSaveCommunity)
        btnSendAnnouncement = view.findViewById(R.id.btnSendAnnouncement)
        btnUserManagement = view.findViewById(R.id.btnUserManagement)
        btnUserSettings = view.findViewById(R.id.btnUserSettings)

        tvAdminWelcome.text = "Selamat datang, Admin ${UserManager.getCurrentUser()}! 👑"
    }

    private fun setupDatabase() {
        database = FirebaseDatabase.getInstance(
            "https://birthday-reminder-f26d8-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).reference
    }

    private fun loadCommunityData() {
        database.child("community_info").get()
            .addOnSuccessListener { snapshot ->
                val name = snapshot.child("name").getValue(String::class.java) ?: "Komunitas Saya"
                val desc = snapshot.child("description").getValue(String::class.java) ?: ""

                etCommunityName.setText(name)
                etCommunityDesc.setText(desc)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Gagal memuat data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupListeners() {
        btnSaveCommunity.setOnClickListener { saveCommunityInfo() }

        btnSendAnnouncement.setOnClickListener { showAnnouncementDialog() }

        btnUserManagement.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_container, UserManagementFragment())
                .addToBackStack(null)
                .commit()
        }

        btnUserSettings.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_container, ExportDataFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun saveCommunityInfo() {
        val name = etCommunityName.text.toString().trim()
        val desc = etCommunityDesc.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Nama komunitas tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        val communityData = mapOf(
            "name" to name,
            "description" to desc,
            "updatedBy" to UserManager.getCurrentUser(),
            "updatedAt" to System.currentTimeMillis()
        )

        database.child("community_info").setValue(communityData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "✅ Data komunitas berhasil disimpan", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "❌ Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAnnouncementDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_announcement, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.etAnnouncementTitle)
        val etMessage = dialogView.findViewById<EditText>(R.id.etAnnouncementMessage)

        AlertDialog.Builder(requireContext())
            .setTitle("📢 Kirim Pengumuman Massal")
            .setView(dialogView)
            .setPositiveButton("Kirim") { dialog, _ ->
                val title = etTitle.text.toString().trim()
                val message = etMessage.text.toString().trim()

                if (title.isNotEmpty() && message.isNotEmpty()) {
                    sendAnnouncement(title, message)
                } else {
                    Toast.makeText(requireContext(), "Judul dan pesan tidak boleh kosong", Toast.LENGTH_SHORT).show()
                }

                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun sendAnnouncement(title: String, message: String) {
        val announcement = mapOf(
            "title" to title,
            "message" to message,
            "sender" to UserManager.getCurrentUser(),
            "timestamp" to System.currentTimeMillis(),
            "type" to "announcement"
        )

        database.child("announcements").push().setValue(announcement)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "✅ Pengumuman berhasil dikirim!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "❌ Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAccessDenied() {
        Toast.makeText(requireContext(), "⛔ Akses ditolak! Hanya admin yang dapat mengakses halaman ini.", Toast.LENGTH_LONG).show()
        parentFragmentManager.popBackStack()
    }
}
