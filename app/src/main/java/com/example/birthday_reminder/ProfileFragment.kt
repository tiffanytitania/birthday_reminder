package com.example.birthday_reminder

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.birthday_reminder.auth.UserManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

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
    private var photoUri: Uri? = null

    // Gallery picker
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            loadProfilePhoto(it.toString())
        }
    }

    // Camera launcher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            selectedImageUri = photoUri
            loadProfilePhoto(photoUri.toString())
        } else {
            Toast.makeText(requireContext(), "Gagal mengambil foto", Toast.LENGTH_SHORT).show()
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

        tvUsername.text = "@$username"
        tvFullName.text = userData.fullName
        tvBirthDate.text = userData.birthDate ?: "Belum diatur"
        tvPhone.text = userData.phone ?: "Belum diatur"

        val isAdmin = UserManager.isAdmin()
        tvRole.text = if (isAdmin) "👑 Admin" else "👤 Anggota"
        cardRoleInfo.setCardBackgroundColor(
            if (isAdmin)
                resources.getColor(R.color.accent, null)
            else
                resources.getColor(R.color.primary, null)
        )

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
        btnEditProfile.setOnClickListener { showEditProfileDialog() }
        btnChangePhoto.setOnClickListener { showPhotoOptionsDialog() }
    }

    /** ---------------------- POPUP FOTO PROFIL ---------------------- **/
    private fun showPhotoOptionsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_photo_options, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val btnCamera = dialogView.findViewById<Button>(R.id.btnCamera)
        val btnGallery = dialogView.findViewById<Button>(R.id.btnGallery)
        val btnRemove = dialogView.findViewById<Button>(R.id.btnRemove)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btnClose)

        // Camera button (non-blocking)
        btnCamera.setOnClickListener {
            dialog.dismiss()
            lifecycleScope.launch(Dispatchers.IO) {
                val photoFile = File(requireContext().cacheDir, "profile_photo_${System.currentTimeMillis()}.jpg")
                photoFile.createNewFile()
                photoUri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    photoFile
                )
                withContext(Dispatchers.Main) {
                    cameraLauncher.launch(photoUri)
                }
            }
        }

        btnGallery.setOnClickListener {
            imagePickerLauncher.launch("image/*")
            dialog.dismiss()
        }

        btnRemove.setOnClickListener {
            ivProfilePhoto.setImageResource(R.drawable.ic_person)
            selectedImageUri = null
            Toast.makeText(requireContext(), "Foto profil dihapus", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    /** ---------------------- EDIT PROFIL ---------------------- **/
    private fun showEditProfileDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)

        val etFullName = dialogView.findViewById<TextInputEditText>(R.id.etFullName)
        val etBirthDate = dialogView.findViewById<TextInputEditText>(R.id.etBirthDate)
        val etPhone = dialogView.findViewById<TextInputEditText>(R.id.etPhone)

        val username = UserManager.getCurrentUser() ?: return
        val userData = UserManager.getUserData(username) ?: return

        etFullName.setText(userData.fullName)
        etBirthDate.setText(userData.birthDate ?: "")
        etPhone.setText(userData.phone ?: "")

        etBirthDate.setOnClickListener {
            showDatePicker { date -> etBirthDate.setText(date) }
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
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    /** ---------------------- PICKER TANGGAL ---------------------- **/
    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, y, m, d ->
            onDateSelected("$d/${m + 1}/$y")
        }, year, month, day).show()
    }

    private fun saveBitmapToCache(bitmap: android.graphics.Bitmap): Uri? {
        val cachePath = File(requireContext().cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "profile_photo.png")
        val stream = java.io.FileOutputStream(file)
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()
        return androidx.core.content.FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
    }
}
