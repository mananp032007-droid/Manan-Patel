package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String, // "user" | "assistant" | "system_error"
    val content: String,
    val toolsUsed: String = "", // comma-separated tool names
    val engineLabel: String = "",
    val latencyMs: Long = 0L,
    val isPlaceholder: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val fact: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "traces")
data class TraceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val promptPreview: String,
    val engineName: String,
    val modelName: String,
    val toolsInvoked: String,
    val latencyMs: Long,
    val estimatedTokens: Int,
    val status: String, // "OK" | "PLACEHOLDER" | "ERROR"
    val timestamp: Long = System.currentTimeMillis()
)
