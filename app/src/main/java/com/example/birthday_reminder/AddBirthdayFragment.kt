package com.example.birthday_reminder

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.birthday_reminder.databinding.FragmentAddBirthdayBinding
import java.util.*
import com.google.firebase.database.FirebaseDatabase

class AddBirthdayFragment : Fragment() {
    private var _binding: FragmentAddBirthdayBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddBirthdayBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Saat field tanggal diklik
        binding.etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    binding.etDate.setText("$selectedDay/${selectedMonth + 1}/$selectedYear")
                }, year, month, day)

            datePicker.show()
        }

        // Tombol simpan
        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val date = binding.etDate.text.toString().trim()

            if (name.isEmpty() || date.isEmpty()) {
                Toast.makeText(requireContext(), "Harap isi semua field", Toast.LENGTH_SHORT).show()
            } else {
                val birthday = Birthday(name, date)

                // Ambil referensi database
                val database = FirebaseDatabase.getInstance("https://birthday-reminder-f26d8-default-rtdb.asia-southeast1.firebasedatabase.app/")
                val ref = database.getReference("birthdays") // node utama

                // Push data ke Firebase
                ref.push().setValue(birthday)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Data berhasil disimpan", Toast.LENGTH_SHORT).show()
                        binding.etName.text?.clear()
                        binding.etDate.text?.clear()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
