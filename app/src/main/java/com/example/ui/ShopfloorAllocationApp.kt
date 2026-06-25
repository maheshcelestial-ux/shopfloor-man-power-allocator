package com.example.ui

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopfloorAllocationApp(viewModel: AllocationViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedShift by viewModel.selectedShift.collectAsStateWithLifecycle()

    val context = LocalContext.current

    if (currentScreen == Screen.Login) {
        LoginScreen(viewModel = viewModel)
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Shopfloor MES",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(
                                            if (syncStatus.contains("Cloud")) Color(0xFF10B981)
                                            else Color(0xFFF59E0B)
                                        )
                                )
                                Text(
                                    syncStatus,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    actions = {
                        // Quick Date Selector Button
                        IconButton(onClick = {
                            showDatePicker(context) { dateStr ->
                                viewModel.setDate(dateStr)
                            }
                        }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                        }
                        Text(
                            text = selectedDate,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(end = 8.dp)
                        )

                        // Quick Shift Selector Trigger
                        var showShiftDropdown by remember { mutableStateOf(false) }
                        TextButton(
                            onClick = { showShiftDropdown = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary)
                        ) {
                            Text("Shift $selectedShift", fontWeight = FontWeight.Black)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Shift")
                        }
                        DropdownMenu(
                            expanded = showShiftDropdown,
                            onDismissRequest = { showShiftDropdown = false }
                        ) {
                            listOf("A", "B", "C", "General").forEach { sh ->
                                DropdownMenuItem(
                                    text = { Text("Shift $sh") },
                                    onClick = {
                                        viewModel.setShift(sh)
                                        showShiftDropdown = false
                                    }
                                )
                            }
                        }

                        // Logout Icon
                        IconButton(onClick = { viewModel.logout() }) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    val userRole = currentUser?.role ?: "Viewer"

                    // Dashboard (All roles)
                    NavigationBarItem(
                        selected = currentScreen == Screen.Dashboard,
                        onClick = { viewModel.navigateTo(Screen.Dashboard) },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Dashboard", fontSize = 11.sp) }
                    )

                    // Attendance (Admin, Production Manager, Supervisor)
                    if (userRole != "Viewer") {
                        NavigationBarItem(
                            selected = currentScreen == Screen.Attendance,
                            onClick = { viewModel.navigateTo(Screen.Attendance) },
                            icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Attendance") },
                            label = { Text("Attendance", fontSize = 11.sp) }
                        )
                    }

                    // Requirements (Admin, Production Manager, Supervisor)
                    if (userRole != "Viewer") {
                        NavigationBarItem(
                            selected = currentScreen == Screen.Requirements,
                            onClick = { viewModel.navigateTo(Screen.Requirements) },
                            icon = { Icon(Icons.Default.Build, contentDescription = "Requirements") },
                            label = { Text("Required", fontSize = 11.sp) }
                        )
                    }

                    // Allocations (All roles)
                    NavigationBarItem(
                        selected = currentScreen == Screen.Allocations,
                        onClick = { viewModel.navigateTo(Screen.Allocations) },
                        icon = { Icon(Icons.Default.Assignment, contentDescription = "Allocation") },
                        label = { Text("Allocation", fontSize = 11.sp) }
                    )

                    // Skill Matrix (Admin, Production Manager, Supervisor)
                    if (userRole != "Viewer") {
                        NavigationBarItem(
                            selected = currentScreen == Screen.SkillMatrix,
                            onClick = { viewModel.navigateTo(Screen.SkillMatrix) },
                            icon = { Icon(Icons.Default.People, contentDescription = "Skills") },
                            label = { Text("Skills", fontSize = 11.sp) }
                        )
                    }

                    // Settings (Admin)
                    if (userRole == "Admin") {
                        NavigationBarItem(
                            selected = currentScreen == Screen.Settings,
                            onClick = { viewModel.navigateTo(Screen.Settings) },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings", fontSize = 11.sp) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (currentScreen) {
                    Screen.Dashboard -> DashboardScreen(viewModel = viewModel)
                    Screen.Attendance -> AttendanceScreen(viewModel = viewModel)
                    Screen.Requirements -> RequirementsScreen(viewModel = viewModel)
                    Screen.Allocations -> AllocationsScreen(viewModel = viewModel)
                    Screen.SkillMatrix -> SkillMatrixScreen(viewModel = viewModel)
                    Screen.Settings -> SettingsScreen(viewModel = viewModel)
                    else -> DashboardScreen(viewModel = viewModel)
                }
            }
        }
    }
}

// --- MODULE 1: LOGIN SYSTEM ---
@Composable
fun LoginScreen(viewModel: AllocationViewModel) {
    val users by viewModel.users.collectAsStateWithLifecycle()
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()

    var selectedUser by remember { mutableStateOf<User?>(null) }
    var pinText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Lock",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )

                Text(
                    "Shopfloor MES Login",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Text(
                    "Select your Profile and enter PIN for fast access:",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                // Select Profile dropdown simulation
                var expandedUsers by remember { mutableStateOf(false) }
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedUsers = true }
                        .testTag("select_profile"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedUser?.name ?: "Tap to Choose Profile...",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedUser != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                    }
                }

                DropdownMenu(
                    expanded = expandedUsers,
                    onDismissRequest = { expandedUsers = false },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    users.forEach { user ->
                        DropdownMenuItem(
                            text = { Text("${user.name} (${user.role})") },
                            onClick = {
                                selectedUser = user
                                expandedUsers = false
                            }
                        )
                    }
                }

                // PIN Entry
                OutlinedTextField(
                    value = pinText,
                    onValueChange = { if (it.length <= 4) pinText = it },
                    label = { Text("Enter 4-Digit Login PIN") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_input"),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                if (loginError != null) {
                    Text(
                        loginError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = {
                        selectedUser?.let {
                            viewModel.login(it.email, pinText)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("login_button"),
                    shape = RoundedCornerShape(8.dp),
                    enabled = selectedUser != null && pinText.isNotEmpty()
                ) {
                    Text("ACCESS shopfloor", fontWeight = FontWeight.Bold)
                }

                // Friendly Tip
                Text(
                    "Default seeded PINs:\nAdmin: 1111 | Supervisor: 2222 | Manager: 3333 | Viewer: 4444",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// --- MODULE 8: DASHBOARD ---
@Composable
fun DashboardScreen(viewModel: AllocationViewModel) {
    val operators by viewModel.operators.collectAsStateWithLifecycle()
    val machines by viewModel.machines.collectAsStateWithLifecycle()
    val stations by viewModel.stations.collectAsStateWithLifecycle()
    val attendance by viewModel.attendanceList.collectAsStateWithLifecycle()
    val allocations by viewModel.allocations.collectAsStateWithLifecycle()

    val totalOps = operators.size
    val presentOps = attendance.count { it.isPresent }
    val absentOps = attendance.count { !it.isPresent }

    val runningMachines = machines.count { it.status == "Running" }
    val runningStations = stations.count { it.status == "Running" }

    // Computations
    val totalReq = allocations.size
    val totalAllocated = allocations.count { it.allocatedOperatorId != null }
    val shortage = allocations.count { it.status == "Shortage" }
    val skillGap = allocations.count { it.status == "Skill Gap" }

    val allocationPct = if (totalReq > 0) (totalAllocated * 100) / totalReq else 0
    val skillCoveragePct = if (totalAllocated > 0) {
        val totalSkill = allocations.filter { it.allocatedOperatorId != null }.sumOf { it.skillLevel }
        (totalSkill * 100) / (totalAllocated * 5)
    } else 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Text(
                "Live Production Summary",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Stats Cards Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardCard(
                    title = "Operators Present",
                    value = "$presentOps / $totalOps",
                    icon = Icons.Default.People,
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                DashboardCard(
                    title = "Active Stations",
                    value = "${runningMachines + runningStations}",
                    icon = Icons.Default.Build,
                    color = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Charts & Coverage Progress
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = CardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Allocation Statistics",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Progress 1: Allocation Fulfillment %
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Allocation Fulfillment", fontSize = 13.sp)
                            Text("$allocationPct%", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        LinearProgressIndicator(
                            progress = { allocationPct / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = if (allocationPct > 80) Color(0xFF10B981) else Color(0xFFEAB308)
                        )
                    }

                    // Progress 2: Average Skill Coverage
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Skill Adequacy Index", fontSize = 13.sp)
                            Text("$skillCoveragePct%", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        LinearProgressIndicator(
                            progress = { skillCoveragePct / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF6366F1)
                        )
                    }

                    // Quick Counters Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        QuickMetric(label = "Allocated", value = "$totalAllocated")
                        QuickMetric(label = "Skill Gaps", value = "$skillGap", highlightColor = Color(0xFFEAB308))
                        QuickMetric(label = "Shortages", value = "$shortage", highlightColor = Color(0xFFEF4444))
                    }
                }
            }
        }

        // Live Shopfloor Status Board
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = CardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Live Alerts & Allocation Actions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (totalReq == 0) {
                        Text(
                            "No current requirements entered for selected date & shift.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    } else {
                        allocations.forEach { alloc ->
                            if (alloc.status != "Allocated") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (alloc.status == "Shortage") Color(0xFFFEF2F2)
                                            else Color(0xFFFFFBEB),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = "Warning",
                                            tint = if (alloc.status == "Shortage") Color(0xFFEF4444) else Color(0xFFF59E0B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            "${alloc.targetName} has a ${alloc.status}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        alloc.remarks,
                                        fontSize = 11.sp,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { viewModel.navigateTo(Screen.Allocations) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("View Allocation Grid", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- MODULE 3: ATTENDANCE SCREEN ---
@Composable
fun AttendanceScreen(viewModel: AllocationViewModel) {
    val operators by viewModel.operators.collectAsStateWithLifecycle()
    val attendance by viewModel.attendanceList.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedShift by viewModel.selectedShift.collectAsStateWithLifecycle()

    var operatorToEdit by remember { mutableStateOf<Operator?>(null) }

    val activeOps = operators.filter { it.status == "Active" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Attendance Entry",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Mark Present / Absent (Target: Under 1 Min)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Button(
                    onClick = { viewModel.markAllPresent() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("All Present", fontWeight = FontWeight.Bold)
                }
            }
        }

        items(activeOps) { op ->
            val attRecord = attendance.find { it.employeeId == op.employeeId }
            val isPresent = attRecord?.isPresent ?: true // Defaults to present

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(8.dp),
                border = CardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(op.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("ID: ${op.employeeId} | Dept: ${op.department}", fontSize = 12.sp, color = Color.Gray)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { operatorToEdit = op }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Operator", tint = MaterialTheme.colorScheme.secondary)
                        }

                        FilterChip(
                            selected = isPresent,
                            onClick = { viewModel.toggleAttendance(op.employeeId, true) },
                            label = { Text("Present") },
                            leadingIcon = { if (isPresent) Icon(Icons.Default.Check, contentDescription = "Present") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFD1FAE5),
                                selectedLabelColor = Color(0xFF065F46)
                            )
                        )
                        FilterChip(
                            selected = !isPresent,
                            onClick = { viewModel.toggleAttendance(op.employeeId, false) },
                            label = { Text("Absent") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFEE2E2),
                                selectedLabelColor = Color(0xFF991B1B)
                            )
                        )
                    }
                }
            }
        }
    }

    if (operatorToEdit != null) {
        AddEditOperatorDialog(
            viewModel = viewModel,
            operator = operatorToEdit,
            onDismiss = { operatorToEdit = null },
            onSave = { op ->
                viewModel.saveOperator(op)
                operatorToEdit = null
            }
        )
    }
}

// --- MODULE 4: MACHINE REQUIREMENT ENTRY ---
@Composable
fun RequirementsScreen(viewModel: AllocationViewModel) {
    val machines by viewModel.machines.collectAsStateWithLifecycle()
    val stations by viewModel.stations.collectAsStateWithLifecycle()
    val activeRequirement by viewModel.activeRequirement.collectAsStateWithLifecycle()

    val selectedMachineIds = Converters().fromStringList(activeRequirement?.requiredMachinesJson)
    val selectedStationIds = Converters().fromStringList(activeRequirement?.requiredStationsJson)

    var showAddMachine by remember { mutableStateOf(false) }
    var showAddStation by remember { mutableStateOf(false) }
    var machineToEdit by remember { mutableStateOf<Machine?>(null) }
    var stationToEdit by remember { mutableStateOf<AssemblyStation?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Column {
                Text(
                    "Shopfloor Requirements",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Select active machines and stations for production:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // Machines (Machining Shop) Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Machining Shop Floor",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                TextButton(
                    onClick = { showAddMachine = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Machine", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Machine", fontSize = 13.sp)
                }
            }
        }

        items(machines) { mac ->
            val isChecked = selectedMachineIds.contains(mac.id)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleMachineRequirement(mac.id, !isChecked) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isChecked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(8.dp),
                border = CardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { viewModel.toggleMachineRequirement(mac.id, it) }
                        )
                        Column {
                            Text(mac.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Priority Level: ${mac.priority}", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Badge(
                            containerColor = when (mac.status) {
                                "Running" -> Color(0xFFD1FAE5)
                                "Idle" -> Color(0xFFF3F4F6)
                                else -> Color(0xFFFEE2E2)
                            },
                            contentColor = when (mac.status) {
                                "Running" -> Color(0xFF065F46)
                                "Idle" -> Color(0xFF374151)
                                else -> Color(0xFF991B1B)
                            },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(mac.status, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }

                        IconButton(
                            onClick = { machineToEdit = mac },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Machine", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                        }

                        IconButton(
                            onClick = { viewModel.deleteMachine(mac) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Machine", tint = Color.Red, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // Stations (Assembly Shop) Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Assembly Lines & Stations",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                TextButton(
                    onClick = { showAddStation = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Station", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Station", fontSize = 13.sp)
                }
            }
        }

        items(stations) { sta ->
            val isChecked = selectedStationIds.contains(sta.id)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleStationRequirement(sta.id, !isChecked) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isChecked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(8.dp),
                border = CardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { viewModel.toggleStationRequirement(sta.id, it) }
                        )
                        Column {
                            Text(sta.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Priority Level: ${sta.priority}", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Badge(
                            containerColor = when (sta.status) {
                                "Running" -> Color(0xFFD1FAE5)
                                "Idle" -> Color(0xFFF3F4F6)
                                else -> Color(0xFFFEE2E2)
                            },
                            contentColor = when (sta.status) {
                                "Running" -> Color(0xFF065F46)
                                "Idle" -> Color(0xFF374151)
                                else -> Color(0xFF991B1B)
                            },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(sta.status, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }

                        IconButton(
                            onClick = { stationToEdit = sta },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Station", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                        }

                        IconButton(
                            onClick = { viewModel.deleteStation(sta) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Station", tint = Color.Red, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = { viewModel.navigateTo(Screen.Allocations) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Proceed to Allocation Engine", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showAddMachine) {
        AddEditMachineDialog(
            machine = null,
            onDismiss = { showAddMachine = false },
            onSave = { mac ->
                viewModel.saveMachine(mac)
                showAddMachine = false
            }
        )
    }

    if (machineToEdit != null) {
        AddEditMachineDialog(
            machine = machineToEdit,
            onDismiss = { machineToEdit = null },
            onSave = { mac ->
                viewModel.saveMachine(mac)
                machineToEdit = null
            }
        )
    }

    if (showAddStation) {
        AddEditStationDialog(
            station = null,
            onDismiss = { showAddStation = false },
            onSave = { sta ->
                viewModel.saveStation(sta)
                showAddStation = false
            }
        )
    }

    if (stationToEdit != null) {
        AddEditStationDialog(
            station = stationToEdit,
            onDismiss = { stationToEdit = null },
            onSave = { sta ->
                viewModel.saveStation(sta)
                stationToEdit = null
            }
        )
    }
}

// --- MODULE 5 & 6: AUTO ALLOCATION ENGINE & REPORT ---
@Composable
fun AllocationsScreen(viewModel: AllocationViewModel) {
    val allocations by viewModel.allocations.collectAsStateWithLifecycle()
    val activeRequirement by viewModel.activeRequirement.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedShift by viewModel.selectedShift.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val totalReq = allocations.size
    val totalAllocated = allocations.count { it.allocatedOperatorId != null }
    val shortage = totalReq - totalAllocated

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Column {
                Text(
                    "Auto Allocation Engine",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Assigns optimized matching based on your live matrices:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // Engine control box (Visible to Supervisors, Managers, Admins)
        if (currentUser?.role != "Viewer") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = CardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Allocation Commands",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (activeRequirement == null) {
                            Text(
                                "⚠️ Setup shopfloor requirements first before allocating.",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.autoAllocateManpower() },
                                modifier = Modifier.weight(1.5f).height(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                enabled = activeRequirement != null
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Run")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Generate", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { viewModel.clearAllocations() },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Clear")
                            }
                        }
                    }
                }
            }
        }

        // Export & Share choices (Module 7)
        if (allocations.isNotEmpty()) {
            item {
                var note1 by remember { mutableStateOf("ETB Water Pump machining & assembly critical , So monitor output .") }
                var note2 by remember { mutableStateOf("Instruct all Operators to Wear Goggle, handgloves & safety shoes in $selectedShift shift") }
                var note3 by remember { mutableStateOf("ETB WP Body , Makino , 4253-12 , PT4 WP Machining , JCB WP Assembly critical for week schedule") }
                var note4 by remember { mutableStateOf("Instruct all casuals to put Box Material in systematic way in $selectedShift shift at designated location , Emergency path should not block") }
                var showInstructionEditor by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = CardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Daily Planning Shift Plan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(onClick = { showInstructionEditor = !showInstructionEditor }) {
                                Icon(
                                    if (showInstructionEditor) Icons.Default.Done else Icons.Default.Edit,
                                    contentDescription = "Edit Instructions",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (showInstructionEditor) "Save Notes" else "Edit Notes", fontSize = 12.sp)
                            }
                        }

                        if (showInstructionEditor) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Edit Planning Footer Notes:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                OutlinedTextField(
                                    value = note1,
                                    onValueChange = { note1 = it },
                                    label = { Text("Critical Alert Note") },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = TextStyle(fontSize = 12.sp)
                                )
                                OutlinedTextField(
                                    value = note2,
                                    onValueChange = { note2 = it },
                                    label = { Text("Safety Note") },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = TextStyle(fontSize = 12.sp)
                                )
                                OutlinedTextField(
                                    value = note3,
                                    onValueChange = { note3 = it },
                                    label = { Text("Critical Line Note") },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = TextStyle(fontSize = 12.sp)
                                )
                                OutlinedTextField(
                                    value = note4,
                                    onValueChange = { note4 = it },
                                    label = { Text("General Instructions") },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = TextStyle(fontSize = 12.sp)
                                )
                            }
                        }

                        // Export & Download Options right after Edit Notes section
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Export & Share Report Options",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            // Row 1: Direct File Downloads / Sharing
                            Text("Standard Share / File Downloads:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 1. Colorful HTML (Stunningly styled colorful format download)
                                Button(
                                    onClick = {
                                        val htmlContent = ExportHelper.generateHtmlReport(
                                            companyName = "Apex Manufacturing Corp",
                                            date = selectedDate,
                                            shift = selectedShift,
                                            department = "Machining & Assembly",
                                            supervisor = currentUser?.name ?: "Duty Supervisor",
                                            allocations = allocations
                                        )
                                        ExportHelper.shareReport(
                                            context,
                                            "colorful_allocation_report_${selectedDate}_${selectedShift}.html",
                                            htmlContent,
                                            "text/html"
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71)), // Vibrant emerald green
                                    modifier = Modifier.weight(1.3f).height(40.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Colorful HTML", modifier = Modifier.size(14.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Colorful HTML", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                // 2. CSV (No Notes) - Satisfying "not with note"
                                Button(
                                    onClick = {
                                        val csvContent = ExportHelper.generateShiftPlanCsv(
                                            shift = selectedShift,
                                            allocations = allocations,
                                            customInstructions = emptyList() // No notes
                                        )
                                        ExportHelper.shareReport(
                                            context,
                                            "${selectedShift}_Shift_Plan_No_Notes_${selectedDate}.csv",
                                            csvContent,
                                            "text/csv"
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE67E22)), // Custom orange
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "CSV No Notes", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("CSV (No Notes)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                // 3. CSV (With Notes)
                                OutlinedButton(
                                    onClick = {
                                        val csvContent = ExportHelper.generateShiftPlanCsv(
                                            shift = selectedShift,
                                            allocations = allocations,
                                            customInstructions = listOf(note1, note2, note3, note4)
                                        )
                                        ExportHelper.shareReport(
                                            context,
                                            "${selectedShift}_Shift_Plan_With_Notes_${selectedDate}.csv",
                                            csvContent,
                                            "text/csv"
                                        )
                                    },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text("CSV (With Notes)", fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // Row 2: WhatsApp Specific Sharing
                            Text("Share Directly with WhatsApp:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 1. WhatsApp Colorful HTML Share
                                Button(
                                    onClick = {
                                        val htmlContent = ExportHelper.generateHtmlReport(
                                            companyName = "Apex Manufacturing Corp",
                                            date = selectedDate,
                                            shift = selectedShift,
                                            department = "Machining & Assembly",
                                            supervisor = currentUser?.name ?: "Duty Supervisor",
                                            allocations = allocations
                                        )
                                        ExportHelper.shareToWhatsApp(
                                            context,
                                            "colorful_allocation_report_${selectedDate}_${selectedShift}.html",
                                            htmlContent,
                                            "text/html"
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)), // WhatsApp Green
                                    modifier = Modifier.weight(1.3f).height(40.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "WhatsApp HTML", modifier = Modifier.size(14.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WhatsApp HTML", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                // 2. WhatsApp CSV Share (No Notes)
                                Button(
                                    onClick = {
                                        val csvContent = ExportHelper.generateShiftPlanCsv(
                                            shift = selectedShift,
                                            allocations = allocations,
                                            customInstructions = emptyList() // No notes
                                        )
                                        ExportHelper.shareToWhatsApp(
                                            context,
                                            "${selectedShift}_Shift_Plan_No_Notes_${selectedDate}.csv",
                                            csvContent,
                                            "text/csv"
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF128C7E)), // WhatsApp Teal
                                    modifier = Modifier.weight(1.2f).height(40.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "WhatsApp CSV", modifier = Modifier.size(14.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WhatsApp CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        // THE EXACT VISUAL TABLE LAYOUT FROM THE ATTACHED PLANNING SHEET
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
                                .clip(RoundedCornerShape(4.dp))
                        ) {
                            // Orange Merged Header: Shift Plan
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFE67E22))
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$selectedShift Shift Plan",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            HorizontalDivider(color = Color.Black, thickness = 1.dp)

                            // Table Rows grouped by station name
                            val groupedAllocations = allocations.groupBy { it.targetName }
                            groupedAllocations.forEach { (stationName, stationAllocs) ->
                                val operatorsText = stationAllocs.mapNotNull { it.allocatedOperatorName }.joinToString(" + ")
                                val remarksText = stationAllocs.map { it.remarks }.firstOrNull { it.isNotEmpty() } ?: ""

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Station Name Column (Left)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = stationName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.Black
                                        )
                                    }

                                    // Vertical black dividing line
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(52.dp)
                                            .background(Color.Black)
                                    )

                                    // Assigned Operators & Remarks Column (Right)
                                    Box(
                                        modifier = Modifier
                                            .weight(1.5f)
                                            .padding(10.dp)
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (operatorsText.isNotEmpty()) {
                                                    Text(
                                                        text = operatorsText,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = Color.Black
                                                    )
                                                    if (remarksText.isNotEmpty()) {
                                                        Text(
                                                            text = " ( $remarksText )",
                                                            fontSize = 12.sp,
                                                            color = Color(0xFF1D4ED8), // Dark Blue alert
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                } else {
                                                    Text(
                                                        text = "Plan one OT / Vacant",
                                                        fontStyle = FontStyle.Italic,
                                                        fontSize = 13.sp,
                                                        color = Color(0xFF991B1B) // Dark Red alert
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                HorizontalDivider(color = Color.Black, thickness = 1.dp)
                            }

                            // Color-Coded Instruction Footers exactly as in the attached sheet image
                            // Note 1: Light Orange background for critical monitoring alert
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFFEDD5))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = note1,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            HorizontalDivider(color = Color.Black, thickness = 1.dp)

                            // Note 2: Light Green background for PPE Safety Instruction
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFD1FAE5))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = note2,
                                    color = Color(0xFF1E40AF), // Blue text for safety instructions
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            HorizontalDivider(color = Color.Black, thickness = 1.dp)

                            // Note 3: Light Green background with critical weekly schedule details
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFD1FAE5))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = note3,
                                    color = Color(0xFF991B1B), // Red text for critical lines
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            HorizontalDivider(color = Color.Black, thickness = 1.dp)

                            // Note 4: Light Grey background for casuals & housekeeping instructions
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF3F4F6))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = note4,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Allocation Report Grid
        item {
            Text("Module 6: Company Allocation Report Sheet", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        if (allocations.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No allocations generated yet. Click 'Generate' above.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            items(allocations) { alloc ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(8.dp),
                    border = CardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(alloc.targetName, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text(
                                text = if (alloc.allocatedOperatorId != null) "Assigned: ${alloc.allocatedOperatorName}" else "Unassigned",
                                fontSize = 13.sp,
                                color = if (alloc.allocatedOperatorId != null) MaterialTheme.colorScheme.secondary else Color.Red,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (alloc.remarks.isNotEmpty()) {
                                Text(alloc.remarks, fontSize = 11.sp, color = Color.Gray)
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(1f)
                        ) {
                            Badge(
                                containerColor = when (alloc.status) {
                                    "Allocated" -> Color(0xFFD1FAE5)
                                    "Skill Gap" -> Color(0xFFFEF3C7)
                                    else -> Color(0xFFFEE2E2)
                                },
                                contentColor = when (alloc.status) {
                                    "Allocated" -> Color(0xFF065F46)
                                    "Skill Gap" -> Color(0xFFD97706)
                                    else -> Color(0xFF991B1B)
                                }
                            ) {
                                Text(alloc.status, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Skill Level: ${alloc.skillLevel}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- MODULE 2: SKILL MATRIX MASTER ---
@Composable
fun SkillMatrixScreen(viewModel: AllocationViewModel) {
    val operators by viewModel.operators.collectAsStateWithLifecycle()
    var operatorSearchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var operatorToEdit by remember { mutableStateOf<Operator?>(null) }

    val filteredOps = operators.filter {
        it.name.contains(operatorSearchQuery, true) || it.employeeId.contains(operatorSearchQuery, true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Skill Matrix Master",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Manage employee training indices",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(99.dp))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Operator", tint = Color.White)
                }
            }
        }

        item {
            OutlinedTextField(
                value = operatorSearchQuery,
                onValueChange = { operatorSearchQuery = it },
                label = { Text("Search Operators by Name or ID") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )
        }

        items(filteredOps) { op ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { operatorToEdit = op },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(8.dp),
                border = CardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(op.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Emp ID: ${op.employeeId} | Dept: ${op.department}", fontSize = 12.sp, color = Color.Gray)
                        Text("Shift: ${op.shift} | Exp: ${op.experienceYears} Years", fontSize = 12.sp, color = Color.Gray)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Badge(
                            containerColor = when (op.skillLevel) {
                                5 -> Color(0xFFFCE7F3)
                                4 -> Color(0xFFF3E8FF)
                                3 -> Color(0xFFDBEAFE)
                                else -> Color(0xFFE2E8F0)
                            }
                        ) {
                            Text("Lvl ${op.skillLevel}", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }

                        IconButton(onClick = { viewModel.deleteOperator(op) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }

    // Add Operator Dialog
    if (showAddDialog) {
        AddEditOperatorDialog(
            viewModel = viewModel,
            operator = null,
            onDismiss = { showAddDialog = false },
            onSave = { op ->
                viewModel.saveOperator(op)
                showAddDialog = false
            }
        )
    }

    // Edit Operator Dialog
    if (operatorToEdit != null) {
        AddEditOperatorDialog(
            viewModel = viewModel,
            operator = operatorToEdit,
            onDismiss = { operatorToEdit = null },
            onSave = { op ->
                viewModel.saveOperator(op)
                operatorToEdit = null
            }
        )
    }
}

@Composable
fun AddEditOperatorDialog(
    viewModel: AllocationViewModel,
    operator: Operator?,
    onDismiss: () -> Unit,
    onSave: (Operator) -> Unit
) {
    val machines by viewModel.machines.collectAsStateWithLifecycle()
    val stations by viewModel.stations.collectAsStateWithLifecycle()

    var empId by remember { mutableStateOf(operator?.employeeId ?: "") }
    var name by remember { mutableStateOf(operator?.name ?: "") }
    var dept by remember { mutableStateOf(operator?.department ?: "Machining") }
    var shift by remember { mutableStateOf(operator?.shift ?: "A") }
    var skillLvl by remember { mutableStateOf(operator?.skillLevel ?: 3) }
    var exp by remember { mutableStateOf(operator?.experienceYears?.toString() ?: "3.0") }
    
    // Skill Matrix state map
    var skillsMap by remember { mutableStateOf(com.example.data.Converters().fromStringMap(operator?.skillsJson ?: "{}")) }
    var customWorkstationName by remember { mutableStateOf("") }
    var customWorkstationLevel by remember { mutableStateOf(3) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Fixed Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (operator == null) "Add New Operator" else "Edit Operator Matrix",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

                // Scrollable content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = empId,
                            onValueChange = { empId = it },
                            label = { Text("Employee ID") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = operator == null
                        )
                    }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Operator Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("Department", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Machining", "Assembly").forEach { dp ->
                            FilterChip(
                                selected = dept == dp,
                                onClick = { dept = dp },
                                label = { Text(dp) }
                            )
                        }
                    }
                }

                // SUBSECTION: Cell/Station Name Skill Category Dropdown (Beginner / Expert)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Add Cell / Station Skill (By Category)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Select a cell and category to save as a specialized skill.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )

                        val cellNames = (machines.map { it.name } + stations.map { it.name }).distinct()
                        var selectedCell by remember(cellNames) { mutableStateOf(cellNames.firstOrNull() ?: "") }
                        var skillCategory by remember { mutableStateOf("beginner") } // "beginner" or "expert"
                        var expandedDropdown by remember { mutableStateOf(false) }

                        // Custom click-to-open card dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedCard(
                                onClick = { expandedDropdown = !expandedDropdown },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (selectedCell.isEmpty()) "Select Cell Name..." else selectedCell,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown"
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = expandedDropdown,
                                onDismissRequest = { expandedDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                if (cellNames.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No cells/stations configured") },
                                        onClick = { expandedDropdown = false }
                                    )
                                } else {
                                    cellNames.forEach { name ->
                                        DropdownMenuItem(
                                            text = { Text(name) },
                                            onClick = {
                                                selectedCell = name
                                                expandedDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Category choice Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Category:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            listOf("beginner", "expert").forEach { cat ->
                                FilterChip(
                                    selected = skillCategory == cat,
                                    onClick = { skillCategory = cat },
                                    label = { Text(cat.uppercase()) }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (selectedCell.isNotEmpty()) {
                                    // format: VISTA assembly station - beginner or vista assembly - expert
                                    val finalSkillKey = "$selectedCell - $skillCategory"
                                    val newMap = skillsMap.toMutableMap()
                                    // assign rating value corresponding to beginner (1) or expert (5)
                                    newMap[finalSkillKey] = if (skillCategory == "expert") 5 else 1
                                    skillsMap = newMap
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Skill", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Cell Skill Category", fontSize = 12.sp)
                        }
                    }
                }

                item {
                    Text("Shift", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("A", "B", "C", "General").forEach { sf ->
                            FilterChip(
                                selected = shift == sf,
                                onClick = { shift = sf },
                                label = { Text("Shift $sf") }
                            )
                        }
                    }
                }

                item {
                    Text("Overall Skill Rank (0 to 5)", fontWeight = FontWeight.Bold)
                    Slider(
                        value = skillLvl.toFloat(),
                        onValueChange = { skillLvl = it.toInt() },
                        valueRange = 0f..5f,
                        steps = 4
                    )
                    Text("Current Selected: Level $skillLvl")
                }

                // WORKSTATION SKILL MATRIX SECTION
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Workstation Training Matrix", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    Text("Select workstations this operator is trained on and set their index (1-5)", fontSize = 12.sp, color = Color.Gray)
                }

                // Machining Workstations
                item {
                    Text("Machining Shop Floor Stations", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                if (machines.isEmpty()) {
                    item {
                        Text("No machines configured in settings.", fontSize = 12.sp, color = Color.Gray)
                    }
                } else {
                    items(machines) { mac ->
                        val isTrained = skillsMap.containsKey(mac.name)
                        val currentLvl = skillsMap[mac.name] ?: 3
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = if (isTrained) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface),
                            border = CardBorder()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = isTrained,
                                        onCheckedChange = { checked ->
                                            val newMap = skillsMap.toMutableMap()
                                            if (checked) {
                                                newMap[mac.name] = 3 // default to level 3
                                            } else {
                                                newMap.remove(mac.name)
                                            }
                                            skillsMap = newMap
                                        }
                                    )
                                    Text(mac.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.weight(1f))
                                    if (isTrained) {
                                        Badge { Text("Trained") }
                                    }
                                }
                                if (isTrained) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                                    ) {
                                        Text("Level: ", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        (1..5).forEach { lvl ->
                                            val isSelected = currentLvl == lvl
                                            val chipColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                            val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(chipColor)
                                                    .clickable {
                                                        val newMap = skillsMap.toMutableMap()
                                                        newMap[mac.name] = lvl
                                                        skillsMap = newMap
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(lvl.toString(), color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Assembly Workstations
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Assembly Shop Floor Stations", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                if (stations.isEmpty()) {
                    item {
                        Text("No assembly stations configured in settings.", fontSize = 12.sp, color = Color.Gray)
                    }
                } else {
                    items(stations) { sta ->
                        val isTrained = skillsMap.containsKey(sta.name)
                        val currentLvl = skillsMap[sta.name] ?: 3
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = if (isTrained) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface),
                            border = CardBorder()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = isTrained,
                                        onCheckedChange = { checked ->
                                            val newMap = skillsMap.toMutableMap()
                                            if (checked) {
                                                newMap[sta.name] = 3 // default to level 3
                                            } else {
                                                newMap.remove(sta.name)
                                            }
                                            skillsMap = newMap
                                        }
                                    )
                                    Text(sta.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.weight(1f))
                                    if (isTrained) {
                                        Badge { Text("Trained") }
                                    }
                                }
                                if (isTrained) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                                    ) {
                                        Text("Level: ", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        (1..5).forEach { lvl ->
                                            val isSelected = currentLvl == lvl
                                            val chipColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                            val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(chipColor)
                                                    .clickable {
                                                        val newMap = skillsMap.toMutableMap()
                                                        newMap[sta.name] = lvl
                                                        skillsMap = newMap
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(lvl.toString(), color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Custom / Other Workstations & Cell Names
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Custom Cells & Sub-categories (Add on-the-fly)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = CardBorder()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Assign Custom Workstation or Cell Name:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = customWorkstationName,
                                    onValueChange = { customWorkstationName = it },
                                    label = { Text("Cell/Station Name") },
                                    modifier = Modifier.weight(1.5f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    (1..5).forEach { lvl ->
                                        val isSelected = customWorkstationLevel == lvl
                                        val chipColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(chipColor)
                                                .clickable { customWorkstationLevel = lvl },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(lvl.toString(), fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        if (customWorkstationName.trim().isNotEmpty()) {
                                            val newMap = skillsMap.toMutableMap()
                                            newMap[customWorkstationName.trim()] = customWorkstationLevel
                                            skillsMap = newMap
                                            customWorkstationName = ""
                                        }
                                    },
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                        .size(36.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Custom Station", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                val masterNames = machines.map { it.name }.toSet() + stations.map { it.name }.toSet()
                val customSkills = skillsMap.filterKeys { !masterNames.contains(it) }

                if (customSkills.isNotEmpty()) {
                    items(customSkills.keys.toList()) { key ->
                        val currentLvl = skillsMap[key] ?: 3
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                            border = CardBorder()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(key, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Text("Level: ", fontSize = 11.sp, color = Color.Gray)
                                        (1..5).forEach { lvl ->
                                            val isSelected = currentLvl == lvl
                                            val chipColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                            val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(chipColor)
                                                    .clickable {
                                                        val newMap = skillsMap.toMutableMap()
                                                        newMap[key] = lvl
                                                        skillsMap = newMap
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(lvl.toString(), color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        val newMap = skillsMap.toMutableMap()
                                        newMap.remove(key)
                                        skillsMap = newMap
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

            // Fixed Sticky Footer for Save and Cancel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (empId.isNotEmpty() && name.isNotEmpty()) {
                            onSave(
                                Operator(
                                    employeeId = empId,
                                    name = name,
                                    department = dept,
                                    shift = shift,
                                    status = "Active",
                                    skillLevel = skillLvl,
                                    experienceYears = exp.toDoubleOrNull() ?: 3.0,
                                    skillsJson = com.example.data.Converters().toStringMap(skillsMap)
                                )
                            )
                        }
                    },
                    enabled = empId.isNotEmpty() && name.isNotEmpty()
                ) {
                    Icon(Icons.Default.Done, contentDescription = "Save", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Operator")
                }
            }
        }
    }
}
}

// --- MODULE 9: ADMIN SETTINGS ---
@Composable
fun SettingsScreen(viewModel: AllocationViewModel) {
    val machines by viewModel.machines.collectAsStateWithLifecycle()
    val stations by viewModel.stations.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var showAddMachine by remember { mutableStateOf(false) }
    var showAddStation by remember { mutableStateOf(false) }
    var editingMachine by remember { mutableStateOf<Machine?>(null) }
    var editingStation by remember { mutableStateOf<AssemblyStation?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Column {
                Text(
                    "Admin Configuration Control",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Master settings editable dynamically after deployment:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // Firebase Sync Guidelines Box
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Cloud Sync Setup (Firebase Firestore)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "To sync this app globally for all users instantly without reinstalling:\n" +
                                "1. Create a Firebase project.\n" +
                                "2. Download 'google-services.json' and place it in your 'app/' directory.\n" +
                                "3. Enable Firestore in Firestore Database with 'Operators', 'Machines', 'AssemblyStations' collections.\n" +
                                "4. Recompile and install. Dynamic sync starts instantly!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Machines Master List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Machine Master Grid", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Button(onClick = { showAddMachine = true }) {
                    Text("+ Add Machine")
                }
            }
        }

        items(machines) { mac ->
            var inlineName by remember(mac.id) { mutableStateOf(mac.name) }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inlineName,
                            onValueChange = { 
                                inlineName = it
                                viewModel.saveMachine(mac.copy(name = it))
                            },
                            label = { Text("Machine Name", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                            singleLine = true
                        )
                        IconButton(
                            onClick = { viewModel.deleteMachine(mac) },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ID: ${mac.id}", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Priority Level:", fontSize = 11.sp, color = Color.Gray)
                            (1..5).forEach { p ->
                                val isSelected = mac.priority == p
                                val pColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                val pTextColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(pColor)
                                        .clickable {
                                            viewModel.saveMachine(mac.copy(priority = p))
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(p.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = pTextColor)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Status:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Running", "Idle", "Maintenance").forEach { st ->
                                val isSelected = mac.status == st
                                val chipColor = if (isSelected) {
                                    when (st) {
                                        "Running" -> Color(0xFF2ECC71).copy(alpha = 0.2f)
                                        "Idle" -> Color(0xFFF1C40F).copy(alpha = 0.2f)
                                        else -> Color(0xFFE74C3C).copy(alpha = 0.2f)
                                    }
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                }
                                val borderColor = if (isSelected) {
                                    when (st) {
                                        "Running" -> Color(0xFF2ECC71)
                                        "Idle" -> Color(0xFFF1C40F)
                                        else -> Color(0xFFE74C3C)
                                    }
                                } else {
                                    Color.Transparent
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(chipColor)
                                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewModel.saveMachine(mac.copy(status = st))
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = st,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) borderColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Assembly Stations Master List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Assembly Stations Grid", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Button(onClick = { showAddStation = true }) {
                    Text("+ Add Station")
                }
            }
        }

        items(stations) { sta ->
            var inlineName by remember(sta.id) { mutableStateOf(sta.name) }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inlineName,
                            onValueChange = { 
                                inlineName = it
                                viewModel.saveStation(sta.copy(name = it))
                            },
                            label = { Text("Station Name", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                            singleLine = true
                        )
                        IconButton(
                            onClick = { viewModel.deleteStation(sta) },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ID: ${sta.id}", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Priority Level:", fontSize = 11.sp, color = Color.Gray)
                            (1..5).forEach { p ->
                                val isSelected = sta.priority == p
                                val pColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                val pTextColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(pColor)
                                        .clickable {
                                            viewModel.saveStation(sta.copy(priority = p))
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(p.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = pTextColor)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Status:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Running", "Idle", "Maintenance").forEach { st ->
                                val isSelected = sta.status == st
                                val chipColor = if (isSelected) {
                                    when (st) {
                                        "Running" -> Color(0xFF2ECC71).copy(alpha = 0.2f)
                                        "Idle" -> Color(0xFFF1C40F).copy(alpha = 0.2f)
                                        else -> Color(0xFFE74C3C).copy(alpha = 0.2f)
                                    }
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                }
                                val borderColor = if (isSelected) {
                                    when (st) {
                                        "Running" -> Color(0xFF2ECC71)
                                        "Idle" -> Color(0xFFF1C40F)
                                        else -> Color(0xFFE74C3C)
                                    }
                                } else {
                                    Color.Transparent
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(chipColor)
                                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewModel.saveStation(sta.copy(status = st))
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = st,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) borderColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Machine Dialog
    if (showAddMachine) {
        AddEditMachineDialog(
            machine = null,
            onDismiss = { showAddMachine = false },
            onSave = { mac ->
                viewModel.saveMachine(mac)
                showAddMachine = false
            }
        )
    }

    // Edit Machine Dialog
    if (editingMachine != null) {
        AddEditMachineDialog(
            machine = editingMachine,
            onDismiss = { editingMachine = null },
            onSave = { mac ->
                viewModel.saveMachine(mac)
                editingMachine = null
            }
        )
    }

    // Add Station Dialog
    if (showAddStation) {
        AddEditStationDialog(
            station = null,
            onDismiss = { showAddStation = false },
            onSave = { sta ->
                viewModel.saveStation(sta)
                showAddStation = false
            }
        )
    }

    // Edit Station Dialog
    if (editingStation != null) {
        AddEditStationDialog(
            station = editingStation,
            onDismiss = { editingStation = null },
            onSave = { sta ->
                viewModel.saveStation(sta)
                editingStation = null
            }
        )
    }
}

@Composable
fun AddMasterItemDialog(
    title: String,
    onDismiss: () -> Unit,
    onSave: (String, String, Int) -> Unit
) {
    var itemId by remember { mutableStateOf("") }
    var itemName by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(3) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)

                OutlinedTextField(
                    value = itemId,
                    onValueChange = { itemId = it },
                    label = { Text("Code / ID") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Priority Level (1 to 5)", fontWeight = FontWeight.Bold)
                Slider(
                    value = priority.toFloat(),
                    onValueChange = { priority = it.toInt() },
                    valueRange = 1f..5f,
                    steps = 3
                )
                Text("Selected Priority: $priority")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(onClick = {
                        if (itemId.isNotEmpty() && itemName.isNotEmpty()) {
                            onSave(itemId, itemName, priority)
                        }
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditMachineDialog(
    machine: Machine?,
    onDismiss: () -> Unit,
    onSave: (Machine) -> Unit
) {
    var itemId by remember { mutableStateOf(machine?.id ?: "") }
    var itemName by remember { mutableStateOf(machine?.name ?: "") }
    var status by remember { mutableStateOf(machine?.status ?: "Running") }
    var priority by remember { mutableStateOf(machine?.priority ?: 3) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (machine == null) "Add Machine" else "Edit Machine",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = itemId,
                    onValueChange = { itemId = it },
                    label = { Text("Code / ID") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = machine == null
                )

                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Status", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Running", "Idle", "Maintenance").forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st) }
                        )
                    }
                }

                Text("Priority Level (1 to 5)", fontWeight = FontWeight.Bold)
                Slider(
                    value = priority.toFloat(),
                    onValueChange = { priority = it.toInt() },
                    valueRange = 1f..5f,
                    steps = 3
                )
                Text("Selected Priority: $priority")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(onClick = {
                        if (itemId.isNotEmpty() && itemName.isNotEmpty()) {
                            onSave(Machine(id = itemId, name = itemName, department = "Machining", status = status, priority = priority))
                        }
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditStationDialog(
    station: AssemblyStation?,
    onDismiss: () -> Unit,
    onSave: (AssemblyStation) -> Unit
) {
    var itemId by remember { mutableStateOf(station?.id ?: "") }
    var itemName by remember { mutableStateOf(station?.name ?: "") }
    var status by remember { mutableStateOf(station?.status ?: "Running") }
    var priority by remember { mutableStateOf(station?.priority ?: 3) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (station == null) "Add Assembly Station" else "Edit Assembly Station",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = itemId,
                    onValueChange = { itemId = it },
                    label = { Text("Code / ID") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = station == null
                )

                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Status", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Running", "Idle", "Setup").forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st) }
                        )
                    }
                }

                Text("Priority Level (1 to 5)", fontWeight = FontWeight.Bold)
                Slider(
                    value = priority.toFloat(),
                    onValueChange = { priority = it.toInt() },
                    valueRange = 1f..5f,
                    steps = 3
                )
                Text("Selected Priority: $priority")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(onClick = {
                        if (itemId.isNotEmpty() && itemName.isNotEmpty()) {
                            onSave(AssemblyStation(id = itemId, name = itemName, department = "Assembly", status = status, priority = priority))
                        }
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

// --- UTILITY VIEW IMPLEMENTATIONS ---

@Composable
fun DashboardCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = CardBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(24.dp))
            Text(title, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun QuickMetric(label: String, value: String, highlightColor: Color = Color.Unspecified) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = highlightColor)
        Text(label, fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
fun CardBorder() = androidx.compose.foundation.BorderStroke(
    1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
)

private fun showDatePicker(context: Context, onDateSelected: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val monthStr = if (month + 1 < 10) "0${month + 1}" else "${month + 1}"
            val dayStr = if (dayOfMonth < 10) "0$dayOfMonth" else "$dayOfMonth"
            onDateSelected("$year-$monthStr-$dayStr")
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}
