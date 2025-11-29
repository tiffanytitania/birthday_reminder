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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.birthday_reminder.auth.UserManager
import com.example.birthday_reminder.databinding.FragmentAddBirthdayBinding
import com.example.birthday_reminder.ui.viewmodel.BirthdayViewModel
import kotlinx.coroutines.launch
import java.util.*

class AddBirthdayFragment : Fragment() {
    private var _binding: FragmentAddBirthdayBinding? = null
    private val binding get() = _binding!!

    // 🆕 ViewModel instance
    private val viewModel: BirthdayViewModel by viewModels()

    private lateinit var adapter: BirthdayAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddBirthdayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        setupFAB()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        val isAdmin = UserManager.isAdmin()

        adapter = BirthdayAdapter(
            items = mutableListOf(),
            isAdmin = isAdmin,
            onDeleteClick = { birthdayItem ->
                if (isAdmin) {
                    showDeleteConfirmation(birthdayItem)
                } else {
                    Toast.makeText(requireContext(), "⛔ Hanya admin yang bisa menghapus", Toast.LENGTH_SHORT).show()
                }
            },
            onEditClick = { birthdayItem ->
                if (isAdmin) {
                    showEditBirthdayDialog(birthdayItem)
                } else {
                    Toast.makeText(requireContext(), "⛔ Hanya admin yang bisa mengedit", Toast.LENGTH_SHORT).show()
                }
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun setupFAB() {
        val isAdmin = UserManager.isAdmin()

        if (isAdmin) {
            binding.fabAdd.visibility = View.VISIBLE
            binding.fabAdd.setOnClickListener {
                showAddBirthdayDialog()
            }
        } else {
            binding.fabAdd.visibility = View.GONE
        }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchBirthdays(newText ?: "")
                return true
            }
        })
    }

    // 🆕 Observe ViewModel states
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe filtered birthdays
                launch {
                    viewModel.filteredBirthdays.collect { birthdays ->
                        adapter.updateItems(birthdays)
                        updateEmptyState(birthdays.isEmpty())
                    }
                }

                // Observe loading state
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    }
                }

                // Observe error
                launch {
                    viewModel.error.collect { error ->
                        error?.let {
                            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                            viewModel.clearError()
                        }
                    }
                }

                // Observe success message
                launch {
                    viewModel.successMessage.collect { message ->
                        message?.let {
                            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                            viewModel.clearSuccessMessage()
                        }
                    }
                }
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyState.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }

    private fun showAddBirthdayDialog() {
        if (!isAdded || context == null) return

        val dialogView = layoutInflater.inflate(R.layout.dialog_add_birthday, null)
        val etName = dialogView.findViewById<EditText>(R.id.etDialogName)
        val etDate = dialogView.findViewById<EditText>(R.id.etDialogDate)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)

        tvTitle.text = "🎉 Tambah Ulang Tahun Baru"

        etDate.setOnClickListener {
            showDatePicker { date -> etDate.setText(date) }
        }

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Simpan") { dialog, _ ->
                val name = etName.text.toString().trim()
                val date = etDate.text.toString().trim()

                // 🆕 Call ViewModel instead of direct Firebase
                viewModel.addBirthday(name, date)
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showEditBirthdayDialog(birthdayItem: BirthdayItem) {
        if (!isAdded || context == null) return

        val dialogView = layoutInflater.inflate(R.layout.dialog_add_birthday, null)
        val etName = dialogView.findViewById<EditText>(R.id.etDialogName)
        val etDate = dialogView.findViewById<EditText>(R.id.etDialogDate)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)

        etName.setText(birthdayItem.name)
        etDate.setText(birthdayItem.date)
        tvTitle.text = "✏️ Edit Ulang Tahun"

        etDate.setOnClickListener {
            showDatePicker { date -> etDate.setText(date) }
        }

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Update") { dialog, _ ->
                val name = etName.text.toString().trim()
                val date = etDate.text.toString().trim()

                // 🆕 Call ViewModel
                viewModel.updateBirthday(birthdayItem.key, name, date)
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showDeleteConfirmation(birthdayItem: BirthdayItem) {
        if (!isAdded || context == null) return

        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Ulang Tahun")
            .setMessage("Yakin ingin menghapus ${birthdayItem.name}?")
            .setPositiveButton("Hapus") { dialog, _ ->
                // 🆕 Call ViewModel
                viewModel.deleteBirthday(birthdayItem.key)
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                onDateSelected("$day/${month + 1}/$year")
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// 🆕 Data class di sini (dalam file yang sama)
data class BirthdayItem(
    val key: String,
    val name: String,
    val date: String
)

// 🆕 Adapter dengan update function
class BirthdayAdapter(
    private val items: MutableList<BirthdayItem>,
    private val isAdmin: Boolean,
    private val onDeleteClick: (BirthdayItem) -> Unit,
    private val onEditClick: (BirthdayItem) -> Unit
) : RecyclerView.Adapter<BirthdayAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
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

        if (isAdmin) {
            holder.btnEdit.visibility = View.VISIBLE
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnEdit.setOnClickListener { onEditClick(item) }
            holder.btnDelete.setOnClickListener { onDeleteClick(item) }
        } else {
            holder.btnEdit.visibility = View.GONE
            holder.btnDelete.visibility = View.GONE
        }
    }

    override fun getItemCount() = items.size

    // 🆕 Function untuk update data dari ViewModel
    fun updateItems(newItems: List<BirthdayItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}