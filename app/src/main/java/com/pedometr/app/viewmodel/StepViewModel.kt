package com.pedometr.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedometr.app.data.StepEntry
import com.pedometr.app.data.StepRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class StepUiState(
    val todaySteps: Int = 0,
    val stepHistory: List<StepEntry> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * ViewModel dla ekranu głównego
 * 
 * Używa Dependency Injection (Koin) do wstrzykiwania StepRepository
 * zamiast AndroidViewModel i bezpośredniego dostępu do Application context
 */
class StepViewModel(
    private val repository: StepRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(StepUiState())
    val uiState: StateFlow<StepUiState> = _uiState.asStateFlow()
    
    init {
        loadStepData()
    }
    
    private fun loadStepData() {
        viewModelScope.launch {
            try {
                // Obserwuj dane z ostatnich 3 miesięcy
                repository.getStepsForLast3Months()
                    .stateIn(
                        scope = viewModelScope,
                        started = SharingStarted.WhileSubscribed(5000),
                        initialValue = emptyList()
                    )
                    .collect { stepHistory ->
                        val today = LocalDate.now().toString()
                        val todayEntry = stepHistory.find { it.date == today }
                        
                        _uiState.value = StepUiState(
                            todaySteps = todayEntry?.steps ?: 0,
                            stepHistory = stepHistory,
                            isLoading = false
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = StepUiState(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    fun refreshData() {
        loadStepData()
    }
    
    fun getStepsForDate(date: LocalDate): Int {
        return _uiState.value.stepHistory
            .find { it.date == date.toString() }
            ?.steps ?: 0
    }
    
    fun getAverageStepsPerDay(): Int {
        val history = _uiState.value.stepHistory
        return if (history.isNotEmpty()) {
            history.sumOf { it.steps } / history.size
        } else {
            0
        }
    }
    
    fun getTotalSteps(): Int {
        return _uiState.value.stepHistory.sumOf { it.steps }
    }
}
