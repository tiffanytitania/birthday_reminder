package com.example.birthday_reminder

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.birthday_reminder.auth.UserManager
import com.example.birthday_reminder.utils.ImageKitConfig
import com.example.birthday_reminder.utils.ImageKitManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.FirebaseDatabase
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
    private lateinit var progressBar: ProgressBar

    private var selectedImageUri: Uri? = null
    private var photoUri: Uri? = null
    private var isUploading = false

    private val database = FirebaseDatabase.getInstance(
        "https://birthday-reminder-fa6fb-default-rtdb.asia-southeast1.firebasedatabase.app/"
    ).reference

    // Request permissions
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            openCamera()
        } else {
            Toast.makeText(
                requireContext(),
                "Izin kamera diperlukan untuk mengambil foto",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Gallery picker
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            uploadImageToImageKit(it)
        }
    }

    // Camera launcher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            selectedImageUri = photoUri
            uploadImageToImageKit(photoUri!!)
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
        progressBar = view.findViewById(R.id.progressBar)
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

        // Load profile photo dari ImageKit
        userData.photoUrl?.let {
            if (it.isNotEmpty()) {
                loadProfilePhoto(it)
            }
        }
    }

    private fun loadProfilePhoto(url: String) {
        if (url.isEmpty()) {
            ivProfilePhoto.setImageResource(R.drawable.ic_person)
            return
        }

        // Use ImageKit transformation untuk optimize
        val optimizedUrl = ImageKitConfig.getTransformedUrl(
            imageUrl = url,
            width = 300,
            height = 300,
            quality = 80
        )

        android.util.Log.d("ProfileFragment", "Loading image: $optimizedUrl")

        Glide.with(this)
            .load(optimizedUrl)
            .placeholder(R.drawable.ic_person)
            .error(R.drawable.ic_person)
            .circleCrop()
            .into(ivProfilePhoto)
    }

    private fun setupListeners() {
        btnEditProfile.setOnClickListener { showEditProfileDialog() }
        btnChangePhoto.setOnClickListener { showPhotoOptionsDialog() }
    }

    private fun showPhotoOptionsDialog() {
        if (!isAdded || context == null) return

        val dialogView = layoutInflater.inflate(R.layout.dialog_photo_options, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val btnCamera = dialogView.findViewById<Button>(R.id.btnCamera)
        val btnGallery = dialogView.findViewById<Button>(R.id.btnGallery)
        val btnRemove = dialogView.findViewById<Button>(R.id.btnRemove)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btnClose)

        btnCamera.setOnClickListener {
            dialog.dismiss()
            checkAndRequestCameraPermission()
        }

        btnGallery.setOnClickListener {
            imagePickerLauncher.launch("image/*")
            dialog.dismiss()
        }

        btnRemove.setOnClickListener {
            ivProfilePhoto.setImageResource(R.drawable.ic_person)
            selectedImageUri = null

            val username = UserManager.getCurrentUser() ?: return@setOnClickListener
            val userData = UserManager.getUserData(username) ?: return@setOnClickListener

            UserManager.updateProfile(
                fullName = userData.fullName,
                birthDate = userData.birthDate,
                phone = userData.phone,
                photoUrl = null
            )

            // Sync ke Firebase
            syncPhotoUrlToFirebase(null)

            Toast.makeText(requireContext(), "Foto profil dihapus", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    /** =============== UPLOAD KE IMAGEKIT =============== **/
    private fun uploadImageToImageKit(uri: Uri) {
        if (isUploading) {
            Toast.makeText(requireContext(), "Upload sedang berjalan...", Toast.LENGTH_SHORT).show()
            return
        }

        isUploading = true
        progressBar.visibility = View.VISIBLE
        btnChangePhoto.isEnabled = false

        android.util.Log.d("ProfileFragment", "🔄 Starting upload process...")

        lifecycleScope.launch {
            try {
                val username = UserManager.getCurrentUser() ?: "unknown"
                val fileName = "profile_${username}_${System.currentTimeMillis()}.jpg"

                android.util.Log.d("ProfileFragment", "📤 Uploading as: $fileName")

                val imageUrl = ImageKitManager.uploadImage(
                    context = requireContext(),
                    imageUri = uri,
                    folder = ImageKitConfig.PROFILE_FOLDER,
                    fileName = fileName
                )

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnChangePhoto.isEnabled = true
                    isUploading = false

                    if (imageUrl != null) {
                        android.util.Log.d("ProfileFragment", "✅ Upload success: $imageUrl")
                        Toast.makeText(requireContext(), "✅ Foto berhasil diupload!", Toast.LENGTH_SHORT).show()

                        // Load image ke UI
                        loadProfilePhoto(imageUrl)

                        // Save URL ke UserManager (local)
                        saveProfilePhotoUrl(imageUrl)

                        // Sync ke Firebase
                        syncPhotoUrlToFirebase(imageUrl)

                    } else {
                        android.util.Log.e("ProfileFragment", "❌ Upload failed: imageUrl is null")
                        Toast.makeText(requireContext(), "❌ Gagal upload foto. Cek log untuk detail.", Toast.LENGTH_LONG).show()
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("ProfileFragment", "❌ Upload exception", e)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnChangePhoto.isEnabled = true
                    isUploading = false
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveProfilePhotoUrl(imageUrl: String) {
        val username = UserManager.getCurrentUser() ?: return
        val userData = UserManager.getUserData(username) ?: return

        UserManager.updateProfile(
            fullName = userData.fullName,
            birthDate = userData.birthDate,
            phone = userData.phone,
            photoUrl = imageUrl
        )

        android.util.Log.d("ProfileFragment", "✅ Photo URL saved locally")
    }

    private fun syncPhotoUrlToFirebase(imageUrl: String?) {
        val username = UserManager.getCurrentUser() ?: return

        database.child("users").child(username).child("photoUrl").setValue(imageUrl ?: "")
            .addOnSuccessListener {
                android.util.Log.d("ProfileFragment", "✅ Photo URL synced to Firebase")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("ProfileFragment", "❌ Failed to sync photo URL", e)
            }
    }

    private fun checkAndRequestCameraPermission() {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.CAMERA
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(android.Manifest.permission.CAMERA)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.READ_MEDIA_IMAGES
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                permissionsNeeded.add(android.Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                permissionsNeeded.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsNeeded.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsNeeded.toTypedArray())
        } else {
            openCamera()
        }
    }

    private fun openCamera() {
        try {
            val photoFile = File(
                requireContext().getExternalFilesDir(null),
                "profile_photo_${System.currentTimeMillis()}.jpg"
            )
            photoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )
            cameraLauncher.launch(photoUri)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

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
                        photoUrl = userData.photoUrl
                    )

                    if (success) {
                        Toast.makeText(
                            requireContext(),
                            "Profil berhasil diupdate!",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadUserProfile()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Gagal update profil",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Nama lengkap tidak boleh kosong",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
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