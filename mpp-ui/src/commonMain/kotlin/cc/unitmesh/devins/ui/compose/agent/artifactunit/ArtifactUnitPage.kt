package cc.unitmesh.devins.ui.compose.agent.artifactunit

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cc.unitmesh.agent.Platform
import cc.unitmesh.devins.ui.base.ResizableSplitPane
import cc.unitmesh.devins.ui.compose.agent.AgentTopAppBar
import cc.unitmesh.devins.ui.compose.agent.AgentTopAppBarActions
import cc.unitmesh.devins.workspace.Workspace
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.flow.collectLatest

/**
 * Artifact Unit Page - Main page for creating reversible executable artifacts
 *
 * Left side: Chat interface for code generation
 * Right side: Artifact workbench (Source Code / Run Preview / Binary Metadata tabs)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtifactUnitPage(
    workspace: Workspace? = null,
    llmService: KoogLLMService?,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onNotification: (String, String) -> Unit = { _, _ -> }
) {
    val viewModel = remember { ArtifactUnitViewModel(workspace) }
    val state = viewModel.state

    // Collect notifications
    LaunchedEffect(viewModel) {
        viewModel.notificationEvent.collectLatest { (title, message) ->
            onNotification(title, message)
        }
    }

    // Cleanup on dispose
    DisposableEffect(viewModel) {
        onDispose {
            viewModel.dispose()
        }
    }

    val notMobile = (Platform.isAndroid || Platform.isIOS).not()
    Scaffold(
        modifier = modifier,
        topBar = {
            if (notMobile) {
                AgentTopAppBar(
                    title = "Artifact Unit",
                    subtitle = workspace?.name,
                    onBack = onBack,
                    actions = {
                        AgentTopAppBarActions.DeleteButton(
                            onClick = { viewModel.clearArtifact() },
                            contentDescription = "Clear Artifact"
                        )
                    }
                )
            }
        }
    ) { paddingValues ->
        ResizableSplitPane(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            initialSplitRatio = 0.5f,
            minRatio = 0.3f,
            maxRatio = 0.7f,
            saveKey = "artifact_unit_split_ratio",
            first = {
                // Left panel - Chat interface for code generation
                ArtifactChatPane(
                    llmService = llmService,
                    workspace = workspace,
                    onCodeGenerated = { code ->
                        viewModel.updateSourceCode(code)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            },
            second = {
                // Right panel - Artifact workbench
                ArtifactWorkbench(
                    state = state,
                    onTabSelected = viewModel::selectTab,
                    onTypeSelected = viewModel::selectArtifactType,
                    onBuildArtifact = { name, prompt, history ->
                        // Launch coroutine from parent scope
                    },
                    onImportArtifact = { binaryData ->
                        // Launch coroutine from parent scope
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        )
    }
}
