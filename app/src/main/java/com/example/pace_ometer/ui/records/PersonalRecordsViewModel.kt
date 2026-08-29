package com.example.pace_ometer.ui.records

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pace_ometer.PaceometerApp
import com.example.pace_ometer.data.ActivityType
import com.example.pace_ometer.data.db.entity.PersonalRecordEntity
import com.example.pace_ometer.data.db.entity.PersonalRecordScope
import com.example.pace_ometer.data.db.entity.SeasonEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class PersonalRecordsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PaceometerApp

    val seasons: StateFlow<List<SeasonEntity>> = app.seasonRepository.observeSeasons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedScope = MutableStateFlow(PersonalRecordScope.ALL_TIME)
    val selectedScope: StateFlow<String> = _selectedScope

    private val _selectedActivityType = MutableStateFlow(ActivityType.RUNNING)
    val selectedActivityType: StateFlow<ActivityType> = _selectedActivityType

    val records: StateFlow<List<PersonalRecordEntity>> = combine(_selectedScope, _selectedActivityType) { scope, type ->
        scope to type
    }.flatMapLatest { (scope, type) -> app.personalRecordRepository.observeForScope(scope, type.name) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectScope(scope: String) {
        _selectedScope.value = scope
    }

    fun selectActivityType(activityType: ActivityType) {
        _selectedActivityType.value = activityType
    }
}
