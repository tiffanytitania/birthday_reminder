package com.example.birthday_reminder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.birthday_reminder.auth.UserManager
import com.example.birthday_reminder.data.model.Member
import com.example.birthday_reminder.databinding.FragmentExportDataBinding
import com.example.birthday_reminder.utils.ExportManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ExportDataFragment : Fragment() {
    private var _binding: FragmentExportDataBinding? = null
    private val binding get() = _binding!!

    private val allMembers = mutableListOf<Member>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExportDataBinding.inflate(inflater, container, false)
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

        setupButtons()
        loadData()
    }

    private fun setupButtons() {
        // Export Birthday List
        binding.btnExportBirthdays.setOnClickListener {
            showExportFormatDialog(ExportManager.ExportType.BIRTHDAYS)
        }

        // Export Contacts
        binding.btnExportContacts.setOnClickListener {
            showExportFormatDialog(ExportManager.ExportType.CONTACTS)
        }
    }

    private fun loadData() {
        binding.progressBar.visibility = View.VISIBLE

        val database = FirebaseDatabase.getInstance(
            "https://birthday-reminder-f26d8-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).reference

        database.child("birthdays").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allMembers.clear()

                for (data in snapshot.children) {
                    val name = data.child("name").getValue(String::class.java) ?: continue
                    val date = data.child("date").getValue(String::class.java) ?: ""
                    val phone = data.child("phone").getValue(String::class.java) ?: ""
                    val email = data.child("email").getValue(String::class.java) ?: ""
                    val role = data.child("role").getValue(String::class.java) ?: "member"

                    val member = Member(
                        id = data.key ?: "",
                        name = name,
                        phone = phone,
                        email = email,
                        birthDate = date,
                        role = role
                    )

                    allMembers.add(member)
                }

                allMembers.sortBy { it.name }
                updateUI()
                binding.progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Gagal memuat data", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
            }
        })
    }

    private fun updateUI() {
        binding.tvTotalMembers.text = "${allMembers.size} Anggota"
        binding.tvTotalContacts.text = "${allMembers.count { it.phone.isNotEmpty() }} Kontak"
    }

    private fun showExportFormatDialog(type: ExportManager.ExportType) {
        val formats = arrayOf("📄 Text File (.txt)", "📊 CSV File (.csv)")

        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Format Export")
            .setItems(formats) { _, which ->
                when (which) {
                    0 -> exportAsText(type)
                    1 -> exportAsCSV(type)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun exportAsText(type: ExportManager.ExportType) {
        binding.progressBar.visibility = View.VISIBLE

        val file = ExportManager.exportToText(requireContext(), allMembers, type)

        binding.progressBar.visibility = View.GONE

        if (file != null) {
            showShareDialog(file, type)
        } else {
            Toast.makeText(requireContext(), "❌ Gagal export data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportAsCSV(type: ExportManager.ExportType) {
        binding.progressBar.visibility = View.VISIBLE

        val file = when (type) {
            ExportManager.ExportType.BIRTHDAYS -> ExportManager.exportBirthdaysToCSV(requireContext(), allMembers)
            ExportManager.ExportType.CONTACTS -> ExportManager.exportContactsToCSV(requireContext(), allMembers)
        }

        binding.progressBar.visibility = View.GONE

        if (file != null) {
            showShareDialog(file, type)
        } else {
            Toast.makeText(requireContext(), "❌ Gagal export data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showShareDialog(file: java.io.File, type: ExportManager.ExportType) {
        val typeName = when (type) {
            ExportManager.ExportType.BIRTHDAYS -> "Daftar Ulang Tahun"
            ExportManager.ExportType.CONTACTS -> "Daftar Kontak"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("✅ Export Berhasil!")
            .setMessage("File disimpan: ${file.name}\n\nMau bagikan sekarang?")
            .setPositiveButton("Bagikan") { _, _ ->
                ExportManager.shareFile(requireContext(), file, "Bagikan $typeName")
            }
            .setNegativeButton("Nanti", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}