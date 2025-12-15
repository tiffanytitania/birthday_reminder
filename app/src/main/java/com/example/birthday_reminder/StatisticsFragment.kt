package com.example.birthday_reminder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.birthday_reminder.ui.viewmodel.StatisticsViewModel
import kotlinx.coroutines.launch

class StatisticsFragment : Fragment() {

    // ViewModel instance
    private val viewModel: StatisticsViewModel by viewModels()

    private lateinit var tvTotalMembers: TextView
    private lateinit var tvBirthdaysToday: TextView
    private lateinit var tvBirthdaysThisWeek: TextView
    private lateinit var tvBirthdaysThisMonth: TextView
    private lateinit var tvAverageAge: TextView
    private lateinit var tvOldestMember: TextView
    private lateinit var tvYoungestMember: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_statistics, container, false)

        // Initialize views
        tvTotalMembers = view.findViewById(R.id.tvTotalMembers)
        tvBirthdaysToday = view.findViewById(R.id.tvBirthdaysToday)
        tvBirthdaysThisWeek = view.findViewById(R.id.tvBirthdaysThisWeek)
        tvBirthdaysThisMonth = view.findViewById(R.id.tvBirthdaysThisMonth)
        tvAverageAge = view.findViewById(R.id.tvAverageAge)
        tvOldestMember = view.findViewById(R.id.tvOldestMember)
        tvYoungestMember = view.findViewById(R.id.tvYoungestMember)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
    }

    //Observe ViewModel
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe statistics data
                launch {
                    viewModel.statistics.collect { stats ->
                        updateUI(stats)
                    }
                }

                // Observe loading state
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        // Bisa tambahkan progress bar kalau mau
                        view?.findViewById<View>(R.id.progressBar)?.visibility =
                            if (isLoading) View.VISIBLE else View.GONE
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
            }
        }
    }

    private fun updateUI(stats: com.example.birthday_reminder.data.repository.StatisticsData) {
        tvTotalMembers.text = stats.totalMembers.toString()
        tvBirthdaysToday.text = stats.birthdaysToday.toString()
        tvBirthdaysThisWeek.text = stats.birthdaysThisWeek.toString()
        tvBirthdaysThisMonth.text = stats.birthdaysThisMonth.toString()
        tvAverageAge.text = "${stats.averageAge} tahun"
        tvOldestMember.text = stats.oldestMember
        tvYoungestMember.text = stats.youngestMember
    }
}