package com.example.birthday_reminder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.birthday_reminder.auth.UserManager
import com.example.birthday_reminder.data.model.MessageType
import com.example.birthday_reminder.databinding.FragmentSendGreetingBinding
import com.example.birthday_reminder.messaging.MessageManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SendGreetingFragment : Fragment() {
    private var _binding: FragmentSendGreetingBinding? = null
    private val binding get() = _binding!!

    private val membersList = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSendGreetingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MessageManager.init(requireContext())
        loadMembers()
        setupTemplateButton()
        setupSendButton()
    }

    private fun loadMembers() {
        // Load members dari Firebase
        val database = FirebaseDatabase.getInstance(
            "https://birthday-reminder-f26d8-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).reference

        database.child("birthdays").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                membersList.clear()
                for (data in snapshot.children) {
                    val name = data.child("name").getValue(String::class.java)
                    if (name != null) {
                        membersList.add(name)
                    }
                }

                // Setup AutoCompleteTextView untuk pilih anggota
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    membersList
                )
                binding.actvRecipient.setAdapter(adapter)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Gagal load anggota", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupTemplateButton() {
        binding.btnSelectTemplate.setOnClickListener {
            showTemplateDialog()
        }
    }

    private fun showTemplateDialog() {
        val templates = MessageManager.getGreetingTemplates()

        AlertDialog.Builder(requireContext())
            .setTitle("🎉 Pilih Template Ucapan")
            .setItems(templates.toTypedArray()) { _, which ->
                binding.etMessage.setText(templates[which])
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun setupSendButton() {
        binding.btnSend.setOnClickListener {
            sendGreeting()
        }
    }

    private fun sendGreeting() {
        val recipient = binding.actvRecipient.text.toString().trim()
        val message = binding.etMessage.text.toString().trim()

        when {
            recipient.isEmpty() -> {
                Toast.makeText(requireContext(), "Pilih penerima terlebih dahulu", Toast.LENGTH_SHORT).show()
            }
            message.isEmpty() -> {
                Toast.makeText(requireContext(), "Tulis pesan terlebih dahulu", Toast.LENGTH_SHORT).show()
            }
            !membersList.contains(recipient) -> {
                Toast.makeText(requireContext(), "Anggota tidak ditemukan", Toast.LENGTH_SHORT).show()
            }
            else -> {
                val sender = UserManager.getCurrentUser() ?: "Anonymous"
                val success = MessageManager.sendMessage(
                    from = sender,
                    to = recipient,
                    message = message,
                    type = MessageType.GREETING
                )

                if (success) {
                    Toast.makeText(requireContext(), "✅ Ucapan berhasil dikirim ke $recipient!", Toast.LENGTH_LONG).show()
                    // Reset form
                    binding.actvRecipient.setText("")
                    binding.etMessage.setText("")
                } else {
                    Toast.makeText(requireContext(), "❌ Gagal mengirim ucapan", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}