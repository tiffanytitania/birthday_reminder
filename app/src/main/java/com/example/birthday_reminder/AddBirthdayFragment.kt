package com.example.birthday_reminder

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.birthday_reminder.auth.UserManager
import com.example.birthday_reminder.databinding.FragmentAddBirthdayBinding
import com.example.birthday_reminder.ui.viewmodel.BirthdayViewModel
import com.example.birthday_reminder.ui.viewmodel.BirthdayItem
import kotlinx.coroutines.launch
import java.util.*

class AddBirthdayFragment : Fragment() {

    private var _binding: FragmentAddBirthdayBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BirthdayViewModel by viewModels()
    private lateinit var adapter: BirthdayListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
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

        adapter = BirthdayListAdapter(
            items = mutableListOf(),
            isAdmin = isAdmin,
            onDeleteClick = { birthdayItem ->
                showDeleteConfirmation(birthdayItem)
            },
            onEditClick = { birthdayItem ->
                showEditBirthdayDialog(birthdayItem)
            },
            layoutResId = R.layout.item_birthday
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AddBirthdayFragment.adapter
        }
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
        binding.searchView.setOnQueryTextListener(
            object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean = false

                override fun onQueryTextChange(newText: String?): Boolean {
                    viewModel.searchBirthdays(newText ?: "")
                    return true
                }
            }
        )
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filteredBirthdays.collect { birthdays ->
                adapter.updateItems(birthdays)
                updateEmptyState(birthdays.isEmpty())
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.successMessage.collect { message ->
                message?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    viewModel.clearSuccessMessage()
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

                if (name.isEmpty() || date.isEmpty()) {
                    Toast.makeText(requireContext(), "Isi semua field!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

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

                if (name.isEmpty() || date.isEmpty()) {
                    Toast.makeText(requireContext(), "Isi semua field!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

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