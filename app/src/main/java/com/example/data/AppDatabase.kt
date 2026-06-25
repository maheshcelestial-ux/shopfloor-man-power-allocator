package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        User::class,
        Operator::class,
        Machine::class,
        AssemblyStation::class,
        Attendance::class,
        MachineRequirement::class,
        Allocation::class,
        SystemSetting::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun operatorDao(): OperatorDao
    abstract fun machineDao(): MachineDao
    abstract fun assemblyStationDao(): AssemblyStationDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun requirementDao(): RequirementDao
    abstract fun allocationDao(): AllocationDao
    abstract fun settingDao(): SettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shopfloor_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
