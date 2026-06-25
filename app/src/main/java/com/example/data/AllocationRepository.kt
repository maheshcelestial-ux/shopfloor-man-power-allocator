package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AllocationRepository(
    private val context: Context,
    private val db: AppDatabase
) {
    val syncHelper = FirebaseSyncHelper(context, db)

    // User Operations
    val allUsers: Flow<List<User>> = db.userDao().getAllUsers()
    suspend fun insertUser(user: User) = db.userDao().insertUser(user)
    suspend fun deleteUser(user: User) = db.userDao().deleteUser(user)
    suspend fun getUserById(id: String) = db.userDao().getUserById(id)

    // Operator Operations
    val allOperators: Flow<List<Operator>> = db.operatorDao().getAllOperators()
    suspend fun insertOperator(operator: Operator) {
        db.operatorDao().insertOperator(operator)
        syncHelper.uploadOperator(operator)
    }
    suspend fun deleteOperator(operator: Operator) {
        db.operatorDao().deleteOperator(operator)
        syncHelper.deleteOperator(operator)
    }

    // Machine Operations
    val allMachines: Flow<List<Machine>> = db.machineDao().getAllMachines()
    suspend fun insertMachine(machine: Machine) {
        db.machineDao().insertMachine(machine)
        syncHelper.uploadMachine(machine)
    }
    suspend fun deleteMachine(machine: Machine) {
        db.machineDao().deleteMachine(machine)
        syncHelper.deleteMachine(machine)
    }

    // Assembly Station Operations
    val allStations: Flow<List<AssemblyStation>> = db.assemblyStationDao().getAllStations()
    suspend fun insertStation(station: AssemblyStation) {
        db.assemblyStationDao().insertStation(station)
        syncHelper.uploadStation(station)
    }
    suspend fun deleteStation(station: AssemblyStation) {
        db.assemblyStationDao().deleteStation(station)
        syncHelper.deleteStation(station)
    }

    // Attendance Operations
    fun getAttendanceForShift(date: String, shift: String): Flow<List<Attendance>> =
        db.attendanceDao().getAttendanceForShift(date, shift)

    suspend fun getAttendanceForShiftList(date: String, shift: String): List<Attendance> =
        db.attendanceDao().getAttendanceForShiftList(date, shift)

    suspend fun saveAttendance(attendanceList: List<Attendance>) = withContext(Dispatchers.IO) {
        if (attendanceList.isNotEmpty()) {
            val sample = attendanceList.first()
            db.attendanceDao().deleteAttendanceForShift(sample.date, sample.shift)
            db.attendanceDao().insertAttendance(attendanceList)
        }
    }

    // Requirements Operations
    fun getRequirement(date: String, shift: String): Flow<MachineRequirement?> =
        db.requirementDao().getRequirement(date, shift)

    suspend fun getRequirementSync(date: String, shift: String): MachineRequirement? =
        db.requirementDao().getRequirementSync(date, shift)

    suspend fun saveRequirement(requirement: MachineRequirement) =
        db.requirementDao().insertRequirement(requirement)

    // Allocations Operations
    fun getAllocations(date: String, shift: String): Flow<List<Allocation>> =
        db.allocationDao().getAllocations(date, shift)

    suspend fun getAllocationsSync(date: String, shift: String): List<Allocation> =
        db.allocationDao().getAllocationsSync(date, shift)

    suspend fun saveAllocations(allocations: List<Allocation>) = withContext(Dispatchers.IO) {
        if (allocations.isNotEmpty()) {
            val sample = allocations.first()
            db.allocationDao().deleteAllocations(sample.date, sample.shift)
            db.allocationDao().insertAllocations(allocations)
            // Sync to Firebase in background
            allocations.forEach { alloc ->
                syncHelper.uploadAllocation(alloc)
            }
        }
    }

    suspend fun clearAllocations(date: String, shift: String) =
        db.allocationDao().deleteAllocations(date, shift)

    // Settings
    val allSettings: Flow<List<SystemSetting>> = db.settingDao().getAllSettings()
    suspend fun insertSetting(setting: SystemSetting) = db.settingDao().insertSetting(setting)

    // Seed Initial Data if empty
    suspend fun seedInitialData() = withContext(Dispatchers.IO) {
        // 1. Seed Users (Roles)
        val existingUsers = db.userDao().getAllUsers()
        var hasUsers = false
        // Quick sync block to inspect database
        val userCheck = db.userDao().getUserById("admin")
        if (userCheck == null) {
            db.userDao().insertUser(User("admin", "admin@factory.com", "System Admin", "Admin", "1111"))
            db.userDao().insertUser(User("supervisor", "supervisor@factory.com", "Shop Supervisor", "Supervisor", "2222"))
            db.userDao().insertUser(User("manager", "manager@factory.com", "Production Manager", "Production Manager", "3333"))
            db.userDao().insertUser(User("viewer", "viewer@factory.com", "Reporting Viewer", "Viewer", "4444"))
        }

        // 2. Seed Machines
        val mList = db.machineDao().getMachineById("mac_cnc1")
        if (mList == null) {
            val machines = listOf(
                Machine("mac_cnc1", "CNC 1", "Machining", "Running", 5),
                Machine("mac_cnc2", "CNC 2", "Machining", "Running", 4),
                Machine("mac_cnc3", "CNC 3", "Machining", "Running", 3),
                Machine("mac_vmc1", "VMC 1", "Machining", "Running", 4),
                Machine("mac_vmc2", "VMC 2", "Machining", "Idle", 2),
                Machine("mac_hmc1", "HMC 1", "Machining", "Maintenance", 1),
                Machine("mac_grind", "Grinding Machine", "Machining", "Running", 2),
                Machine("mac_drill", "Drilling Machine", "Machining", "Running", 1),
                Machine("mac_turn", "Turning Machine", "Machining", "Running", 2),
                Machine("mac_mill", "Milling Machine", "Machining", "Running", 1)
            )
            db.machineDao().insertMachines(machines)
        }

        // 3. Seed Assembly Stations
        val sList = db.assemblyStationDao().getStationById("sta_line_a")
        if (sList == null) {
            val stations = listOf(
                AssemblyStation("sta_line_a", "Assembly Line A", "Assembly", "Running", 5),
                AssemblyStation("sta_line_b", "Assembly Line B", "Assembly", "Running", 4),
                AssemblyStation("sta_line_c", "Assembly Line C", "Assembly", "Idle", 3),
                AssemblyStation("sta_pump", "Pump Assembly", "Assembly", "Running", 4),
                AssemblyStation("sta_test", "Testing Station", "Assembly", "Running", 5),
                AssemblyStation("sta_leak", "Leak Test Station", "Assembly", "Running", 3),
                AssemblyStation("sta_pack", "Packing Line", "Assembly", "Running", 2)
            )
            db.assemblyStationDao().insertStations(stations)
        }

        // 4. Seed Operators with professional skill matrices
        val oCheck = db.operatorDao().getOperatorById("emp_101")
        if (oCheck == null) {
            val operators = listOf(
                Operator(
                    employeeId = "emp_101",
                    name = "Mahesh Patil",
                    department = "Machining",
                    shift = "A",
                    status = "Active",
                    skillLevel = 5,
                    experienceYears = 8.5,
                    isAvailable = true,
                    skillsJson = """{"CNC 1":5,"CNC 2":5,"VMC 1":4,"Grinding Machine":2,"Milling Machine":1}"""
                ),
                Operator(
                    employeeId = "emp_102",
                    name = "Rahul Sharma",
                    department = "Machining",
                    shift = "A",
                    status = "Active",
                    skillLevel = 4,
                    experienceYears = 5.0,
                    isAvailable = true,
                    skillsJson = """{"CNC 1":4,"CNC 2":4,"VMC 1":4,"VMC 2":3,"Milling Machine":2}"""
                ),
                Operator(
                    employeeId = "emp_103",
                    name = "Amit Kumar",
                    department = "Assembly",
                    shift = "A",
                    status = "Active",
                    skillLevel = 5,
                    experienceYears = 7.0,
                    isAvailable = true,
                    skillsJson = """{"Assembly Line A":5,"Assembly Line B":5,"Pump Assembly":4,"Testing Station":3}"""
                ),
                Operator(
                    employeeId = "emp_104",
                    name = "Suresh Naidu",
                    department = "Assembly",
                    shift = "A",
                    status = "Active",
                    skillLevel = 3,
                    experienceYears = 3.2,
                    isAvailable = true,
                    skillsJson = """{"Assembly Line B":3,"Testing Station":3,"Leak Test Station":4,"Packing Line":4}"""
                ),
                Operator(
                    employeeId = "emp_105",
                    name = "Vikram Singh",
                    department = "Machining",
                    shift = "B",
                    status = "Active",
                    skillLevel = 4,
                    experienceYears = 6.0,
                    isAvailable = true,
                    skillsJson = """{"CNC 2":5,"CNC 3":4,"VMC 1":3,"Turning Machine":4}"""
                ),
                Operator(
                    employeeId = "emp_106",
                    name = "Sanjay Dutt",
                    department = "Machining",
                    shift = "B",
                    status = "Active",
                    skillLevel = 2,
                    experienceYears = 1.5,
                    isAvailable = true,
                    skillsJson = """{"CNC 1":2,"Drilling Machine":4,"Milling Machine":3}"""
                ),
                Operator(
                    employeeId = "emp_107",
                    name = "Anil Kapoor",
                    department = "Assembly",
                    shift = "B",
                    status = "Active",
                    skillLevel = 4,
                    experienceYears = 4.8,
                    isAvailable = true,
                    skillsJson = """{"Assembly Line A":4,"Assembly Line C":4,"Packing Line":5}"""
                ),
                Operator(
                    employeeId = "emp_108",
                    name = "Priya Patel",
                    department = "Assembly",
                    shift = "B",
                    status = "Active",
                    skillLevel = 5,
                    experienceYears = 9.0,
                    isAvailable = true,
                    skillsJson = """{"Testing Station":5,"Leak Test Station":5,"Pump Assembly":5}"""
                )
            )
            db.operatorDao().insertOperators(operators)
        }
    }
}
