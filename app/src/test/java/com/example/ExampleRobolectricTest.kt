package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.EnginePreferencesRepository
import com.example.data.local.OpenJarvisDatabase
import com.example.domain.LocalToolExecutor
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: OpenJarvisDatabase
    private lateinit var toolExecutor: LocalToolExecutor

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, OpenJarvisDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        toolExecutor = LocalToolExecutor(db.memoryDao(), db.taskDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `app_name is OpenJarvis`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("OpenJarvis", context.getString(R.string.app_name))
    }

    @Test
    fun `local tools persist memory and tasks and evaluate math`() = runTest {
        val memOutcome = toolExecutor.evaluateAndExecute("Remember that my preferred roast is Ethiopian")
        assertTrue(memOutcome.toolRecords.any { it.toolName == "memory_store" })
        assertEquals(1, db.memoryDao().getAllMemoriesOnce().size)

        val taskOutcome = toolExecutor.evaluateAndExecute("Add task review OpenJarvis logs")
        assertTrue(taskOutcome.toolRecords.any { it.toolName == "task_manager" })
        assertEquals(1, db.taskDao().getAllTasksOnce().size)

        val calcOutcome = toolExecutor.evaluateAndExecute("Calculate (125 * 4) + 18")
        val calcRecord = calcOutcome.toolRecords.first { it.toolName == "calculator" }
        assertTrue(calcRecord.summary.contains("518"))
    }

    @Test
    fun `server url normalization appends v1 slash`() {
        assertEquals(
            "http://10.0.2.2:8000/v1/",
            EnginePreferencesRepository.normalizeBaseUrl("10.0.2.2:8000")
        )
        assertEquals(
            "http://192.168.1.50:11434/v1/",
            EnginePreferencesRepository.normalizeBaseUrl("http://192.168.1.50:11434/v1")
        )
    }
}
