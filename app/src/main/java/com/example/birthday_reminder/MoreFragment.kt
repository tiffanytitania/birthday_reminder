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
import androidx.recyclerview.widget.RecyclerView
import com.example.birthday_reminder.data.api.RetrofitInstance
import com.example.birthday_reminder.data.model.Quote
import com.example.birthday_reminder.ui.adapter.QuoteAdapter
import com.example.birthday_reminder.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MoreFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private var adapter: QuoteAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_more, container, false)
        recyclerView = view.findViewById(R.id.rvQuotes)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Pastikan permission notifikasi di Android 13+ sudah diberikan
        requestNotificationPermission()

        // Ambil data ucapan dari API
        fetchQuotes()
        return view
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
                recyclerView.adapter = adapter

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
}
