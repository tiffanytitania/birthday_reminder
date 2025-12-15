package com.example.birthday_reminder

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.birthday_reminder.databinding.FragmentHomeBinding
import com.example.birthday_reminder.ui.viewmodel.BirthdayViewModel
import com.example.birthday_reminder.ui.viewmodel.BirthdayItem
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BirthdayViewModel by viewModels()
    private lateinit var birthdayAdapter: BirthdayListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupCalendar()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        birthdayAdapter = BirthdayListAdapter(
            items = mutableListOf(),
            isAdmin = false,
            onDeleteClick = null,
            onEditClick = null,
            layoutResId = R.layout.item_home_birthday
        )

        binding.birthdayRecycler.apply {
            adapter = birthdayAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupCalendar() {
        binding.calendarView.setOnDateChangedListener { _, date, _ ->
            viewModel.filterBirthdaysByDate(date.day, date.month + 1)
        }

        val today = CalendarDay.today()
        binding.calendarView.selectedDate = today
        viewModel.filterBirthdaysByDate(today.day, today.month + 1)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.birthdays.collect { birthdays ->
                highlightBirthdays(birthdays)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filteredBirthdays.collect { filtered ->
                birthdayAdapter.updateItems(filtered)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                if (error != null) {
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.successMessage.collect { message ->
                if (message != null) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    viewModel.clearSuccessMessage()
                }
            }
        }
    }

    private fun highlightBirthdays(birthdays: List<BirthdayItem>) {
        val decorator = object : DayViewDecorator {
            override fun shouldDecorate(day: CalendarDay): Boolean {
                return birthdays.any { birthday ->
                    val parts = birthday.date.split("/")
                    if (parts.size >= 2) {
                        val bdayDay = parts[0].toIntOrNull() ?: 0
                        val bdayMonth = parts[1].toIntOrNull() ?: 0
                        (bdayDay == day.day && bdayMonth == (day.month + 1))
                    } else {
                        false
                    }
                }
            }

            override fun decorate(view: DayViewFacade) {
                view.addSpan(DotSpan(8f, Color.RED))
            }
        }

        binding.calendarView.removeDecorators()
        binding.calendarView.addDecorator(decorator)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}