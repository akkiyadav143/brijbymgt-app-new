package com.brijby77.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.brijby77.data.model.DashboardStats
import com.brijby77.data.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class DashboardViewModel(private val repository: StudentRepository) : ViewModel() {

    private val _selectedFilter = MutableStateFlow("ALL CL")
    val selectedFilter: StateFlow<String> = _selectedFilter

    val dashboardStats: StateFlow<DashboardStats> = _selectedFilter
        .flatMapLatest { filter ->
            repository.getDashboardStats(filter)
        }
        .let { flow ->
            val state = MutableStateFlow(DashboardStats())
            viewModelScope.launch {
                flow.collect { state.value = it }
            }
            state
        }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }
}

class DashboardViewModelFactory(private val repository: StudentRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

