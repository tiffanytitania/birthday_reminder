package com.example.birthday_reminder.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.birthday_reminder.data.repository.StatisticsData
import com.example.birthday_reminder.data.repository.StatisticsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StatisticsViewModel(
    private val repository: StatisticsRepository = StatisticsRepository()
) : ViewModel() {

    private val _statistics = MutableStateFlow(StatisticsData())
    val statistics: StateFlow<StatisticsData> = _statistics.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadStatistics()
    }

    fun loadStatistics() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getStatistics()
            _isLoading.value = false

            result.onSuccess { stats ->
                _statistics.value = stats
                _error.value = null
            }.onFailure { exception ->
                _error.value = exception.message
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}