package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val email: String,
    val name: String,
    val role: String, // Admin, Production Manager, Supervisor, Viewer
    val pin: String = "1234" // Simplified login pin for shop floor convenience
)

@Entity(tableName = "operators")
data class Operator(
    @PrimaryKey val employeeId: String,
    val name: String,
    val department: String,
    val shift: String, // A, B, C, General
    val status: String, // Active, On Leave, Resigned
    val skillLevel: Int, // Default/Average skill level
    val experienceYears: Double,
    val isAvailable: Boolean = true,
    val skillsJson: String = "{}" // JSON map: skillName -> level (0 to 5)
)

@Entity(tableName = "machines")
data class Machine(
    @PrimaryKey val id: String,
    val name: String,
    val department: String,
    val status: String, // Running, Maintenance, Idle
    val priority: Int = 1 // Higher is higher priority
)

@Entity(tableName = "assembly_stations")
data class AssemblyStation(
    @PrimaryKey val id: String,
    val name: String,
    val department: String,
    val status: String, // Running, Setup, Idle
    val priority: Int = 1
)

@Entity(tableName = "attendance")
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val shift: String, // A, B, C, General
    val employeeId: String,
    val isPresent: Boolean
)

@Entity(tableName = "requirements")
data class MachineRequirement(
    @PrimaryKey val id: String, // date_shift
    val date: String,
    val shift: String,
    val requiredMachinesJson: String = "[]", // List of Machine IDs
    val requiredStationsJson: String = "[]"  // List of Assembly Station IDs
)

@Entity(tableName = "allocations")
data class Allocation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val shift: String,
    val targetId: String, // Machine or Station ID
    val targetName: String, // CNC 1, Assembly A, etc.
    val isMachine: Boolean,
    val allocatedOperatorId: String?, // Null if not allocated (Skill Gap)
    val allocatedOperatorName: String?,
    val skillLevel: Int, // Allocated person's level (0-5)
    val remarks: String = "",
    val status: String = "Allocated" // Allocated, Skill Gap, Shortage
)

@Entity(tableName = "system_settings")
data class SystemSetting(
    @PrimaryKey val key: String,
    val value: String
)

// Type Converters for JSON fields
class Converters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @TypeConverter
    fun fromStringMap(value: String?): Map<String, Int> {
        if (value.isNullOrEmpty()) return emptyMap()
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Integer::class.java)
        val adapter = moshi.adapter<Map<String, Int>>(type)
        return adapter.fromJson(value) ?: emptyMap()
    }

    @TypeConverter
    fun toStringMap(map: Map<String, Int>?): String {
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Integer::class.java)
        val adapter = moshi.adapter<Map<String, Int>>(type)
        return adapter.toJson(map ?: emptyMap())
    }

    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<List<String>>(type)
        return adapter.fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String {
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<List<String>>(type)
        return adapter.toJson(list ?: emptyList())
    }
}
