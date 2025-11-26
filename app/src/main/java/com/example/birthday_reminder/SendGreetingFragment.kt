package com.example.birthday_reminder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.example.birthday_reminder.auth.UserManager
import com.example.birthday_reminder.databinding.FragmentSendGreetingBinding
import com.example.birthday_reminder.messaging.MessageManager
import com.example.birthday_reminder.ml.SentimentAnalyzer
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SendGreetingFragment : Fragment() {
    private var _binding: FragmentSendGreetingBinding? = null
    private val binding get() = _binding!!

    private val membersList = mutableListOf<String>()

    // 🆕 ML Feature
    private lateinit var sentimentAnalyzer: SentimentAnalyzer

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

        // 🆕 Initialize ML
        sentimentAnalyzer = SentimentAnalyzer(requireContext())

        loadMembers()
        setupTemplateButton()
        setupSendButton()
        setupSentimentAnalysis() // 🆕 Tambahkan ini
    }

    private fun loadMembers() {
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

                if (isAdded && _binding != null) {
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        membersList
                    )
                    binding.actvRecipient.setAdapter(adapter)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (isAdded) {
                    Toast.makeText(
                        requireContext(),
                        "Gagal memuat data anggota",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    private fun setupTemplateButton() {
        binding.btnSelectTemplate.setOnClickListener {
            showTemplateDialog()
        }
    }

    private fun showTemplateDialog() {
        if (!isAdded) return

        val templates = MessageManager.getGreetingTemplates()
        val templateArray = templates.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("📝 Pilih Template Ucapan")
            .setItems(templateArray) { dialog, which ->
                binding.etMessage.setText(templates[which])
                dialog.dismiss()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun setupSendButton() {
        binding.btnSend.setOnClickListener {
            val recipient = binding.actvRecipient.text.toString().trim()
            val message = binding.etMessage.text.toString().trim()

            when {
                recipient.isEmpty() -> {
                    Toast.makeText(requireContext(), "⚠️ Pilih penerima dulu", Toast.LENGTH_SHORT).show()
                }
                message.isEmpty() -> {
                    Toast.makeText(requireContext(), "⚠️ Tulis pesan dulu", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    sendGreeting(recipient, message)
                }
            }
        }
    }

    private fun sendGreeting(recipient: String, message: String) {
        val sender = UserManager.getCurrentUser() ?: "Unknown"

        val success = MessageManager.sendMessage(
            from = sender,
            to = recipient,
            message = message,
            type = com.example.birthday_reminder.data.model.MessageType.GREETING
        )

        if (success) {
            Toast.makeText(
                requireContext(),
                "✅ Ucapan berhasil dikirim ke $recipient!",
                Toast.LENGTH_SHORT
            ).show()

            // Clear form
            binding.actvRecipient.setText("")
            binding.etMessage.setText("")
        } else {
            Toast.makeText(
                requireContext(),
                "❌ Gagal mengirim ucapan",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // 🆕 Setup Sentiment Analysis
    private fun setupSentimentAnalysis() {
        // Analyze saat user mengetik
        binding.etMessage.addTextChangedListener { text ->
            val message = text.toString().trim()

            if (message.isEmpty()) {
                binding.tvSentimentResult.text = "Sentiment: Ketik pesan untuk analisis..."
                binding.tvConfidence.text = "Confidence: -"
            } else {
                // Analyze sentiment
                val sentiment = sentimentAnalyzer.analyzeSentiment(message)
                val confidence = sentimentAnalyzer.getConfidence(message)

                binding.tvSentimentResult.text = "Sentiment: $sentiment"
                binding.tvConfidence.text = "Confidence: $confidence%"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}