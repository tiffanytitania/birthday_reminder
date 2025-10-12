package com.example.birthday_reminder

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.birthday_reminder.auth.UserManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import java.util.*

/**
 * Fragment untuk menampilkan dan mengedit profil pengguna
 * Tambahkan fragment ini ke MoreFragment atau buat menu baru
 */
class ProfileFragment : Fragment() {

    private lateinit var ivProfilePhoto: ImageView
    private lateinit var tvUsername: TextView
    private lateinit var tvRole: TextView
    private lateinit var tvFullName: TextView
    private lateinit var tvBirthDate: TextView
    private lateinit var tvPhone: TextView
    private lateinit var btnEditProfile: Button
    private lateinit var btnChangePhoto: ImageButton
    private lateinit var cardRoleInfo: MaterialCardView

    private var selectedImageUri: Uri? = null

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            loadProfilePhoto(it.toString())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        initViews(view)
        loadUserProfile()
        setupListeners()

        return view
    }

    private fun initViews(view: View) {
        ivProfilePhoto = view.findViewById(R.id.ivProfilePhoto)
        tvUsername = view.findViewById(R.id.tvUsername)
        tvRole = view.findViewById(R.id.tvRole)
        tvFullName = view.findViewById(R.id.tvFullName)
        tvBirthDate = view.findViewById(R.id.tvBirthDate)
        tvPhone = view.findViewById(R.id.tvPhone)
        btnEditProfile = view.findViewById(R.id.btnEditProfile)
        btnChangePhoto = view.findViewById(R.id.btnChangePhoto)
        cardRoleInfo = view.findViewById(R.id.cardRoleInfo)
    }

    private fun loadUserProfile() {
        val username = UserManager.getCurrentUser() ?: return
        val userData = UserManager.getUserData(username) ?: return

        // Set data ke UI
        tvUsername.text = "@$username"
        tvFullName.text = userData.fullName
        tvBirthDate.text = userData.birthDate ?: "Belum diatur"
        tvPhone.text = userData.phone ?: "Belum diatur"

        // Set role dengan styling
        val isAdmin = UserManager.isAdmin()
        tvRole.text = if (isAdmin) "👑 Admin" else "👤 Anggota"
        cardRoleInfo.setCardBackgroundColor(
            if (isAdmin)
                resources.getColor(R.color.accent, null)
            else
                resources.getColor(R.color.primary, null)
        )

        // Load foto profil
        userData.photoUrl?.let { loadProfilePhoto(it) }
    }

    private fun loadProfilePhoto(url: String) {
        Glide.with(this)
            .load(url)
            .placeholder(R.drawable.ic_person)
            .circleCrop()
            .into(ivProfilePhoto)
    }

    private fun setupListeners() {
        btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }

        btnChangePhoto.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }
    }

    private fun showEditProfileDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)

        val etFullName = dialogView.findViewById<TextInputEditText>(R.id.etFullName)
        val etBirthDate = dialogView.findViewById<TextInputEditText>(R.id.etBirthDate)
        val etPhone = dialogView.findViewById<TextInputEditText>(R.id.etPhone)

        // ✅ Ambil username aktif
        val username = UserManager.getCurrentUser() ?: return
        val userData = UserManager.getUserData(username) ?: return

        // Pre-fill data
        etFullName.setText(userData.fullName)
        etBirthDate.setText(userData.birthDate ?: "")
        etPhone.setText(userData.phone ?: "")

        // Date picker untuk tanggal lahir
        etBirthDate.setOnClickListener {
            showDatePicker { date ->
                etBirthDate.setText(date)
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("✏️ Edit Profil")
            .setView(dialogView)
            .setPositiveButton("Simpan") { dialog, _ ->
                val fullName = etFullName.text.toString().trim()
                val birthDate = etBirthDate.text.toString().trim()
                val phone = etPhone.text.toString().trim()

                if (fullName.isNotEmpty()) {
                    val success = UserManager.updateProfile(
                        fullName = fullName,
                        birthDate = birthDate.ifEmpty { null },
                        phone = phone.ifEmpty { null },
                        photoUrl = selectedImageUri?.toString()
                    )

                    if (success) {
                        Toast.makeText(requireContext(), "Profil berhasil diupdate!", Toast.LENGTH_SHORT).show()
                        loadUserProfile()
                    } else {
                        Toast.makeText(requireContext(), "Gagal update profil", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Nama lengkap tidak boleh kosong", Toast.LENGTH_SHORT).show()
                }

                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, y, m, d ->
            onDateSelected("$d/${m + 1}/$y")
        }, year, month, day).show()
    }
}