package cc.unitmesh.devins.ui.compose.agent.artifactunit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.agent.artifact.ArtifactType
import cc.unitmesh.agent.artifact.ChatMessage

/**
 * Artifact Workbench - Tabbed interface for viewing and editing artifacts
 */
@Composable
fun ArtifactWorkbench(
    state: ArtifactUnitState,
    onTabSelected: (WorkbenchTab) -> Unit,
    onTypeSelected: (ArtifactType) -> Unit,
    onBuildArtifact: (String, String, List<ChatMessage>) -> Unit,
    onImportArtifact: (ByteArray) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Tab bar
        TabRow(
            selectedTabIndex = state.selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            WorkbenchTab.entries.forEach { tab ->
                Tab(
                    selected = state.selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    text = { Text(tab.displayName) }
                )
            }
        }

        // Tab content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            when (state.selectedTab) {
                WorkbenchTab.SOURCE_CODE -> {
                    SourceCodeTab(
                        sourceCode = state.sourceCode,
                        selectedType = state.selectedType,
                        onTypeSelected = onTypeSelected,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                WorkbenchTab.RUN_PREVIEW -> {
                    RunPreviewTab(
                        sourceCode = state.sourceCode,
                        artifactType = state.selectedType,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                WorkbenchTab.BINARY_METADATA -> {
                    BinaryMetadataTab(
                        payload = state.currentPayload,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * Source Code Tab - Display generated code with syntax highlighting
 */
@Composable
fun SourceCodeTab(
    sourceCode: Map<String, String>,
    selectedType: ArtifactType,
    onTypeSelected: (ArtifactType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        // Artifact type selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Artifact Type:", style = MaterialTheme.typography.labelLarge)
            
            var expanded by remember { mutableStateOf(false) }
            
            Box {
                OutlinedButton(onClick = { expanded = true }) {
                    Text(selectedType.displayName)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    ArtifactType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName) },
                            onClick = {
                                onTypeSelected(type)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Source code display
        if (sourceCode.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No source code generated yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                sourceCode.forEach { (filename, content) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                filename,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                content,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Run Preview Tab - Preview code execution (Pyodide for Python, iframe for Web)
 */
@Composable
fun RunPreviewTab(
    sourceCode: Map<String, String>,
    artifactType: ArtifactType,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Run preview (Pyodide/WASM) - Coming Soon",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Binary Metadata Tab - Visualize binary structure
 */
@Composable
fun BinaryMetadataTab(
    payload: cc.unitmesh.agent.artifact.ArtifactPayload?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(16.dp),
        contentAlignment = if (payload == null) Alignment.Center else Alignment.TopStart
    ) {
        if (payload == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No artifact built yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Artifact Metadata",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Metadata details
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        MetadataRow("Name", payload.metadata.name)
                        MetadataRow("Type", payload.metadata.type.displayName)
                        MetadataRow("Entry Point", payload.entryPoint)
                        MetadataRow("Files", payload.sourceFiles.size.toString())
                        MetadataRow("Dependencies", payload.dependencies.parsed.size.toString())
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Binary structure visualization
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Binary Structure",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        BinaryStructureItem("Runtime Shell", "Pre-built executable")
                        BinaryStructureItem("Magic Delimiter", "@@AUTODEV_ARTIFACT_DATA@@")
                        BinaryStructureItem("Payload (ZIP)", "Source files compressed")
                        BinaryStructureItem("Metadata (JSON)", "Chat history & context")
                        BinaryStructureItem("Footer", "Offsets & checksums")
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun BinaryStructureItem(name: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Icon(
            Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
