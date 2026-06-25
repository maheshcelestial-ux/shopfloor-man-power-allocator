package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): User?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)
}

@Dao
interface OperatorDao {
    @Query("SELECT * FROM operators ORDER BY name ASC")
    fun getAllOperators(): Flow<List<Operator>>

    @Query("SELECT * FROM operators WHERE employeeId = :id LIMIT 1")
    suspend fun getOperatorById(id: String): Operator?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperator(operator: Operator)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperators(operators: List<Operator>)

    @Delete
    suspend fun deleteOperator(operator: Operator)

    @Query("DELETE FROM operators WHERE employeeId = :id")
    suspend fun deleteOperatorById(id: String)

    @Query("DELETE FROM operators")
    suspend fun deleteAllOperators()
}

@Dao
interface MachineDao {
    @Query("SELECT * FROM machines ORDER BY priority DESC, name ASC")
    fun getAllMachines(): Flow<List<Machine>>

    @Query("SELECT * FROM machines WHERE id = :id LIMIT 1")
    suspend fun getMachineById(id: String): Machine?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMachine(machine: Machine)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMachines(machines: List<Machine>)

    @Delete
    suspend fun deleteMachine(machine: Machine)

    @Query("DELETE FROM machines WHERE id = :id")
    suspend fun deleteMachineById(id: String)
}

@Dao
interface AssemblyStationDao {
    @Query("SELECT * FROM assembly_stations ORDER BY priority DESC, name ASC")
    fun getAllStations(): Flow<List<AssemblyStation>>

    @Query("SELECT * FROM assembly_stations WHERE id = :id LIMIT 1")
    suspend fun getStationById(id: String): AssemblyStation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStation(station: AssemblyStation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStations(stations: List<AssemblyStation>)

    @Delete
    suspend fun deleteStation(station: AssemblyStation)

    @Query("DELETE FROM assembly_stations WHERE id = :id")
    suspend fun deleteStationById(id: String)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE date = :date AND shift = :shift")
    fun getAttendanceForShift(date: String, shift: String): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE date = :date AND shift = :shift")
    suspend fun getAttendanceForShiftList(date: String, shift: String): List<Attendance>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: List<Attendance>)

    @Query("DELETE FROM attendance WHERE date = :date AND shift = :shift")
    suspend fun deleteAttendanceForShift(date: String, shift: String)
}

@Dao
interface RequirementDao {
    @Query("SELECT * FROM requirements WHERE date = :date AND shift = :shift LIMIT 1")
    fun getRequirement(date: String, shift: String): Flow<MachineRequirement?>

    @Query("SELECT * FROM requirements WHERE date = :date AND shift = :shift LIMIT 1")
    suspend fun getRequirementSync(date: String, shift: String): MachineRequirement?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequirement(requirement: MachineRequirement)
}

@Dao
interface AllocationDao {
    @Query("SELECT * FROM allocations WHERE date = :date AND shift = :shift")
    fun getAllocations(date: String, shift: String): Flow<List<Allocation>>

    @Query("SELECT * FROM allocations WHERE date = :date AND shift = :shift")
    suspend fun getAllocationsSync(date: String, shift: String): List<Allocation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllocations(allocations: List<Allocation>)

    @Query("DELETE FROM allocations WHERE date = :date AND shift = :shift")
    suspend fun deleteAllocations(date: String, shift: String)
}

@Dao
interface SettingDao {
    @Query("SELECT * FROM system_settings")
    fun getAllSettings(): Flow<List<SystemSetting>>

    @Query("SELECT * FROM system_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): SystemSetting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: SystemSetting)
}
