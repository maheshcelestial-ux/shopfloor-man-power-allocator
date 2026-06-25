package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object AllocationEngine {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    /**
     * Executes the automatic manpower allocation logic.
     *
     * @param date The allocation date (YYYY-MM-DD)
     * @param shift The shift (A, B, C, General)
     * @param requiredMachines List of machine entities that are required for this shift
     * @param requiredStations List of assembly station entities that are required for this shift
     * @param presentOperators List of operator entities who are marked present for this shift
     * @return List of generated Allocation records
     */
    fun generateAllocation(
        date: String,
        shift: String,
        requiredMachines: List<Machine>,
        requiredStations: List<AssemblyStation>,
        presentOperators: List<Operator>
    ): List<Allocation> {
        val allocations = mutableListOf<Allocation>()
        val allocatedOperatorIds = mutableSetOf<String>()

        // Helper to parse skill matrix from JSON
        fun getSkillLevel(operator: Operator, skillName: String): Int {
            return try {
                val type = Types.newParameterizedType(Map::class.java, String::class.java, Integer::class.java)
                val adapter = moshi.adapter<Map<String, Int>>(type)
                val skills = adapter.fromJson(operator.skillsJson) ?: emptyMap()
                skills[skillName] ?: 0
            } catch (e: Exception) {
                0
            }
        }

        // We process requirements by priority (highest priority first).
        // Let's combine machines and stations into a single requirement list.
        // Pair(Item ID, Item Name, IsMachine, Priority)
        data class RequirementItem(
            val id: String,
            val name: String,
            val isMachine: Boolean,
            val priority: Int
        )

        val requirements = mutableListOf<RequirementItem>()
        requiredMachines.forEach {
            requirements.add(RequirementItem(it.id, it.name, true, it.priority))
        }
        requiredStations.forEach {
            requirements.add(RequirementItem(it.id, it.name, false, it.priority))
        }

        // Sort by priority descending (higher value = higher priority), then by name
        val sortedRequirements = requirements.sortedWith(
            compareByDescending<RequirementItem> { it.priority }
                .thenBy { it.name }
        )

        for (req in sortedRequirements) {
            // Find candidates who:
            // 1. Are present
            // 2. Are not yet allocated
            // 3. Have trained skill level > 0 for this specific machine/station
            val candidates = presentOperators.filter { op ->
                !allocatedOperatorIds.contains(op.employeeId) && getSkillLevel(op, req.name) > 0
            }.map { op ->
                val level = getSkillLevel(op, req.name)
                Pair(op, level)
            }

            // Sort candidates by skill level descending, then by experience descending
            val selectedCandidate = candidates.sortedWith(
                compareByDescending<Pair<Operator, Int>> { it.second }
                    .thenByDescending { it.first.experienceYears }
            ).firstOrNull()

            if (selectedCandidate != null) {
                val (operator, level) = selectedCandidate
                allocatedOperatorIds.add(operator.employeeId)
                allocations.add(
                    Allocation(
                        date = date,
                        shift = shift,
                        targetId = req.id,
                        targetName = req.name,
                        isMachine = req.isMachine,
                        allocatedOperatorId = operator.employeeId,
                        allocatedOperatorName = operator.name,
                        skillLevel = level,
                        status = "Allocated",
                        remarks = "Optimal Match (Level $level)"
                    )
                )
            } else {
                // Check if there is any unallocated operator left at all (generic shortage)
                val unallocatedOperators = presentOperators.filter { op ->
                    !allocatedOperatorIds.contains(op.employeeId)
                }

                if (unallocatedOperators.isEmpty()) {
                    allocations.add(
                        Allocation(
                            date = date,
                            shift = shift,
                            targetId = req.id,
                            targetName = req.name,
                            isMachine = req.isMachine,
                            allocatedOperatorId = null,
                            allocatedOperatorName = null,
                            skillLevel = 0,
                            status = "Shortage",
                            remarks = "Manpower Shortage (No Operators Left)"
                        )
                    )
                } else {
                    allocations.add(
                        Allocation(
                            date = date,
                            shift = shift,
                            targetId = req.id,
                            targetName = req.name,
                            isMachine = req.isMachine,
                            allocatedOperatorId = null,
                            allocatedOperatorName = null,
                            skillLevel = 0,
                            status = "Skill Gap",
                            remarks = "Skill Gap (Unallocated operators lack training)"
                        )
                    )
                }
            }
        }

        return allocations
    }
}
