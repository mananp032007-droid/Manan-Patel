package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.TraceEntity
import com.example.domain.BuiltinModelsCatalog
import com.example.domain.EngineMode
import com.example.domain.ServerProbeState
import com.example.ui.OpenJarvisUiState
import com.example.ui.components.FirebaseNoticeBanner
import com.example.ui.components.MetricStatCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EngineTracesScreen(
    uiState: OpenJarvisUiState,
    onSelectEngineMode: (EngineMode) -> Unit,
    onSaveServerUrl: (String) -> Unit,
    onProbeServer: (String) -> Unit,
    onSelectModel: (String) -> Unit,
    onTemperatureChange: (Float) -> Unit,
    onToggleToolRouting: (Boolean) -> Unit,
    onToggleAutoSpeak: (Boolean) -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    onClearTraces: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config = uiState.engineConfig
    var serverUrlDraft by rememberSaveable(config.serverBaseUrl) {
        mutableStateOf(config.serverBaseUrl)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Engine & Learning Traces",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Configure OpenJarvis Intelligence, local jarvis serve / Ollama endpoints, and trace telemetry.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (config.engineMode == EngineMode.CLOUD_PLACEHOLDER) {
                item {
                    FirebaseNoticeBanner(onOpenEngineSettings = {})
                }
            }

            // Engine Mode Selector
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Inference Engine",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = config.engineMode == EngineMode.CLOUD_PLACEHOLDER,
                                onClick = { onSelectEngineMode(EngineMode.CLOUD_PLACEHOLDER) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.CloudOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text("Sample Cloud AI") },
                                modifier = Modifier.testTag("engine_mode_cloud")
                            )
                            FilterChip(
                                selected = config.engineMode == EngineMode.OPENJARVIS_SERVER,
                                onClick = { onSelectEngineMode(EngineMode.OPENJARVIS_SERVER) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Dns,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text("jarvis serve (Local)") },
                                modifier = Modifier.testTag("engine_mode_local")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "OpenAI-Compatible Server URL (jarvis serve / Ollama / vLLM)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = serverUrlDraft,
                            onValueChange = { serverUrlDraft = it },
                            singleLine = true,
                            placeholder = { Text("http://10.0.2.2:8000/v1/") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("server_url_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onSaveServerUrl(serverUrlDraft) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text("Save URL")
                            }
                            Button(
                                onClick = { onProbeServer(serverUrlDraft) },
                                enabled = uiState.serverProbeState !is ServerProbeState.Probing,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("probe_server_button")
                            ) {
                                if (uiState.serverProbeState is ServerProbeState.Probing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text("Test Connection")
                            }
                        }

                        when (val probe = uiState.serverProbeState) {
                            is ServerProbeState.Healthy -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Connected (${probe.latencyMs} ms) · ${probe.discoveredModels.size} model(s): ${probe.discoveredModels.take(3).joinToString()}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                            is ServerProbeState.Unreachable -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = probe.reason,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            else -> Unit
                        }
                    }
                }
            }

            // Intelligence Catalog
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Intelligence Catalog (BUILTIN_MODELS)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BuiltinModelsCatalog.models.forEach { spec ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = config.selectedModelId == spec.id,
                                    onClick = { onSelectModel(spec.id) }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = spec.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${spec.engineTag} · ${spec.parameters} · ${spec.contextWindow}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Parameters & Toggles
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Agent & Voice Configuration",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Temperature: ${"%.2f".format(config.temperature)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Slider(
                            value = config.temperature,
                            onValueChange = onTemperatureChange,
                            valueRange = 0f..1.2f
                        )
                        SettingSwitchRow(
                            title = "Tool-Using Agent Routing",
                            subtitle = "Automatically run local calculator, memory, tasks & clock tools",
                            checked = config.toolRoutingEnabled,
                            onCheckedChange = onToggleToolRouting
                        )
                        SettingSwitchRow(
                            title = "Auto-Speak Voice Replies (TTS)",
                            subtitle = "Speak assistant replies aloud automatically",
                            checked = config.autoSpeakEnabled,
                            onCheckedChange = onToggleAutoSpeak
                        )
                        SettingSwitchRow(
                            title = "Obsidian Dark Theme",
                            subtitle = "Toggle between Obsidian Neural dark and Daylight light mode",
                            checked = config.darkThemeEnabled,
                            onCheckedChange = onToggleDarkTheme
                        )
                    }
                }
            }

            // Trace-Driven Learning Telemetry Metrics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Trace-Driven Telemetry",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (uiState.traces.isNotEmpty()) {
                        IconButton(
                            onClick = onClearTraces,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = "Clear traces",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricStatCard(
                        label = "TRACES",
                        value = uiState.traceMetrics.totalTraces.toString(),
                        icon = Icons.Rounded.Timeline,
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        label = "AVG LATENCY",
                        value = "${uiState.traceMetrics.avgLatencyMs}ms",
                        icon = Icons.Rounded.Speed,
                        accent = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricStatCard(
                        label = "TOOL HIT RATE",
                        value = "${uiState.traceMetrics.toolHitPercent}%",
                        icon = Icons.Rounded.Build,
                        accent = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        label = "LAST STATUS",
                        value = uiState.traceMetrics.lastStatus,
                        icon = Icons.Rounded.Bolt,
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (uiState.traces.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.empty_traces),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(uiState.traces, key = { "trace_${it.id}" }) { trace ->
                    TraceLogCard(trace = trace)
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun TraceLogCard(trace: TraceEntity) {
    val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val statusColor = when (trace.status) {
        "OK" -> MaterialTheme.colorScheme.tertiary
        "PLACEHOLDER" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Memory,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${trace.engineName} · ${trace.modelName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor
                    )
                }
                Text(
                    text = "${trace.latencyMs} ms · ${timeFmt.format(Date(trace.timestamp))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = trace.promptPreview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "tools=[${trace.toolsInvoked}] · ~${trace.estimatedTokens} tok · status=${trace.status}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
