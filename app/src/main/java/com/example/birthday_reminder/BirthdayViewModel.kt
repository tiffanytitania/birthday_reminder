package com.example.birthday_reminder.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.birthday_reminder.data.repository.BirthdayRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BirthdayViewModel(
    private val repository: BirthdayRepository = BirthdayRepository()
) : ViewModel() {

    private val _birthdays = MutableStateFlow<List<BirthdayItem>>(emptyList())
    val birthdays: StateFlow<List<BirthdayItem>> = _birthdays.asStateFlow()

    private val _filteredBirthdays = MutableStateFlow<List<BirthdayItem>>(emptyList())
    val filteredBirthdays: StateFlow<List<BirthdayItem>> = _filteredBirthdays.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        loadBirthdays()
    }

    private fun loadBirthdays() {
        viewModelScope.launch {
            _isLoading.value = true
            Log.d("BirthdayViewModel", "Loading birthdays...")

            repository.getAllBirthdays().collect { result ->
                _isLoading.value = false
                result.onSuccess { list ->
                    Log.d("BirthdayViewModel", "Loaded ${list.size} birthdays")
                    _birthdays.value = list
                    _filteredBirthdays.value = list
                }.onFailure { exception ->
                    Log.e("BirthdayViewModel", "Error: ${exception.message}")
                    _error.value = exception.message
                }
            }
        }
    }

    fun addBirthday(name: String, date: String) {
        if (name.isEmpty() || date.isEmpty()) {
            _error.value = "Nama dan tanggal tidak boleh kosong"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.addBirthday(name, date)
            _isLoading.value = false

            result.onSuccess {
                _successMessage.value = "Berhasil ditambahkan"
                _error.value = null
                loadBirthdays()
            }.onFailure { exception ->
                _error.value = "Gagal: ${exception.message}"
                _successMessage.value = null
            }
        }
    }

    fun updateBirthday(key: String, name: String, date: String) {
        if (name.isEmpty() || date.isEmpty()) {
            _error.value = "Nama dan tanggal tidak boleh kosong"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.updateBirthday(key, name, date)
            _isLoading.value = false

            result.onSuccess {
                _successMessage.value = "Berhasil diupdate"
                _error.value = null
                loadBirthdays()
            }.onFailure { exception ->
                _error.value = "Gagal: ${exception.message}"
                _successMessage.value = null
            }
        }
    }

    fun deleteBirthday(key: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.deleteBirthday(key)
            _isLoading.value = false

            result.onSuccess {
                _successMessage.value = "Berhasil dihapus"
                _error.value = null
                loadBirthdays()
            }.onFailure { exception ->
                _error.value = "Gagal: ${exception.message}"
                _successMessage.value = null
            }
        }
    }

    fun filterBirthdaysByDate(day: Int, month: Int) {
        _filteredBirthdays.value = _birthdays.value.filter { birthday ->
            val parts = birthday.date.split("/")
            if (parts.size >= 2) {
                val bdayDay = parts[0].toIntOrNull() ?: 0
                val bdayMonth = parts[1].toIntOrNull() ?: 0
                (bdayDay == day && bdayMonth == month)
            } else {
                false
            }
        }
    }

    fun searchBirthdays(query: String) {
        _filteredBirthdays.value = _birthdays.value.filter {
            it.name.lowercase().contains(query.lowercase())
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }
}