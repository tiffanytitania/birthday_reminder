package com.example.birthday_reminder

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.birthday_reminder.databinding.FragmentAddBirthdayBinding
import com.google.firebase.database.*
import java.util.*

class AddBirthdayFragment : Fragment() {
    private var _binding: FragmentAddBirthdayBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: DatabaseReference
    private lateinit var adapter: BirthdayAdapter
    private val birthdayList = mutableListOf<BirthdayItem>()
    private var birthdayListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddBirthdayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            database = FirebaseDatabase.getInstance("https://birthday-reminder-f26d8-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .reference

            setupRecyclerView()
            loadBirthdays()

            binding.fabAdd.setOnClickListener {
                showAddBirthdayDialog()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        adapter = BirthdayAdapter(birthdayList) { birthdayItem ->
            showDeleteConfirmation(birthdayItem)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun loadBirthdays() {
        birthdayListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                birthdayList.clear()
                for (data in snapshot.children) {
                    try {
                        val name = data.child("name").getValue(String::class.java) ?: ""
                        val date = data.child("date").getValue(String::class.java) ?: ""
                        val key = data.key ?: ""
                        if (name.isNotEmpty() && date.isNotEmpty()) {
                            birthdayList.add(BirthdayItem(key, name, date))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Update adapter safely
                activity?.runOnUiThread {
                    if (isAdded && _binding != null) {
                        adapter.notifyDataSetChanged()
                        updateEmptyState()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                activity?.runOnUiThread {
                    if (isAdded && context != null) {
                        Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        database.child("birthdays").addValueEventListener(birthdayListener!!)
    }

    private fun updateEmptyState() {
        if (_binding == null) return

        if (birthdayList.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }

    private fun showAddBirthdayDialog() {
        if (!isAdded || context == null) return

        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_add_birthday, null)
            val etName = dialogView.findViewById<EditText>(R.id.etDialogName)
            val etDate = dialogView.findViewById<EditText>(R.id.etDialogDate)

            // Date picker untuk field tanggal
            etDate.setOnClickListener {
                if (!isAdded || context == null) return@setOnClickListener

                try {
                    val calendar = Calendar.getInstance()
                    val year = calendar.get(Calendar.YEAR)
                    val month = calendar.get(Calendar.MONTH)
                    val day = calendar.get(Calendar.DAY_OF_MONTH)

                    val datePicker = DatePickerDialog(requireContext(),
                        { _, selectedYear, selectedMonth, selectedDay ->
                            etDate.setText("$selectedDay/${selectedMonth + 1}/$selectedYear")
                        }, year, month, day)

                    datePicker.show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Error membuka date picker", Toast.LENGTH_SHORT).show()
                }
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Tambah Ulang Tahun")
                .setView(dialogView)
                .setPositiveButton("Simpan") { dialog, _ ->
                    val name = etName.text.toString().trim()
                    val date = etDate.text.toString().trim()

                    if (name.isEmpty() || date.isEmpty()) {
                        Toast.makeText(requireContext(), "Harap isi semua field", Toast.LENGTH_SHORT).show()
                    } else {
                        saveBirthday(name, date)
                        dialog.dismiss()
                    }
                }
                .setNegativeButton("Batal") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveBirthday(name: String, date: String) {
        try {
            // buat map sederhana supaya tidak tergantung pada class Birthday
            val birthdayMap = mapOf(
                "name" to name,
                "date" to date
            )

            database.child("birthdays").push().setValue(birthdayMap)
                .addOnSuccessListener {
                    activity?.runOnUiThread {
                        if (isAdded && context != null) {
                            Toast.makeText(requireContext(), "Berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    activity?.runOnUiThread {
                        if (isAdded && context != null) {
                            Toast.makeText(requireContext(), "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmation(birthdayItem: BirthdayItem) {
        if (!isAdded || context == null) return

        try {
            AlertDialog.Builder(requireContext())
                .setTitle("Hapus Ulang Tahun")
                .setMessage("Yakin ingin menghapus ${birthdayItem.name}?")
                .setPositiveButton("Hapus") { dialog, _ ->
                    deleteBirthday(birthdayItem.key)
                    dialog.dismiss()
                }
                .setNegativeButton("Batal") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteBirthday(key: String) {
        try {
            database.child("birthdays").child(key).removeValue()
                .addOnSuccessListener {
                    activity?.runOnUiThread {
                        if (isAdded && context != null) {
                            Toast.makeText(requireContext(), "Berhasil dihapus", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    activity?.runOnUiThread {
                        if (isAdded && context != null) {
                            Toast.makeText(requireContext(), "Gagal menghapus: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove listener to prevent memory leak
        birthdayListener?.let {
            database.child("birthdays").removeEventListener(it)
        }
        _binding = null
    }
}

// Data class untuk item dengan key
data class BirthdayItem(
    val key: String,
    val name: String,
    val date: String
)

// Adapter untuk RecyclerView
class BirthdayAdapter(
    private val items: MutableList<BirthdayItem>,
    private val onDeleteClick: (BirthdayItem) -> Unit
) : RecyclerView.Adapter<BirthdayAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_birthday, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvDate.text = item.date
        holder.btnDelete.setOnClickListener {
            onDeleteClick(item)
        }
    }

    override fun getItemCount() = items.size
}