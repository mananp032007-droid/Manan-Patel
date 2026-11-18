package com.example.domain

import com.example.data.local.MemoryDao
import com.example.data.local.MemoryEntity
import com.example.data.local.TaskDao
import com.example.data.local.TaskEntity
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.pow

data class LocalToolOutcome(
    val toolRecords: List<ToolExecutionRecord>,
    val contextNotes: List<String>
)

class LocalToolExecutor(
    private val memoryDao: MemoryDao,
    private val taskDao: TaskDao
) {
    suspend fun evaluateAndExecute(prompt: String): LocalToolOutcome {
        val trimmed = prompt.trim()
        val lower = trimmed.lowercase(Locale.US)
        val records = mutableListOf<ToolExecutionRecord>()
        val notes = mutableListOf<String>()

        // 1. Memory Store Tool
        val rememberMatch = Regex(
            pattern = "^(?:please\\s+)?(?:remember(?:\\s+that)?|save\\s+to\\s+memory(?:\\s+that)?|memorize(?:\\s+that)?)\\s+(.+)$",
            options = setOf(RegexOption.IGNORE_CASE)
        ).find(trimmed)
        if (rememberMatch != null) {
            val fact = rememberMatch.groupValues[1].trim().removeSuffix(".")
            if (fact.isNotBlank()) {
                val category = inferCategory(fact)
                memoryDao.insertMemory(MemoryEntity(category = category, fact = fact))
                records += ToolExecutionRecord(
                    toolName = "memory_store",
                    summary = "Saved [$category]: \"$fact\""
                )
                notes += "Saved to persistent local memory ($category): \"$fact\"."
            }
        }

        // 2. Memory Recall Tool
        val recallKeywords = listOf(
            "what do you remember",
            "recall",
            "my memory",
            "my memories",
            "what did i tell you",
            "what is my "
        )
        if (recallKeywords.any { lower.contains(it) }) {
            val memories = memoryDao.getAllMemoriesOnce()
            records += ToolExecutionRecord(
                toolName = "memory_recall",
                summary = "Retrieved ${memories.size} local memory item(s)"
            )
            if (memories.isEmpty()) {
                notes += "Persistent memory is currently empty."
            } else {
                val formatted = memories.take(6).joinToString("; ") { "[${it.category}] ${it.fact}" }
                notes += "Stored memories (${memories.size}): $formatted."
            }
        }

        // 3. Task Manager Tool (Add / Complete / List)
        val addTaskMatch = Regex(
            pattern = "^(?:please\\s+)?(?:add\\s+(?:a\\s+)?task(?:\\s+to)?|remind\\s+me\\s+to|create\\s+(?:a\\s+)?reminder(?:\\s+to)?|todo:?\\s*)\\s+(.+)$",
            options = setOf(RegexOption.IGNORE_CASE)
        ).find(trimmed)
        if (addTaskMatch != null) {
            val taskTitle = addTaskMatch.groupValues[1].trim().removeSuffix(".")
            if (taskTitle.isNotBlank()) {
                taskDao.insertTask(TaskEntity(title = taskTitle))
                records += ToolExecutionRecord(
                    toolName = "task_manager",
                    summary = "Added task: \"$taskTitle\""
                )
                notes += "Added task to your local workspace: \"$taskTitle\"."
            }
        } else if (lower.contains("task") || lower.contains("todo") || lower.contains("reminder")) {
            val completeMatch = Regex(
                pattern = "(?:complete|finish|done|check off)\\s+(?:task\\s+)?(.+)",
                options = setOf(RegexOption.IGNORE_CASE)
            ).find(trimmed)
            val tasks = taskDao.getAllTasksOnce()
            if (completeMatch != null && tasks.isNotEmpty()) {
                val query = completeMatch.groupValues[1].trim().lowercase(Locale.US)
                val matched = tasks.firstOrNull { !it.isCompleted && it.title.lowercase(Locale.US).contains(query) }
                if (matched != null) {
                    taskDao.setTaskCompleted(matched.id, true)
                    records += ToolExecutionRecord(
                        toolName = "task_manager",
                        summary = "Completed \"${matched.title}\""
                    )
                    notes += "Marked task \"${matched.title}\" as completed."
                } else {
                    records += ToolExecutionRecord(
                        toolName = "task_manager",
                        summary = "Checked ${tasks.size} task(s)"
                    )
                }
            } else {
                val openTasks = tasks.filter { !it.isCompleted }
                records += ToolExecutionRecord(
                    toolName = "task_manager",
                    summary = "Listed ${openTasks.size} open task(s)"
                )
                notes += if (openTasks.isEmpty()) {
                    "You have no open tasks."
                } else {
                    "Open tasks (${openTasks.size}): ${openTasks.take(6).joinToString("; ") { it.title }}."
                }
            }
        }

        // 4. Calculator Tool
        val calcResult = tryEvaluateMath(trimmed)
        if (calcResult != null) {
            records += ToolExecutionRecord(
                toolName = "calculator",
                summary = "${calcResult.first} = ${calcResult.second}"
            )
            notes += "Calculator result: ${calcResult.first} = ${calcResult.second}."
        }

        // 5. System Clock Tool
        if (Regex("\\b(what time|current time|what's the time|what day|today's date|current date)\\b").containsMatchIn(lower)) {
            val now = Date()
            val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
            val dateFmt = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
            val tz = TimeZone.getDefault().id
            val timeStr = timeFmt.format(now)
            val dateStr = dateFmt.format(now)
            records += ToolExecutionRecord(
                toolName = "system_clock",
                summary = "$timeStr ($tz)"
            )
            notes += "Current local time is $timeStr on $dateStr ($tz)."
        }

        // 6. Diagnostics / Health Tool
        if (lower.contains("health") || lower.contains("diagnostic") || lower.contains("status")) {
            val memoryCount = memoryDao.getAllMemoriesOnce().size
            val taskCount = taskDao.getAllTasksOnce().count { !it.isCompleted }
            records += ToolExecutionRecord(
                toolName = "engine_diagnostics",
                summary = "$memoryCount memories, $taskCount open tasks"
            )
            notes += "On-device diagnostics: $memoryCount stored memory item(s), $taskCount open task(s)."
        }

        return LocalToolOutcome(toolRecords = records, contextNotes = notes)
    }

    private fun inferCategory(fact: String): String {
        val lower = fact.lowercase(Locale.US)
        return when {
            lower.contains("coffee") || lower.contains("food") || lower.contains("like") || lower.contains("prefer") -> "Preference"
            lower.contains("work") || lower.contains("project") || lower.contains("meeting") || lower.contains("server") -> "Work"
            lower.contains("wifi") || lower.contains("ip") || lower.contains("port") || lower.contains("model") -> "System"
            else -> "Personal"
        }
    }

    fun tryEvaluateMath(input: String): Pair<String, String>? {
        val lower = input.lowercase(Locale.US)

        // Handle "18% tip on 84.50" or "18% of 84.50"
        val percentMatch = Regex(
            "(\\d+(?:\\.\\d+)?)\\s*%\\s*(?:tip\\s+on|of)\\s*\\$?\\s*(\\d+(?:\\.\\d+)?)"
        ).find(lower)
        if (percentMatch != null) {
            val pct = percentMatch.groupValues[1].toDoubleOrNull() ?: return null
            val base = percentMatch.groupValues[2].toDoubleOrNull() ?: return null
            val part = base * (pct / 100.0)
            val fmt = DecimalFormat("#,##0.##")
            val summary = if (lower.contains("tip")) {
                "${fmt.format(part)} (total ${fmt.format(base + part)})"
            } else {
                fmt.format(part)
            }
            return "$pct% of ${fmt.format(base)}" to summary
        }

        // Extract explicit arithmetic expression (supports leading parentheses like "(125 * 4) + 18")
        val sanitized = input.replace("×", "*").replace("÷", "/")
        val exprMatch = Regex("[0-9(][0-9().+\\-*/^\\s]*[0-9)]")
            .findAll(sanitized)
            .map { it.value.trim() }
            .firstOrNull { candidate -> candidate.any { it in "+-*/^" } }
            ?: return null

        val expr = exprMatch
        return try {
            val value = ExpressionParser(expr).parse()
            if (value.isNaN() || value.isInfinite()) return null
            val fmt = DecimalFormat("#,##0.######")
            expr to fmt.format(value)
        } catch (_: Exception) {
            null
        }
    }

    private class ExpressionParser(private val str: String) {
        private var pos = -1
        private var ch = 0

        private fun nextChar() {
            ch = if (++pos < str.length) str[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < str.length) throw IllegalArgumentException("Unexpected character")
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm()
                    eat('-'.code) -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor()
                    eat('/'.code) -> x /= parseFactor()
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()
            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                if (!eat(')'.code)) throw IllegalArgumentException("Missing ')'")
            } else if (ch in '0'.code..'9'.code || ch == '.'.code) {
                while (ch in '0'.code..'9'.code || ch == '.'.code) nextChar()
                x = str.substring(startPos, pos).toDouble()
            } else {
                throw IllegalArgumentException("Unexpected token")
            }
            if (eat('^'.code)) x = x.pow(parseFactor())
            return x
        }
    }
}
