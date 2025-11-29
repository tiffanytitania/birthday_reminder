package com.example.birthday_reminder.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.birthday_reminder.BirthdayItem
import com.example.birthday_reminder.data.repository.BirthdayRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel untuk BirthdayFragment
 * Handle UI state dan business logic
 */
class BirthdayViewModel(
    private val repository: BirthdayRepository = BirthdayRepository()
) : ViewModel() {

    // State untuk list birthday
    private val _birthdays = MutableStateFlow<List<BirthdayItem>>(emptyList())
    val birthdays: StateFlow<List<BirthdayItem>> = _birthdays.asStateFlow()

    // State untuk filtered list (search)
    private val _filteredBirthdays = MutableStateFlow<List<BirthdayItem>>(emptyList())
    val filteredBirthdays: StateFlow<List<BirthdayItem>> = _filteredBirthdays.asStateFlow()

    // State untuk loading
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // State untuk error
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // State untuk success message
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        loadBirthdays()
    }

    /**
     * Load all birthdays from repository
     */
    private fun loadBirthdays() {
        viewModelScope.launch {
            _isLoading.value = true
            Log.d("BirthdayViewModel", "🔄 LOADING BIRTHDAYS STARTED")

            repository.getAllBirthdays().collect { result ->
                _isLoading.value = false
                result.onSuccess { list ->
                    Log.d("BirthdayViewModel", "✅ SUCCESS: Loaded ${list.size} birthdays")
                    _birthdays.value = list
                }.onFailure { exception ->
                    Log.e("BirthdayViewModel", "❌ ERROR: ${exception.message}")
                }
            }
        }
    }

    /**
     * Add new birthday
     */
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
                _successMessage.value = "✅ Berhasil ditambahkan"
                _error.value = null
            }.onFailure { exception ->
                _error.value = "❌ Gagal: ${exception.message}"
                _successMessage.value = null
            }
        }
    }

    /**
     * Update existing birthday
     */
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
                _successMessage.value = "✅ Berhasil diupdate"
                _error.value = null
            }.onFailure { exception ->
                _error.value = "❌ Gagal: ${exception.message}"
                _successMessage.value = null
            }
        }
    }

    /**
     * Delete birthday
     */
    fun deleteBirthday(key: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.deleteBirthday(key)
            _isLoading.value = false

            result.onSuccess {
                _successMessage.value = "✅ Berhasil dihapus"
                _error.value = null
            }.onFailure { exception ->
                _error.value = "❌ Gagal menghapus: ${exception.message}"
                _successMessage.value = null
            }
        }
    }

    /**
     * Search/filter birthdays
     */
    fun searchBirthdays(query: String) {
        _filteredBirthdays.value = repository.searchBirthdays(query, _birthdays.value)
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
}