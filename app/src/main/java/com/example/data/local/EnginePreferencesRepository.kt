package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.EngineConfig
import com.example.domain.EngineMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.engineDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "openjarvis_engine_prefs"
)

class EnginePreferencesRepository(private val context: Context) {

    private object Keys {
        val ENGINE_MODE = stringPreferencesKey("engine_mode")
        val SERVER_BASE_URL = stringPreferencesKey("server_base_url")
        val SELECTED_MODEL_ID = stringPreferencesKey("selected_model_id")
        val TEMPERATURE = floatPreferencesKey("temperature")
        val MAX_TOKENS = intPreferencesKey("max_tokens")
        val AUTO_SPEAK = booleanPreferencesKey("auto_speak")
        val TOOL_ROUTING = booleanPreferencesKey("tool_routing")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
    }

    val configFlow: Flow<EngineConfig> = context.engineDataStore.data.map { prefs ->
        val modeName = prefs[Keys.ENGINE_MODE] ?: EngineMode.CLOUD_PLACEHOLDER.name
        val mode = EngineMode.entries.firstOrNull { it.name == modeName }
            ?: EngineMode.CLOUD_PLACEHOLDER

        EngineConfig(
            engineMode = mode,
            serverBaseUrl = prefs[Keys.SERVER_BASE_URL] ?: "http://10.0.2.2:8000/v1/",
            selectedModelId = prefs[Keys.SELECTED_MODEL_ID] ?: "gemini-3.8-flash",
            temperature = prefs[Keys.TEMPERATURE] ?: 0.6f,
            maxTokens = prefs[Keys.MAX_TOKENS] ?: 1024,
            autoSpeakEnabled = prefs[Keys.AUTO_SPEAK] ?: true,
            toolRoutingEnabled = prefs[Keys.TOOL_ROUTING] ?: true,
            darkThemeEnabled = prefs[Keys.DARK_THEME] ?: true
        )
    }

    suspend fun setEngineMode(mode: EngineMode) {
        context.engineDataStore.edit { prefs ->
            prefs[Keys.ENGINE_MODE] = mode.name
            if (mode == EngineMode.OPENJARVIS_SERVER &&
                prefs[Keys.SELECTED_MODEL_ID] == "gemini-3.8-flash"
            ) {
                prefs[Keys.SELECTED_MODEL_ID] = "qwen2.5:7b-instruct"
            } else if (mode == EngineMode.CLOUD_PLACEHOLDER) {
                prefs[Keys.SELECTED_MODEL_ID] = "gemini-3.8-flash"
            }
        }
    }

    suspend fun setServerBaseUrl(url: String) {
        val normalized = normalizeBaseUrl(url)
        context.engineDataStore.edit { it[Keys.SERVER_BASE_URL] = normalized }
    }

    suspend fun setSelectedModel(modelId: String) {
        context.engineDataStore.edit { it[Keys.SELECTED_MODEL_ID] = modelId }
    }

    suspend fun setTemperature(temperature: Float) {
        context.engineDataStore.edit { it[Keys.TEMPERATURE] = temperature.coerceIn(0f, 1.5f) }
    }

    suspend fun setAutoSpeak(enabled: Boolean) {
        context.engineDataStore.edit { it[Keys.AUTO_SPEAK] = enabled }
    }

    suspend fun setToolRouting(enabled: Boolean) {
        context.engineDataStore.edit { it[Keys.TOOL_ROUTING] = enabled }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.engineDataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    companion object {
        fun normalizeBaseUrl(raw: String): String {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return "http://10.0.2.2:8000/v1/"
            val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                trimmed
            } else {
                "http://$trimmed"
            }
            val noTrailing = withScheme.trimEnd('/')
            return if (noTrailing.endsWith("/v1")) {
                "$noTrailing/"
            } else {
                "$noTrailing/v1/"
            }
        }
    }
}
