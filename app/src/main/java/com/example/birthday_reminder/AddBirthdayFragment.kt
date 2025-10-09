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
    private val filteredList = mutableListOf<BirthdayItem>()
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
            setupSearch()
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
        adapter = BirthdayAdapter(filteredList,
            onDeleteClick = { birthdayItem ->
                showDeleteConfirmation(birthdayItem)
            },
            onEditClick = { birthdayItem ->  // 🆕 EDIT CALLBACK
                showEditBirthdayDialog(birthdayItem)
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterBirthdays(newText ?: "")
                return true
            }
        })
    }

    private fun filterBirthdays(query: String) {
        filteredList.clear()

        if (query.isEmpty()) {
            filteredList.addAll(birthdayList)
        } else {
            val filtered = birthdayList.filter { item ->
                item.name.lowercase().contains(query.lowercase())
            }
            filteredList.addAll(filtered)
        }

        adapter.notifyDataSetChanged()
        updateEmptyState()
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

                activity?.runOnUiThread {
                    if (isAdded && _binding != null) {
                        val currentQuery = binding.searchView.query.toString()
                        filterBirthdays(currentQuery)
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

        if (filteredList.isEmpty()) {
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
            val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)

            tvTitle.text = "🎉 Tambah Ulang Tahun Baru"

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

    // 🆕 FITUR BARU: EDIT DIALOG
    private fun showEditBirthdayDialog(birthdayItem: BirthdayItem) {
        if (!isAdded || context == null) return

        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_add_birthday, null)
            val etName = dialogView.findViewById<EditText>(R.id.etDialogName)
            val etDate = dialogView.findViewById<EditText>(R.id.etDialogDate)
            val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)

            // Pre-fill dengan data existing
            etName.setText(birthdayItem.name)
            etDate.setText(birthdayItem.date)
            tvTitle.text = "✏️ Edit Ulang Tahun"

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
                .setView(dialogView)
                .setPositiveButton("Update") { dialog, _ ->
                    val name = etName.text.toString().trim()
                    val date = etDate.text.toString().trim()

                    if (name.isEmpty() || date.isEmpty()) {
                        Toast.makeText(requireContext(), "Harap isi semua field", Toast.LENGTH_SHORT).show()
                    } else {
                        updateBirthday(birthdayItem.key, name, date)
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

    // 🆕 FITUR BARU: UPDATE LOGIC
    private fun updateBirthday(key: String, name: String, date: String) {
        try {
            val updates = mapOf(
                "name" to name,
                "date" to date
            )

            database.child("birthdays").child(key).updateChildren(updates)
                .addOnSuccessListener {
                    activity?.runOnUiThread {
                        if (isAdded && context != null) {
                            Toast.makeText(requireContext(), "Berhasil diupdate", Toast.LENGTH_SHORT).show()
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
        birthdayListener?.let {
            database.child("birthdays").removeEventListener(it)
        }
        _binding = null
    }
}

data class BirthdayItem(
    val key: String,
    val name: String,
    val date: String
)

class BirthdayAdapter(
    private val items: MutableList<BirthdayItem>,
    private val onDeleteClick: (BirthdayItem) -> Unit,
    private val onEditClick: (BirthdayItem) -> Unit  // 🆕 EDIT CALLBACK
) : RecyclerView.Adapter<BirthdayAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)  // 🆕 EDIT BUTTON
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

        holder.btnEdit.setOnClickListener {
            onEditClick(item)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(item)
        }
    }

    override fun getItemCount() = items.size
}