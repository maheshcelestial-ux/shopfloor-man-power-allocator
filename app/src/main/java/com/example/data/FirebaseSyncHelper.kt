package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.DocumentChange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FirebaseSyncHelper(private val context: Context, private val db: AppDatabase) {

    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val _syncStatus = MutableStateFlow("Local-Only Mode")
    val syncStatus = _syncStatus.asStateFlow()

    private val _isCloudActive = MutableStateFlow(false)
    val isCloudActive = _isCloudActive.asStateFlow()

    private var firestore: FirebaseFirestore? = null
    private val listeners = mutableListOf<ListenerRegistration>()

    init {
        setupFirebase()
    }

    private fun setupFirebase() {
        try {
            // Attempt to initialize Firebase. If google-services.json is missing,
            // or Firebase can't init, it will safely fall back to local-only mode.
            val apps = FirebaseApp.getApps(context)
            if (apps.isNotEmpty() || FirebaseApp.initializeApp(context) != null) {
                firestore = FirebaseFirestore.getInstance()
                _isCloudActive.value = true
                _syncStatus.value = "Cloud Sync Enabled"
                Log.d("FirebaseSyncHelper", "Firebase Firestore successfully initialized.")
                startListeningForUpdates()
            } else {
                _isCloudActive.value = false
                _syncStatus.value = "Local-Only Mode"
            }
        } catch (e: Exception) {
            _isCloudActive.value = false
            _syncStatus.value = "Local fallback (No Config)"
            Log.w("FirebaseSyncHelper", "Firebase Firestore initialization bypassed: ${e.localizedMessage}")
        }
    }

    // --- Dynamic Listener: Real-time Data Sync from Firestore ---
    private fun startListeningForUpdates() {
        val fs = firestore ?: return

        // 1. Listen for Operators
        try {
            val opListener = fs.collection("operators")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e("FirebaseSyncHelper", "Operators listen failed", error)
                        return@addSnapshotListener
                    }
                    if (snapshots != null) {
                        scope.launch {
                            for (change in snapshots.documentChanges) {
                                val doc = change.document
                                val docId = doc.id
                                when (change.type) {
                                    DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                        val op = Operator(
                                            employeeId = doc.getString("employeeId") ?: docId,
                                            name = doc.getString("name") ?: "",
                                            department = doc.getString("department") ?: "",
                                            shift = doc.getString("shift") ?: "General",
                                            status = doc.getString("status") ?: "Active",
                                            skillLevel = (doc.getLong("skillLevel") ?: 0L).toInt(),
                                            experienceYears = doc.getDouble("experienceYears") ?: 0.0,
                                            isAvailable = doc.getBoolean("isAvailable") ?: true,
                                            skillsJson = doc.getString("skillsJson") ?: "{}"
                                        )
                                        db.operatorDao().insertOperator(op)
                                    }
                                    DocumentChange.Type.REMOVED -> {
                                        db.operatorDao().deleteOperatorById(docId)
                                    }
                                }
                            }
                            _syncStatus.value = "Synced with Cloud"
                        }
                    }
                }
            listeners.add(opListener)
        } catch (e: Exception) {
            Log.e("FirebaseSyncHelper", "Error starting operator listener", e)
        }

        // 2. Listen for Machines
        try {
            val macListener = fs.collection("machines")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshots != null) {
                        scope.launch {
                            for (change in snapshots.documentChanges) {
                                val doc = change.document
                                val docId = doc.id
                                when (change.type) {
                                    DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                        val mac = Machine(
                                            id = doc.getString("id") ?: docId,
                                            name = doc.getString("name") ?: "",
                                            department = doc.getString("department") ?: "",
                                            status = doc.getString("status") ?: "Running",
                                            priority = (doc.getLong("priority") ?: 1L).toInt()
                                        )
                                        db.machineDao().insertMachine(mac)
                                    }
                                    DocumentChange.Type.REMOVED -> {
                                        db.machineDao().deleteMachineById(docId)
                                    }
                                }
                            }
                        }
                    }
                }
            listeners.add(macListener)
        } catch (e: Exception) {
            Log.e("FirebaseSyncHelper", "Error starting machine listener", e)
        }

        // 3. Listen for Assembly Stations
        try {
            val stationListener = fs.collection("assembly_stations")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshots != null) {
                        scope.launch {
                            for (change in snapshots.documentChanges) {
                                val doc = change.document
                                val docId = doc.id
                                when (change.type) {
                                    DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                        val station = AssemblyStation(
                                            id = doc.getString("id") ?: docId,
                                            name = doc.getString("name") ?: "",
                                            department = doc.getString("department") ?: "",
                                            status = doc.getString("status") ?: "Running",
                                            priority = (doc.getLong("priority") ?: 1L).toInt()
                                        )
                                        db.assemblyStationDao().insertStation(station)
                                    }
                                    DocumentChange.Type.REMOVED -> {
                                        db.assemblyStationDao().deleteStationById(docId)
                                    }
                                }
                            }
                        }
                    }
                }
            listeners.add(stationListener)
        } catch (e: Exception) {
            Log.e("FirebaseSyncHelper", "Error starting station listener", e)
        }

        // 4. Listen for System Settings
        try {
            val settingsListener = fs.collection("settings")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshots != null) {
                        scope.launch {
                            snapshots.forEach { doc ->
                                db.settingDao().insertSetting(
                                    SystemSetting(key = doc.id, value = doc.getString("value") ?: "")
                                )
                            }
                        }
                    }
                }
            listeners.add(settingsListener)
        } catch (e: Exception) {
            Log.e("FirebaseSyncHelper", "Error starting settings listener", e)
        }
    }

    // --- Dynamic Upload: Push Room DB Changes to Firestore ---
    suspend fun uploadOperator(operator: Operator) = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext
        try {
            val data = hashMapOf(
                "employeeId" to operator.employeeId,
                "name" to operator.name,
                "department" to operator.department,
                "shift" to operator.shift,
                "status" to operator.status,
                "skillLevel" to operator.skillLevel,
                "experienceYears" to operator.experienceYears,
                "isAvailable" to operator.isAvailable,
                "skillsJson" to operator.skillsJson
            )
            fs.collection("operators").document(operator.employeeId).set(data)
            _syncStatus.value = "Updated Cloud"
        } catch (e: Exception) {
            Log.w("FirebaseSyncHelper", "Could not sync operator to Firestore: ${e.localizedMessage}")
        }
    }

    suspend fun uploadMachine(machine: Machine) = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext
        try {
            val data = hashMapOf(
                "id" to machine.id,
                "name" to machine.name,
                "department" to machine.department,
                "status" to machine.status,
                "priority" to machine.priority
            )
            fs.collection("machines").document(machine.id).set(data)
        } catch (e: Exception) {
            Log.w("FirebaseSyncHelper", "Could not sync machine to Firestore: ${e.localizedMessage}")
        }
    }

    suspend fun uploadStation(station: AssemblyStation) = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext
        try {
            val data = hashMapOf(
                "id" to station.id,
                "name" to station.name,
                "department" to station.department,
                "status" to station.status,
                "priority" to station.priority
            )
            fs.collection("assembly_stations").document(station.id).set(data)
        } catch (e: Exception) {
            Log.w("FirebaseSyncHelper", "Could not sync station to Firestore: ${e.localizedMessage}")
        }
    }

    suspend fun uploadAllocation(allocation: Allocation) = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext
        try {
            val data = hashMapOf(
                "date" to allocation.date,
                "shift" to allocation.shift,
                "targetId" to allocation.targetId,
                "targetName" to allocation.targetName,
                "isMachine" to allocation.isMachine,
                "allocatedOperatorId" to allocation.allocatedOperatorId,
                "allocatedOperatorName" to allocation.allocatedOperatorName,
                "skillLevel" to allocation.skillLevel,
                "status" to allocation.status,
                "remarks" to allocation.remarks
            )
            val docId = "${allocation.date}_${allocation.shift}_${allocation.targetId}"
            fs.collection("allocations").document(docId).set(data)
        } catch (e: Exception) {
            Log.w("FirebaseSyncHelper", "Could not sync allocation to Firestore: ${e.localizedMessage}")
        }
    }

    suspend fun deleteOperator(operator: Operator) = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext
        try {
            fs.collection("operators").document(operator.employeeId).delete()
        } catch (e: Exception) {
            Log.w("FirebaseSyncHelper", "Could not delete operator from Firestore: ${e.localizedMessage}")
        }
    }

    suspend fun deleteMachine(machine: Machine) = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext
        try {
            fs.collection("machines").document(machine.id).delete()
        } catch (e: Exception) {
            Log.w("FirebaseSyncHelper", "Could not delete machine from Firestore: ${e.localizedMessage}")
        }
    }

    suspend fun deleteStation(station: AssemblyStation) = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext
        try {
            fs.collection("assembly_stations").document(station.id).delete()
        } catch (e: Exception) {
            Log.w("FirebaseSyncHelper", "Could not delete station from Firestore: ${e.localizedMessage}")
        }
    }

    fun removeListeners() {
        listeners.forEach { it.remove() }
        listeners.clear()
    }
}
