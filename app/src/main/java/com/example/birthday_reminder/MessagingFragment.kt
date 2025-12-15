package com.example.birthday_reminder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.birthday_reminder.auth.UserManager
import com.example.birthday_reminder.databinding.FragmentMessagingBinding
import com.example.birthday_reminder.messaging.MessageManager
import com.example.birthday_reminder.ui.adapter.MessageAdapter
import com.google.android.material.tabs.TabLayout

class MessagingFragment : Fragment() {
    private var _binding: FragmentMessagingBinding? = null
    private val binding get() = _binding!!

    private lateinit var messageAdapter: MessageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMessagingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MessageManager.init(requireContext())
        setupRecyclerView()
        setupTabs()
        loadInbox()
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(
            messages = emptyList(),
            onMessageClick = { message ->
                // Tandai sebagai sudah dibaca
                MessageManager.markAsRead(message.id)
                // Refresh
                refreshCurrentTab()
            },
            onDeleteClick = { message ->
                MessageManager.deleteMessage(message.id)
                refreshCurrentTab()
            }
        )

        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = messageAdapter
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> loadInbox()
                    1 -> loadSentMessages()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadInbox() {
        val username = UserManager.getCurrentUser() ?: return
        val messages = MessageManager.getReceivedMessages(username)

        messageAdapter.updateMessages(messages)
        updateEmptyState(messages.isEmpty(), "Inbox")
        updateUnreadBadge()
    }

    private fun loadSentMessages() {
        val username = UserManager.getCurrentUser() ?: return
        val messages = MessageManager.getSentMessages(username)

        messageAdapter.updateMessages(messages)
        updateEmptyState(messages.isEmpty(), "Terkirim")
    }

    private fun updateEmptyState(isEmpty: Boolean, tabName: String) {
        if (isEmpty) {
            binding.emptyState.visibility = View.VISIBLE
            binding.rvMessages.visibility = View.GONE
            binding.tvEmptyMessage.text = "Tidak ada pesan di $tabName"
        } else {
            binding.emptyState.visibility = View.GONE
            binding.rvMessages.visibility = View.VISIBLE
        }
    }

    private fun updateUnreadBadge() {
        val username = UserManager.getCurrentUser() ?: return
        val unreadCount = MessageManager.getUnreadCount(username)

        if (unreadCount > 0) {
            binding.tvUnreadBadge.visibility = View.VISIBLE
            binding.tvUnreadBadge.text = unreadCount.toString()
        } else {
            binding.tvUnreadBadge.visibility = View.GONE
        }
    }

    private fun refreshCurrentTab() {
        when (binding.tabLayout.selectedTabPosition) {
            0 -> loadInbox()
            1 -> loadSentMessages()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}