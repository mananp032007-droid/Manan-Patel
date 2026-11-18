package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.VoiceUiState
import com.example.ui.theme.JarvisEmerald
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisRose
import com.example.ui.theme.NeuralCyan
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NeuralVoiceOrb(
    voiceState: VoiceUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    orbSize: Dp = 152.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_orb_transition")

    val idleBreath by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_breath"
    )

    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (voiceState) {
                    is VoiceUiState.Thinking -> 1400
                    is VoiceUiState.Listening -> 3600
                    else -> 8000
                },
                easing = LinearEasing
            )
        ),
        label = "sweep_angle"
    )

    val phaseShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing)
        ),
        label = "wave_phase"
    )

    val targetRms = when (voiceState) {
        is VoiceUiState.Listening -> voiceState.rmsLevel.coerceIn(0.1f, 1f)
        is VoiceUiState.Speaking -> 0.55f
        is VoiceUiState.Thinking -> 0.4f
        else -> 0.08f
    }
    val smoothRms by animateFloatAsState(
        targetValue = targetRms,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 350f),
        label = "smooth_rms"
    )

    val primaryAccent = when (voiceState) {
        is VoiceUiState.Listening -> NeuralCyan
        is VoiceUiState.Thinking -> JarvisGold
        is VoiceUiState.Speaking -> JarvisEmerald
        is VoiceUiState.Error -> JarvisRose
        is VoiceUiState.Idle -> MaterialTheme.colorScheme.primary
    }
    val secondaryAccent = when (voiceState) {
        is VoiceUiState.Thinking -> NeuralCyan
        is VoiceUiState.Listening -> JarvisGold
        else -> MaterialTheme.colorScheme.secondary
    }

    val a11yLabel = when (voiceState) {
        is VoiceUiState.Listening -> "Listening. Tap to stop."
        is VoiceUiState.Thinking -> "OpenJarvis is thinking."
        is VoiceUiState.Speaking -> "Speaking response. Tap to stop."
        is VoiceUiState.Error -> "Voice error. Tap to retry."
        is VoiceUiState.Idle -> "Tap to speak to OpenJarvis."
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(orbSize)
            .clip(CircleShape)
            .testTag("voice_orb_button")
            .semantics { contentDescription = a11yLabel }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = primaryAccent),
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension * 0.34f

            // Ambient radial halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryAccent.copy(alpha = 0.28f + smoothRms * 0.22f),
                        secondaryAccent.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.5f
                ),
                center = center,
                radius = size.minDimension * 0.5f
            )

            // Outer harmonic acoustic ring
            val outerRingRadius = baseRadius * (1.22f + smoothRms * 0.24f) * idleBreath
            val wavePath = Path()
            val segments = 72
            for (i in 0..segments) {
                val theta = (i.toFloat() / segments) * (2f * PI.toFloat())
                val amplitude = if (voiceState is VoiceUiState.Listening || voiceState is VoiceUiState.Speaking) {
                    (4.dp.toPx() + smoothRms * 10.dp.toPx()) * sin(theta * 6f + phaseShift)
                } else {
                    1.8.dp.toPx() * sin(theta * 4f + phaseShift)
                }
                val r = outerRingRadius + amplitude
                val x = center.x + r * cos(theta)
                val y = center.y + r * sin(theta)
                if (i == 0) wavePath.moveTo(x, y) else wavePath.lineTo(x, y)
            }
            wavePath.close()
            drawPath(
                path = wavePath,
                color = primaryAccent.copy(alpha = 0.65f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Rotating orbital arc
            rotate(degrees = sweepAngle, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            secondaryAccent.copy(alpha = 0.85f),
                            primaryAccent,
                            Color.Transparent
                        ),
                        center = center
                    ),
                    startAngle = 0f,
                    sweepAngle = 210f,
                    useCenter = false,
                    topLeft = Offset(center.x - baseRadius * 1.12f, center.y - baseRadius * 1.12f),
                    size = androidx.compose.ui.geometry.Size(
                        baseRadius * 2.24f,
                        baseRadius * 2.24f
                    ),
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Inner glowing core
            val coreRadius = baseRadius * (0.86f + smoothRms * 0.14f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryAccent.copy(alpha = 0.36f),
                        primaryAccent.copy(alpha = 0.16f)
                    ),
                    center = center,
                    radius = coreRadius
                ),
                center = center,
                radius = coreRadius
            )
            drawCircle(
                color = primaryAccent.copy(alpha = 0.85f),
                center = center,
                radius = coreRadius,
                style = Stroke(width = 1.75.dp.toPx())
            )
        }

        val icon = when (voiceState) {
            is VoiceUiState.Listening -> Icons.Rounded.GraphicEq
            is VoiceUiState.Thinking -> Icons.Rounded.AutoAwesome
            is VoiceUiState.Speaking -> Icons.AutoMirrored.Rounded.VolumeUp
            else -> Icons.Rounded.Mic
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = primaryAccent,
            modifier = Modifier.size(34.dp)
        )
    }
}
