package com.example.birthday_reminder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.birthday_reminder.data.api.RetrofitInstance
import com.example.birthday_reminder.data.model.Quote
import com.example.birthday_reminder.databinding.FragmentMoreBinding
import com.example.birthday_reminder.ui.adapter.QuoteAdapter
import com.example.birthday_reminder.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MoreFragment : Fragment() {

    private var _binding: FragmentMoreBinding? = null
    private val binding get() = _binding!!

    private var adapter: QuoteAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup RecyclerView
        binding.rvQuotes.layoutManager = LinearLayoutManager(requireContext())

        // Pastikan permission notifikasi di Android 13+ sudah diberikan
        requestNotificationPermission()

        // 🆕 Setup navigation cards untuk messaging features
        setupMessagingMenu()

        // Ambil data ucapan dari API
        fetchQuotes()
    }

    // 🆕 Setup menu cards untuk Kirim Ucapan & Kotak Pesan
    private fun setupMessagingMenu() {
        // Card: Kirim Ucapan
        binding.cardSendGreeting.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_container, SendGreetingFragment())
                .addToBackStack(null)
                .commit()
        }

        // Card: Kotak Pesan (Inbox)
        binding.cardInbox.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_container, MessagingFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun fetchQuotes() {
        lifecycleScope.launch {
            try {
                val quotes = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.getQuotes()
                }

                adapter = QuoteAdapter(quotes) { selected ->
                    sendBirthdayMessage(selected)
                }
                binding.rvQuotes.adapter = adapter

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    requireContext(),
                    "Gagal mengambil data: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun sendBirthdayMessage(quote: Quote) {
        val text = quote.text ?: "Selamat ulang tahun penuh kebahagiaan!"
        val author = quote.author ?: "Anonim"
        val message = "Selamat Ulang Tahun! 🎉\n\n\"$text\"\n— $author"

        // 🔹 Tampilkan notifikasi langsung di HP (fitur kirim antar pengguna)
        NotificationHelper.showBirthdayNotification(
            requireContext(),
            name = "Temanmu", // nanti bisa diganti nama user dari database
            message = message
        )

        // 🔹 (Masih bisa kirim lewat app lain juga)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        startActivity(Intent.createChooser(intent, "Kirim ucapan via..."))
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}