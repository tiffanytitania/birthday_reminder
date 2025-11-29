package com.example.birthday_reminder

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.birthday_reminder.auth.UserManager
import com.google.android.material.card.MaterialCardView
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class AdminPanelFragment : Fragment() {

    private lateinit var database: DatabaseReference
    private var selectedImageUri: Uri? = null

    private lateinit var tvAdminWelcome: TextView
    private lateinit var etCommunityName: EditText
    private lateinit var etCommunityDesc: EditText
    private lateinit var btnSaveCommunity: Button
    private lateinit var btnSendAnnouncement: Button
    private lateinit var btnUserManagement: Button
    private lateinit var btnUserSettings: Button
    private lateinit var imgCommunityBanner: ImageView
    private lateinit var btnUploadImage: Button

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleLocalImage(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_admin_panel, container, false)

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
        etCommunityName = view.findViewById(R.id.etCommunityName)
        etCommunityDesc = view.findViewById(R.id.etCommunityDesc)
        btnSaveCommunity = view.findViewById(R.id.btnSaveCommunity)
        btnSendAnnouncement = view.findViewById(R.id.btnSendAnnouncement)
        btnUserManagement = view.findViewById(R.id.btnUserManagement)
        btnUserSettings = view.findViewById(R.id.btnUserSettings)
        imgCommunityBanner = view.findViewById(R.id.imgCommunityBanner)
        btnUploadImage = view.findViewById(R.id.btnUploadImage)

        val username = UserManager.getCurrentUser() ?: "Admin"
        tvAdminWelcome.text = "Selamat datang, $username 👑"
    }

    private fun setupDatabase() {
        database = FirebaseDatabase.getInstance(
            "https://birthday-reminder-fa6fb-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).reference
    }

    private fun loadCommunityData() {
        database.child("community_info").get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener

                val name = snapshot.child("name").getValue(String::class.java) ?: "Komunitas Saya"
                val desc = snapshot.child("description").getValue(String::class.java) ?: ""

                etCommunityName.setText(name)
                etCommunityDesc.setText(desc)

                val prefs = requireContext().getSharedPreferences("community_prefs", Activity.MODE_PRIVATE)
                val localUri = prefs.getString("localBannerUri", null)

                if (localUri != null) {
                    try {
                        Glide.with(requireContext())
                            .load(Uri.parse(localUri))
                            .placeholder(R.drawable.banner_placeholder)
                            .error(R.drawable.banner_placeholder)
                            .into(imgCommunityBanner)
                    } catch (e: Exception) {
                        imgCommunityBanner.setImageResource(R.drawable.banner_placeholder)
                    }
                } else {
                    imgCommunityBanner.setImageResource(R.drawable.banner_placeholder)
                }
            }
            .addOnFailureListener { e ->
                if (isAdded)
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
                .commitAllowingStateLoss()
        }

        btnUserSettings.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_container, ExportDataFragment())
                .addToBackStack(null)
                .commitAllowingStateLoss()
        }

        btnUploadImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    private fun handleLocalImage(uri: Uri) {
        selectedImageUri = uri
        try {
            Glide.with(requireContext())
                .load(uri)
                .placeholder(R.drawable.banner_placeholder)
                .error(R.drawable.banner_placeholder)
                .into(imgCommunityBanner)
        } catch (e: Exception) {
            imgCommunityBanner.setImageResource(R.drawable.banner_placeholder)
        }

        val prefs = requireContext().getSharedPreferences("community_prefs", Activity.MODE_PRIVATE)
        prefs.edit().putString("localBannerUri", uri.toString()).apply()
        Toast.makeText(requireContext(), "✅ Gambar komunitas diset secara lokal", Toast.LENGTH_SHORT).show()
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
            "updatedBy" to (UserManager.getCurrentUser() ?: "Unknown"),
            "updatedAt" to System.currentTimeMillis()
        )

        database.child("community_info").updateChildren(communityData)
            .addOnSuccessListener {
                if (isAdded)
                    Toast.makeText(requireContext(), "✅ Data komunitas berhasil disimpan", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                if (isAdded)
                    Toast.makeText(requireContext(), "❌ Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAnnouncementDialog() {
        if (!isAdded) return
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
            "sender" to (UserManager.getCurrentUser() ?: "Admin"),
            "timestamp" to System.currentTimeMillis(),
            "type" to "announcement"
        )

        database.child("announcements").push().setValue(announcement)
            .addOnSuccessListener {
                if (isAdded)
                    Toast.makeText(requireContext(), "✅ Pengumuman berhasil dikirim!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                if (isAdded)
                    Toast.makeText(requireContext(), "❌ Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAccessDenied() {
        if (!isAdded) return
        Toast.makeText(requireContext(), "⛔ Akses ditolak! Hanya admin yang dapat mengakses halaman ini.", Toast.LENGTH_LONG).show()
        parentFragmentManager.popBackStack()
    }
}
