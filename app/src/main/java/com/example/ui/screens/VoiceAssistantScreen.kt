package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.local.MessageEntity
import com.example.domain.EngineMode
import com.example.domain.VoiceUiState
import com.example.ui.OpenJarvisUiState
import com.example.ui.components.EngineBadgeChip
import com.example.ui.components.FirebaseNoticeBanner
import com.example.ui.components.NeuralVoiceOrb
import com.example.ui.components.ToolExecutionChip

@Composable
fun VoiceAssistantScreen(
    uiState: OpenJarvisUiState,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onSendPrompt: (String) -> Unit,
    onSpeakMessage: (Long, String) -> Unit,
    onStopSpeaking: () -> Unit,
    onToggleAutoSpeak: (Boolean) -> Unit,
    onClearConversation: () -> Unit,
    onDismissError: () -> Unit,
    onNavigateToEngine: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var draftPrompt by rememberSaveable { mutableStateOf("") }
    var permissionDeniedBanner by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDeniedBanner = false
            onStartVoice()
        } else {
            permissionDeniedBanner = true
        }
    }

    val handleMicTrigger = {
        when (uiState.voiceState) {
            is VoiceUiState.Listening -> onStopVoice()
            is VoiceUiState.Speaking -> onStopSpeaking()
            is VoiceUiState.Thinking -> Unit
            else -> {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) {
                    permissionDeniedBanner = false
                    onStartVoice()
                } else {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .imePadding()
        ) {
            // Top Studio Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    EngineBadgeChip(
                        engineMode = uiState.engineConfig.engineMode,
                        modelId = uiState.engineConfig.selectedModelId,
                        onClick = onNavigateToEngine,
                        modifier = Modifier.testTag("engine_badge_chip")
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onToggleAutoSpeak(!uiState.engineConfig.autoSpeakEnabled) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("auto_speak_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.engineConfig.autoSpeakEnabled) {
                                Icons.AutoMirrored.Rounded.VolumeUp
                            } else {
                                Icons.AutoMirrored.Rounded.VolumeOff
                            },
                            contentDescription = if (uiState.engineConfig.autoSpeakEnabled) {
                                "Disable auto-speak"
                            } else {
                                "Enable auto-speak"
                            },
                            tint = if (uiState.engineConfig.autoSpeakEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    if (uiState.messages.isNotEmpty()) {
                        IconButton(
                            onClick = onClearConversation,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("clear_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = "Clear conversation",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Firebase Notice when in Cloud Sample Mode
            AnimatedVisibility(visible = uiState.engineConfig.engineMode == EngineMode.CLOUD_PLACEHOLDER) {
                FirebaseNoticeBanner(
                    onOpenEngineSettings = onNavigateToEngine,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Permission or Runtime Error Banner
            val activeBanner = if (permissionDeniedBanner) {
                stringResource(R.string.mic_permission_needed)
            } else {
                uiState.errorBanner
            }
            AnimatedVisibility(visible = !activeBanner.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = activeBanner.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                permissionDeniedBanner = false
                                onDismissError()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Dismiss error",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Neural Voice Orb Hero
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NeuralVoiceOrb(
                    voiceState = uiState.voiceState,
                    onClick = handleMicTrigger,
                    orbSize = if (uiState.messages.isEmpty()) 164.dp else 118.dp
                )

                Spacer(modifier = Modifier.height(6.dp))

                val statusText = when (val vs = uiState.voiceState) {
                    is VoiceUiState.Listening -> {
                        if (vs.partialTranscript.isNotBlank()) {
                            "\"${vs.partialTranscript}\""
                        } else {
                            stringResource(R.string.voice_listening)
                        }
                    }
                    is VoiceUiState.Thinking -> stringResource(R.string.voice_thinking)
                    is VoiceUiState.Speaking -> stringResource(R.string.voice_speaking)
                    is VoiceUiState.Error -> vs.message
                    is VoiceUiState.Idle -> stringResource(R.string.voice_tap_to_speak)
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelLarge,
                    color = when (uiState.voiceState) {
                        is VoiceUiState.Listening -> MaterialTheme.colorScheme.primary
                        is VoiceUiState.Thinking -> MaterialTheme.colorScheme.secondary
                        is VoiceUiState.Speaking -> MaterialTheme.colorScheme.tertiary
                        is VoiceUiState.Error -> MaterialTheme.colorScheme.error
                        is VoiceUiState.Idle -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            // Conversation Thread or Quick Prompts Empty State
            if (uiState.messages.isEmpty()) {
                EmptyAssistantStage(
                    onPickPrompt = onSendPrompt,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { msg ->
                        MessageBubbleCard(
                            message = msg,
                            isCurrentlySpeaking = uiState.speakingMessageId == msg.id,
                            onSpeakClick = { onSpeakMessage(msg.id, msg.content) },
                            onStopSpeakClick = onStopSpeaking
                        )
                    }
                }
            }

            // Quick Prompt Strip when messages exist
            if (uiState.messages.isNotEmpty()) {
                QuickPromptsRow(
                    onPickPrompt = onSendPrompt,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            // Bottom Input Dock
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = draftPrompt,
                        onValueChange = { draftPrompt = it },
                        placeholder = {
                            Text(
                                text = "Ask OpenJarvis or run a tool…",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (draftPrompt.isNotBlank() && !uiState.isInferring) {
                                    val text = draftPrompt
                                    draftPrompt = ""
                                    onSendPrompt(text)
                                }
                            }
                        ),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("prompt_input_field")
                    )

                    if (draftPrompt.isNotBlank()) {
                        FilledIconButton(
                            onClick = {
                                val text = draftPrompt
                                draftPrompt = ""
                                onSendPrompt(text)
                            },
                            enabled = !uiState.isInferring,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("send_prompt_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "Send message"
                            )
                        }
                    }

                    val isListening = uiState.voiceState is VoiceUiState.Listening
                    FilledIconButton(
                        onClick = handleMicTrigger,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isListening) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                            contentColor = if (isListening) {
                                MaterialTheme.colorScheme.onError
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("mic_action_button")
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                            contentDescription = if (isListening) "Stop listening" else "Start voice command"
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MessageBubbleCard(
    message: MessageEntity,
    isCurrentlySpeaking: Boolean,
    onSpeakClick: () -> Unit,
    onStopSpeakClick: () -> Unit
) {
    val isUser = message.role == "user"
    val isError = message.role == "system_error"

    val containerColor = when {
        isUser -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
        isError -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val borderColor = when {
        isUser -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        isError -> MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 520.dp),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            color = containerColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                val tools = message.toolsUsed.split(",").filter { it.isNotBlank() }
                if (tools.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        tools.forEach { toolName ->
                            ToolExecutionChip(toolName = toolName)
                        }
                    }
                }

                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isError) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                if (!isUser && !isError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${message.engineLabel} · ${message.latencyMs} ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(
                            onClick = if (isCurrentlySpeaking) onStopSpeakClick else onSpeakClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isCurrentlySpeaking) {
                                    Icons.Rounded.Stop
                                } else {
                                    Icons.AutoMirrored.Rounded.VolumeUp
                                },
                                contentDescription = if (isCurrentlySpeaking) "Stop speech" else "Read aloud",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyAssistantStage(
    onPickPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val suggestions = listOf(
        "Remember I drink oat cortados",
        "Calculate 18% tip on 84.50",
        "Add task review local server setup",
        "What time is it?",
        "What do you remember?"
    )

    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.empty_chat_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.empty_chat_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { sample ->
                AssistChip(
                    onClick = { onPickPrompt(sample) },
                    label = {
                        Text(
                            text = sample,
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun QuickPromptsRow(
    onPickPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickItems = listOf(
        "List my tasks",
        "What do you remember?",
        "Check diagnostics",
        "What time is it?"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        quickItems.forEach { item ->
            AssistChip(
                onClick = { onPickPrompt(item) },
                label = {
                    Text(
                        text = item,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}
