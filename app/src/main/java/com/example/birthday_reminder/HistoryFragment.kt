package com.example.birthday_reminder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.birthday_reminder.auth.UserManager
import com.example.birthday_reminder.databinding.FragmentHistoryBinding
import com.example.birthday_reminder.messaging.MessageManager
import com.example.birthday_reminder.ui.adapter.MessageAdapter

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var messageAdapter: MessageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MessageManager.init(requireContext())
        setupRecyclerView()
        loadHistory()
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(
            messages = emptyList(),
            onMessageClick = { message ->
                // Mark as read
                MessageManager.markAsRead(message.id)
                loadHistory()
            },
            onDeleteClick = { message ->
                MessageManager.deleteMessage(message.id)
                loadHistory()
            }
        )

        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = messageAdapter
        }
    }

    private fun loadHistory() {
        val username = UserManager.getCurrentUser() ?: return

        // Gabungkan pesan yang diterima dan dikirim
        val receivedMessages = MessageManager.getReceivedMessages(username)
        val sentMessages = MessageManager.getSentMessages(username)

        val allMessages = (receivedMessages + sentMessages)
            .sortedByDescending { it.timestamp }

        messageAdapter.updateMessages(allMessages)

        // Update statistik
        binding.tvTotalMessages.text = allMessages.size.toString()
        binding.tvSentCount.text = sentMessages.size.toString()
        binding.tvReceivedCount.text = receivedMessages.size.toString()

        // Empty state
        if (allMessages.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.rvHistory.visibility = View.GONE
            binding.statsLayout.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.rvHistory.visibility = View.VISIBLE
            binding.statsLayout.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}