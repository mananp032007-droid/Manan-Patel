package com.example.data.repository

import com.example.data.local.MemoryDao
import com.example.data.local.MemoryEntity
import com.example.data.local.MessageDao
import com.example.data.local.MessageEntity
import com.example.data.local.TaskDao
import com.example.data.local.TaskEntity
import com.example.data.local.TraceDao
import com.example.data.local.TraceEntity
import kotlinx.coroutines.flow.Flow

class JarvisWorkspaceRepository(
    private val messageDao: MessageDao,
    private val memoryDao: MemoryDao,
    private val taskDao: TaskDao,
    private val traceDao: TraceDao
) {
    val messages: Flow<List<MessageEntity>> = messageDao.observeAllMessages()
    val memories: Flow<List<MemoryEntity>> = memoryDao.observeAllMemories()
    val tasks: Flow<List<TaskEntity>> = taskDao.observeAllTasks()
    val traces: Flow<List<TraceEntity>> = traceDao.observeRecentTraces()

    suspend fun addMessage(message: MessageEntity): Long = messageDao.insertMessage(message)

    suspend fun clearConversation() = messageDao.clearAll()

    suspend fun addMemory(category: String, fact: String) {
        if (fact.isNotBlank()) {
            memoryDao.insertMemory(MemoryEntity(category = category.ifBlank { "Personal" }, fact = fact.trim()))
        }
    }

    suspend fun deleteMemory(id: Long) = memoryDao.deleteMemory(id)

    suspend fun getMemoriesSnapshot(): List<MemoryEntity> = memoryDao.getAllMemoriesOnce()

    suspend fun addTask(title: String) {
        if (title.isNotBlank()) {
            taskDao.insertTask(TaskEntity(title = title.trim()))
        }
    }

    suspend fun toggleTask(id: Long, completed: Boolean) = taskDao.setTaskCompleted(id, completed)

    suspend fun deleteTask(id: Long) = taskDao.deleteTask(id)

    suspend fun recordTrace(trace: TraceEntity) = traceDao.insertTrace(trace)

    suspend fun clearTraces() = traceDao.clearTraces()
}
