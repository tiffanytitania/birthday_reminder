package com.example.birthday_reminder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.birthday_reminder.data.model.Member
import com.example.birthday_reminder.databinding.FragmentMemberDirectoryBinding
import com.example.birthday_reminder.ui.adapter.MemberAdapter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MemberDirectoryFragment : Fragment() {
    private var _binding: FragmentMemberDirectoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var memberAdapter: MemberAdapter
    private val allMembers = mutableListOf<Member>()
    private val filteredMembers = mutableListOf<Member>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMemberDirectoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        loadMembers()
    }

    private fun setupRecyclerView() {
        memberAdapter = MemberAdapter(
            members = filteredMembers,
            onMemberClick = { member ->
                showMemberDetail(member)
            },
            onWhatsAppClick = { member ->
                openWhatsApp(member)
            },
            onCallClick = { member ->
                callMember(member)
            },
            onSmsClick = { member ->
                sendSms(member)
            }
        )

        binding.rvMembers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = memberAdapter
        }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterMembers(newText ?: "")
                return true
            }
        })
    }

    private fun loadMembers() {
        binding.progressBar.visibility = View.VISIBLE

        val database = FirebaseDatabase.getInstance(
            "https://birthday-reminder-fa6fb-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).reference

        // Load dari node birthdays
        database.child("birthdays").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allMembers.clear()

                for (data in snapshot.children) {
                    val name = data.child("name").getValue(String::class.java) ?: continue
                    val date = data.child("date").getValue(String::class.java) ?: ""
                    val phone = data.child("phone").getValue(String::class.java) ?: ""
                    val email = data.child("email").getValue(String::class.java) ?: ""

                    val member = Member(
                        id = data.key ?: "",
                        name = name,
                        phone = phone,
                        email = email,
                        birthDate = date
                    )

                    allMembers.add(member)
                }

                // Sort by name
                allMembers.sortBy { it.name }

                // Update filtered list
                filteredMembers.clear()
                filteredMembers.addAll(allMembers)
                memberAdapter.notifyDataSetChanged()

                // Update UI
                updateUI()
                binding.progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Gagal memuat data: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
                binding.progressBar.visibility = View.GONE
            }
        })
    }

    private fun filterMembers(query: String) {
        filteredMembers.clear()

        if (query.isEmpty()) {
            filteredMembers.addAll(allMembers)
        } else {
            val lowerQuery = query.lowercase()
            filteredMembers.addAll(
                allMembers.filter {
                    it.name.lowercase().contains(lowerQuery) ||
                            it.phone.contains(query) ||
                            it.email.lowercase().contains(lowerQuery)
                }
            )
        }

        memberAdapter.notifyDataSetChanged()
        updateUI()
    }

    private fun updateUI() {
        binding.tvTotalMembers.text = "${allMembers.size} Anggota"

        if (filteredMembers.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.rvMembers.visibility = View.GONE

            val message = if (allMembers.isEmpty()) {
                "Belum ada anggota"
            } else {
                "Tidak ada hasil pencarian"
            }
            binding.tvEmptyMessage.text = message
        } else {
            binding.emptyState.visibility = View.GONE
            binding.rvMembers.visibility = View.VISIBLE
        }
    }

    private fun showMemberDetail(member: Member) {
        val bundle = Bundle().apply {
            putString("member_id", member.id)
            putString("member_name", member.name)
            putString("member_phone", member.phone)
            putString("member_email", member.email)
            putString("member_birthdate", member.birthDate)
        }

        val fragment = MemberDetailFragment().apply {
            arguments = bundle
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun openWhatsApp(member: Member) {
        if (member.phone.isEmpty()) {
            Toast.makeText(requireContext(), "Nomor HP tidak tersedia", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val phone = member.phone.replace("[^0-9]".toRegex(), "")
            val phoneWithCountryCode = if (phone.startsWith("0")) {
                "62${phone.substring(1)}"
            } else {
                phone
            }

            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("https://wa.me/$phoneWithCountryCode")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "WhatsApp tidak terinstall", Toast.LENGTH_SHORT).show()
        }
    }

    private fun callMember(member: Member) {
        if (member.phone.isEmpty()) {
            Toast.makeText(requireContext(), "Nomor HP tidak tersedia", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                data = android.net.Uri.parse("tel:${member.phone}")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal membuka dialer", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendSms(member: Member) {
        if (member.phone.isEmpty()) {
            Toast.makeText(requireContext(), "Nomor HP tidak tersedia", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("sms:${member.phone}")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal membuka SMS", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}