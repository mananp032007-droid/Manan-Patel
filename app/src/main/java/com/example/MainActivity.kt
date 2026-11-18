package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.OpenJarvisViewModel
import com.example.ui.screens.EngineTracesScreen
import com.example.ui.screens.MemoryToolsScreen
import com.example.ui.screens.VoiceAssistantScreen
import com.example.ui.theme.OpenJarvisTheme

const val ROUTE_ASSISTANT = "assistant"
const val ROUTE_MEMORY_TOOLS = "memory_tools"
const val ROUTE_ENGINE_TRACES = "engine_traces"

data class NavDestinationItem(
    val route: String,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val jarvisDestinations = listOf(
    NavDestinationItem(
        route = ROUTE_ASSISTANT,
        labelResId = R.string.nav_assistant,
        selectedIcon = Icons.Rounded.GraphicEq,
        unselectedIcon = Icons.Outlined.GraphicEq,
        testTag = "nav_assistant"
    ),
    NavDestinationItem(
        route = ROUTE_MEMORY_TOOLS,
        labelResId = R.string.nav_memory_tools,
        selectedIcon = Icons.Rounded.Psychology,
        unselectedIcon = Icons.Outlined.Psychology,
        testTag = "nav_memory_tools"
    ),
    NavDestinationItem(
        route = ROUTE_ENGINE_TRACES,
        labelResId = R.string.nav_engine_traces,
        selectedIcon = Icons.Rounded.Dns,
        unselectedIcon = Icons.Outlined.Dns,
        testTag = "nav_engine_traces"
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: OpenJarvisViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            OpenJarvisTheme(darkTheme = uiState.engineConfig.darkThemeEnabled) {
                OpenJarvisAppShell(viewModel = viewModel, uiState = uiState)
            }
        }
    }
}

@Composable
fun OpenJarvisAppShell(
    viewModel: OpenJarvisViewModel,
    uiState: com.example.ui.OpenJarvisUiState
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        jarvisDestinations.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == dest.route
                            } == true
                            val label = stringResource(dest.labelResId)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        jarvisDestinations.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == dest.route
                            } == true
                            val label = stringResource(dest.labelResId)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_ASSISTANT,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_ASSISTANT) {
                        VoiceAssistantScreen(
                            uiState = uiState,
                            onStartVoice = viewModel::startVoiceCapture,
                            onStopVoice = viewModel::stopVoiceCapture,
                            onSendPrompt = viewModel::sendPrompt,
                            onSpeakMessage = viewModel::speakMessage,
                            onStopSpeaking = viewModel::stopSpeaking,
                            onToggleAutoSpeak = viewModel::setAutoSpeak,
                            onClearConversation = viewModel::clearConversation,
                            onDismissError = viewModel::dismissErrorBanner,
                            onNavigateToEngine = { navigateTo(ROUTE_ENGINE_TRACES) }
                        )
                    }
                    composable(ROUTE_MEMORY_TOOLS) {
                        MemoryToolsScreen(
                            uiState = uiState,
                            onAddMemory = viewModel::addMemory,
                            onDeleteMemory = viewModel::deleteMemory,
                            onAddTask = viewModel::addTask,
                            onToggleTask = viewModel::toggleTask,
                            onDeleteTask = viewModel::deleteTask,
                            onRunToolCommand = { cmd ->
                                viewModel.sendPrompt(cmd)
                                navigateTo(ROUTE_ASSISTANT)
                            }
                        )
                    }
                    composable(ROUTE_ENGINE_TRACES) {
                        EngineTracesScreen(
                            uiState = uiState,
                            onSelectEngineMode = viewModel::setEngineMode,
                            onSaveServerUrl = viewModel::setServerBaseUrl,
                            onProbeServer = viewModel::probeLocalServer,
                            onSelectModel = viewModel::setSelectedModel,
                            onTemperatureChange = viewModel::setTemperature,
                            onToggleToolRouting = viewModel::setToolRouting,
                            onToggleAutoSpeak = viewModel::setAutoSpeak,
                            onToggleDarkTheme = viewModel::setDarkTheme,
                            onClearTraces = viewModel::clearTraces
                        )
                    }
                }
            }
        }
    }
}
