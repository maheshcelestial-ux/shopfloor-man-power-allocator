package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.Allocation
import java.io.File

object ExportHelper {

    /**
     * Generates a beautifully formatted CSV string representing the official allocation report.
     */
    fun generateCsvReport(
        companyName: String,
        date: String,
        shift: String,
        department: String,
        supervisor: String,
        allocations: List<Allocation>
    ): String {
        val sb = java.lang.StringBuilder()
        
        // Header info
        sb.append("OFFICIAL MANPOWER ALLOCATION REPORT\n")
        sb.append("Company Name,$companyName\n")
        sb.append("Date,$date\n")
        sb.append("Shift,$shift\n")
        sb.append("Department,$department\n")
        sb.append("Supervisor,$supervisor\n")
        sb.append("\n")

        // Table Header
        sb.append("Sr No,Machine/Station,Required,Allocated Operator,Skill Level,Status,Remarks\n")

        // Table Rows
        allocations.forEachIndexed { idx, alloc ->
            val cleanName = alloc.allocatedOperatorName ?: "UNALLOCATED"
            val cleanRemarks = alloc.remarks.replace(",", ";")
            sb.append("${idx + 1},${alloc.targetName},Yes,$cleanName,${alloc.skillLevel},${alloc.status},$cleanRemarks\n")
        }
        sb.append("\n")

        // Summary Statistics
        val totalReq = allocations.size
        val totalAlloc = allocations.count { it.allocatedOperatorId != null }
        val shortage = totalReq - totalAlloc
        val utilization = if (totalReq > 0) (totalAlloc * 100) / totalReq else 0
        val avgSkill = if (totalAlloc > 0) allocations.filter { it.allocatedOperatorId != null }.map { it.skillLevel }.average() else 0.0

        sb.append("SUMMARY STATS\n")
        sb.append("Total Required Stations,$totalReq\n")
        sb.append("Total Operators Allocated,$totalAlloc\n")
        sb.append("Shortage / Gap,$shortage\n")
        sb.append("Allocation Utilization,$utilization%\n")
        sb.append("Average Skill Level,${String.format("%.1f", avgSkill)} / 5\n")

        return sb.toString()
    }

    /**
     * Generates the shift plan CSV following the exact format from the attached planning sheet image.
     */
    fun generateShiftPlanCsv(
        shift: String,
        allocations: List<Allocation>,
        customInstructions: List<String> = emptyList()
    ): String {
        val sb = java.lang.StringBuilder()
        
        // Header
        sb.append("${escapeCsvField("$shift Shift Plan")},\n")
        
        // Group allocations by targetName to handle multiple operators per station
        val grouped = allocations.groupBy { it.targetName }
        
        // Row items
        grouped.forEach { (stationName, stationAllocations) ->
            val operatorsText = stationAllocations.mapNotNull { it.allocatedOperatorName }.joinToString(" + ")
            val remarksText = stationAllocations.map { it.remarks }.firstOrNull { it.isNotEmpty() } ?: ""
            val rightContent = if (operatorsText.isNotEmpty()) {
                if (remarksText.isNotEmpty()) "$operatorsText ( $remarksText )" else operatorsText
            } else {
                if (remarksText.isNotEmpty()) remarksText else "Vacant"
            }
            sb.append("${escapeCsvField(stationName)},${escapeCsvField(rightContent)}\n")
        }
        
        // Footer notes / instructions as per attached image
        val standardFooters = listOf(
            "ETB Water Pump machining & assembly critical , So monitor output .",
            "Instruct all Operators to Wear Goggle, handgloves & safety shoes in $shift shift",
            "ETB WP Body , Makino , 4253-12 , PT4 WP Machining , JCB WP Assembly critical for week schedule",
            "Instruct all casuals to put Box Material in systematic way in $shift shift at designated location , Emergency path should not block"
        )
        
        val footers = if (customInstructions.isNotEmpty()) customInstructions else standardFooters
        footers.forEach { note ->
            sb.append("${escapeCsvField(note)},\n")
        }
        
        return sb.toString()
    }

    private fun escapeCsvField(field: String): String {
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\""
        }
        return field
    }

    /**
     * Generates a stunningly styled HTML file content representing the company's allocation grid.
     * This is perfect for high-fidelity printing, PDF generation, or emailing!
     */
    fun generateHtmlReport(
        companyName: String,
        date: String,
        shift: String,
        department: String,
        supervisor: String,
        allocations: List<Allocation>
    ): String {
        val totalReq = allocations.size
        val totalAlloc = allocations.count { it.allocatedOperatorId != null }
        val shortage = totalReq - totalAlloc
        val utilization = if (totalReq > 0) (totalAlloc * 100) / totalReq else 0
        val avgSkill = if (totalAlloc > 0) allocations.filter { it.allocatedOperatorId != null }.map { it.skillLevel }.average() else 0.0

        val tableRows = allocations.mapIndexed { idx, alloc ->
            val rowClass = when (alloc.status) {
                "Allocated" -> "row-allocated"
                "Skill Gap" -> "row-gap"
                else -> "row-shortage"
            }
            """
            <tr class="$rowClass">
                <td>${idx + 1}</td>
                <td><strong>${alloc.targetName}</strong></td>
                <td>Yes</td>
                <td>${alloc.allocatedOperatorName ?: "<em>None (Shortage)</em>"}</td>
                <td><span class="skill-pill lvl-${alloc.skillLevel}">${alloc.skillLevel}</span></td>
                <td><span class="status-badge state-${alloc.status.lowercase().replace(" ", "")}">${alloc.status}</span></td>
                <td>${alloc.remarks}</td>
            </tr>
            """.trimIndent()
        }.joinToString("\n")

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <title>Allocation Report - $companyName</title>
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                    color: #1e293b;
                    background-color: #f8fafc;
                    margin: 0;
                    padding: 40px;
                }
                .report-card {
                    background: #ffffff;
                    border-radius: 12px;
                    box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1);
                    padding: 30px;
                    border: 1px solid #e2e8f0;
                    max-width: 900px;
                    margin: 0 auto;
                }
                .header-table {
                    width: 100%;
                    border-collapse: collapse;
                    margin-bottom: 30px;
                }
                .header-table td {
                    padding: 6px 0;
                    font-size: 14px;
                }
                .company-title {
                    font-size: 24px;
                    font-weight: 800;
                    color: #0f172a;
                    margin-bottom: 5px;
                    text-transform: uppercase;
                    letter-spacing: 0.5px;
                }
                .report-subtitle {
                    font-size: 14px;
                    color: #64748b;
                    text-transform: uppercase;
                    font-weight: 600;
                    letter-spacing: 1px;
                }
                .meta-label {
                    color: #64748b;
                    font-weight: 600;
                }
                .data-table {
                    width: 100%;
                    border-collapse: collapse;
                    text-align: left;
                    margin-bottom: 30px;
                }
                .data-table th {
                    background-color: #0f172a;
                    color: #ffffff;
                    padding: 12px 10px;
                    font-size: 13px;
                    text-transform: uppercase;
                    font-weight: 600;
                    letter-spacing: 0.5px;
                }
                .data-table td {
                    padding: 12px 10px;
                    border-bottom: 1px solid #e2e8f0;
                    font-size: 14px;
                }
                .row-allocated { background-color: #ffffff; }
                .row-gap { background-color: #fef3c7; }
                .row-shortage { background-color: #fee2e2; }
                
                .skill-pill {
                    display: inline-block;
                    padding: 3px 10px;
                    border-radius: 9999px;
                    font-weight: bold;
                    font-size: 12px;
                }
                .lvl-5 { background: #dcfce7; color: #15803d; }
                .lvl-4 { background: #dbeafe; color: #1d4ed8; }
                .lvl-3 { background: #fef3c7; color: #b45309; }
                .lvl-2 { background: #f3e8ff; color: #7e22ce; }
                .lvl-1 { background: #f5f5f5; color: #555555; }
                .lvl-0 { background: #fee2e2; color: #b91c1c; }

                .status-badge {
                    display: inline-block;
                    padding: 3px 8px;
                    border-radius: 4px;
                    font-size: 11px;
                    text-transform: uppercase;
                    font-weight: bold;
                }
                .state-allocated { background: #dcfce7; color: #15803d; }
                .state-skillgap { background: #fef3c7; color: #d97706; }
                .state-shortage { background: #fee2e2; color: #b91c1c; }

                .stats-grid {
                    display: grid;
                    grid-template-columns: repeat(4, 1fr);
                    gap: 15px;
                    margin-top: 20px;
                    padding-top: 20px;
                    border-top: 2px solid #e2e8f0;
                }
                .stat-box {
                    background: #f1f5f9;
                    padding: 15px;
                    border-radius: 8px;
                    text-align: center;
                }
                .stat-value {
                    font-size: 20px;
                    font-weight: 800;
                    color: #0f172a;
                }
                .stat-label {
                    font-size: 11px;
                    color: #64748b;
                    text-transform: uppercase;
                    font-weight: bold;
                    margin-top: 4px;
                }
                @media print {
                    body { padding: 0; background: #fff; }
                    .report-card { border: none; box-shadow: none; padding: 0; }
                }
            </style>
        </head>
        <body>
            <div class="report-card">
                <table class="header-table">
                    <tr>
                        <td>
                            <div class="company-title">$companyName</div>
                            <div class="report-subtitle">Manpower Allocation Sheet</div>
                        </td>
                        <td align="right" style="vertical-align: bottom;">
                            <span class="meta-label">Date:</span> $date &nbsp;|&nbsp;
                            <span class="meta-label">Shift:</span> Shift $shift
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2" style="border-top: 1px solid #e2e8f0; padding-top: 10px;">
                            <span class="meta-label">Department:</span> $department &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                            <span class="meta-label">Supervisor:</span> $supervisor
                        </td>
                    </tr>
                </table>

                <table class="data-table">
                    <thead>
                        <tr>
                            <th width="8%">Sr No</th>
                            <th width="22%">Machine/Station</th>
                            <th width="10%">Required</th>
                            <th width="25%">Allocated Operator</th>
                            <th width="10%">Skill</th>
                            <th width="12%">Status</th>
                            <th width="13%">Remarks</th>
                        </tr>
                    </thead>
                    <tbody>
                        $tableRows
                    </tbody>
                </table>

                <div class="stats-grid">
                    <div class="stat-box">
                        <div class="stat-value">$totalReq</div>
                        <div class="stat-label">Total Required</div>
                    </div>
                    <div class="stat-box">
                        <div class="stat-value">$totalAlloc</div>
                        <div class="stat-label">Total Allocated</div>
                    </div>
                    <div class="stat-box" style="background-color: ${if (shortage > 0) "#fef2f2" else "#f1f5f9"};">
                        <div class="stat-value" style="color: ${if (shortage > 0) "#dc2626" else "#0f172a"};">$shortage</div>
                        <div class="stat-label">Shortage Gap</div>
                    </div>
                    <div class="stat-box">
                        <div class="stat-value">$utilization%</div>
                        <div class="stat-label">Utilization %</div>
                    </div>
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    /**
     * Shares the exported file using Android standard system Share Sheet.
     */
    fun shareReport(
        context: Context,
        fileName: String,
        fileContent: String,
        mimeType: String = "text/csv"
    ) {
        try {
            val cacheDir = File(context.cacheDir, "reports")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            
            val file = File(cacheDir, fileName)
            file.writeText(fileContent)

            // Get standard content URI via FileProvider
            val authority = "${context.packageName}.fileprovider"
            val fileUri: Uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Manpower Allocation Report")
                putExtra(Intent.EXTRA_TEXT, "Hello, please find attached the manpower allocation report.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = Intent.createChooser(shareIntent, "Export Report via:")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Shares the exported file directly to WhatsApp.
     */
    fun shareToWhatsApp(
        context: Context,
        fileName: String,
        fileContent: String,
        mimeType: String = "text/csv"
    ) {
        try {
            val cacheDir = File(context.cacheDir, "reports")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            
            val file = File(cacheDir, fileName)
            file.writeText(fileContent)

            val authority = "${context.packageName}.fileprovider"
            val fileUri: Uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Manpower Allocation Report")
                putExtra(Intent.EXTRA_TEXT, "Hello, please find attached the manpower allocation report.")
                `package` = "com.whatsapp"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            // Fallback to standard share if WhatsApp package is not installed or has error
            try {
                val cacheDir = File(context.cacheDir, "reports")
                val file = File(cacheDir, fileName)
                val authority = "${context.packageName}.fileprovider"
                val fileUri: Uri = FileProvider.getUriForFile(context, authority, file)
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    putExtra(Intent.EXTRA_SUBJECT, "Manpower Allocation Report")
                    putExtra(Intent.EXTRA_TEXT, "Hello, please find attached the manpower allocation report.")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(fallbackIntent, "Share Report:")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}
