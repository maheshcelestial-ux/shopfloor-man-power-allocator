package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AllocationViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = AllocationRepository(application, db)

    // Current Date and Shift State
    private val _selectedDate = MutableStateFlow(getCurrentDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _selectedShift = MutableStateFlow("A") // A, B, C, General
    val selectedShift: StateFlow<String> = _selectedShift.asStateFlow()

    // Current Navigation/Screen State
    private val _currentScreen = MutableStateFlow(Screen.Login)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Auth State
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // Database Flows
    val operators = repository.allOperators.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val machines = repository.allMachines.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val stations = repository.allStations.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val settings = repository.allSettings.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val users = repository.allUsers.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Attendance state for selected Date & Shift
    val attendanceList = combine(_selectedDate, _selectedShift) { date, shift ->
        Pair(date, shift)
    }.flatMapLatest { (date, shift) ->
        repository.getAttendanceForShift(date, shift)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Requirements state for selected Date & Shift
    val activeRequirement = combine(_selectedDate, _selectedShift) { date, shift ->
        Pair(date, shift)
    }.flatMapLatest { (date, shift) ->
        repository.getRequirement(date, shift)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Allocations state for selected Date & Shift
    val allocations = combine(_selectedDate, _selectedShift) { date, shift ->
        Pair(date, shift)
    }.flatMapLatest { (date, shift) ->
        repository.getAllocations(date, shift)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search query states for filtering lists
    val operatorSearchQuery = MutableStateFlow("")
    val machineSearchQuery = MutableStateFlow("")
    val stationSearchQuery = MutableStateFlow("")

    // Status state
    val syncStatus: StateFlow<String> = repository.syncHelper.syncStatus

    init {
        viewModelScope.launch {
            repository.seedInitialData()
        }
    }

    // Date & Shift Setters
    fun setDate(date: String) {
        _selectedDate.value = date
    }

    fun setShift(shift: String) {
        _selectedShift.value = shift
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    // --- Authentication ---
    fun login(emailOrName: String, pin: String) {
        viewModelScope.launch {
            _loginError.value = null
            // Check in seeded/local users
            val matchedUser = users.value.find { 
                (it.email.equals(emailOrName, true) || it.name.equals(emailOrName, true)) && it.pin == pin 
            }
            if (matchedUser != null) {
                _currentUser.value = matchedUser
                _currentScreen.value = Screen.Dashboard
            } else {
                _loginError.value = "Invalid Username or PIN"
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _currentScreen.value = Screen.Login
    }

    // --- Module 3: Attendance Management ---
    fun toggleAttendance(employeeId: String, isPresent: Boolean) {
        viewModelScope.launch {
            val date = _selectedDate.value
            val shift = _selectedShift.value
            val current = attendanceList.value.toMutableList()
            val existingIdx = current.indexOfFirst { it.employeeId == employeeId }

            val record = Attendance(
                date = date,
                shift = shift,
                employeeId = employeeId,
                isPresent = isPresent
            )

            if (existingIdx != -1) {
                current[existingIdx] = record
            } else {
                current.add(record)
            }
            repository.saveAttendance(current)
        }
    }

    fun markAllPresent() {
        viewModelScope.launch {
            val date = _selectedDate.value
            val shift = _selectedShift.value
            val ops = operators.value.filter { it.status == "Active" && it.shift == shift }
            val list = ops.map {
                Attendance(date = date, shift = shift, employeeId = it.employeeId, isPresent = true)
            }
            repository.saveAttendance(list)
        }
    }

    // --- Module 4: Machine Requirements ---
    fun toggleMachineRequirement(machineId: String, isRequired: Boolean) {
        viewModelScope.launch {
            val date = _selectedDate.value
            val shift = _selectedShift.value
            val req = activeRequirement.value ?: MachineRequirement(id = "${date}_${shift}", date = date, shift = shift)
            
            val list = Converters().fromStringList(req.requiredMachinesJson).toMutableList()
            if (isRequired) {
                if (!list.contains(machineId)) list.add(machineId)
            } else {
                list.remove(machineId)
            }

            val updated = req.copy(requiredMachinesJson = Converters().toStringList(list))
            repository.saveRequirement(updated)
        }
    }

    fun toggleStationRequirement(stationId: String, isRequired: Boolean) {
        viewModelScope.launch {
            val date = _selectedDate.value
            val shift = _selectedShift.value
            val req = activeRequirement.value ?: MachineRequirement(id = "${date}_${shift}", date = date, shift = shift)
            
            val list = Converters().fromStringList(req.requiredStationsJson).toMutableList()
            if (isRequired) {
                if (!list.contains(stationId)) list.add(stationId)
            } else {
                list.remove(stationId)
            }

            val updated = req.copy(requiredStationsJson = Converters().toStringList(list))
            repository.saveRequirement(updated)
        }
    }

    // --- Module 5: Auto Allocation Engine Trigger ---
    fun autoAllocateManpower() {
        viewModelScope.launch {
            val date = _selectedDate.value
            val shift = _selectedShift.value

            val req = activeRequirement.value ?: return@launch
            val requiredMachineIds = Converters().fromStringList(req.requiredMachinesJson)
            val requiredStationIds = Converters().fromStringList(req.requiredStationsJson)

            // Filter real machines and stations details
            val reqMachines = machines.value.filter { requiredMachineIds.contains(it.id) }
            val reqStations = stations.value.filter { requiredStationIds.contains(it.id) }

            // Check present operators
            // Get attendance list
            val attRecords = attendanceList.value
            val presentEmpIds = attRecords.filter { it.isPresent }.map { it.employeeId }
            
            // Filter actual operator objects marked present
            // If attendance wasn't marked, fallback to available operators for convenience
            val presentOps = if (attRecords.isEmpty()) {
                operators.value.filter { it.status == "Active" && it.isAvailable }
            } else {
                operators.value.filter { presentEmpIds.contains(it.employeeId) }
            }

            val resultAllocations = AllocationEngine.generateAllocation(
                date = date,
                shift = shift,
                requiredMachines = reqMachines,
                requiredStations = reqStations,
                presentOperators = presentOps
            )

            repository.saveAllocations(resultAllocations)
        }
    }

    fun clearAllocations() {
        viewModelScope.launch {
            repository.clearAllocations(_selectedDate.value, _selectedShift.value)
        }
    }

    // --- Master Data Updates (Admin settings Module 9) ---
    fun saveOperator(operator: Operator) {
        viewModelScope.launch {
            repository.insertOperator(operator)
        }
    }

    fun deleteOperator(operator: Operator) {
        viewModelScope.launch {
            repository.deleteOperator(operator)
        }
    }

    fun saveMachine(machine: Machine) {
        viewModelScope.launch {
            repository.insertMachine(machine)
        }
    }

    fun deleteMachine(machine: Machine) {
        viewModelScope.launch {
            repository.deleteMachine(machine)
        }
    }

    fun saveStation(station: AssemblyStation) {
        viewModelScope.launch {
            repository.insertStation(station)
        }
    }

    fun deleteStation(station: AssemblyStation) {
        viewModelScope.launch {
            repository.deleteStation(station)
        }
    }

    // Helpers
    private fun getCurrentDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }
}

// Simple enum screen navigation
enum class Screen {
    Login,
    Dashboard,
    SkillMatrix,
    Attendance,
    Requirements,
    Allocations,
    Settings,
    Reports
}
