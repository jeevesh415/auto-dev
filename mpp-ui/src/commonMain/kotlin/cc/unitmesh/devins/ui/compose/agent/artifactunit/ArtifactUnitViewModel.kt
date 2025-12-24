package cc.unitmesh.devins.ui.compose.agent.artifactunit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cc.unitmesh.agent.artifact.*
import cc.unitmesh.devins.workspace.Workspace
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * ViewModel for Artifact Unit page
 *
 * Manages state for the dual-pane artifact builder:
 * - Left: Chat interface for code generation
 * - Right: Artifact workbench (source/preview/metadata tabs)
 */
class ArtifactUnitViewModel(
    private val workspace: Workspace?
) {
    /**
     * Current state of the artifact unit
     */
    var state by mutableStateOf(ArtifactUnitState())
        private set

    /**
     * Notification events (title, message)
     */
    private val _notificationEvent = MutableSharedFlow<Pair<String, String>>()
    val notificationEvent: SharedFlow<Pair<String, String>> = _notificationEvent

    /**
     * Select a workbench tab
     */
    fun selectTab(tab: WorkbenchTab) {
        state = state.copy(selectedTab = tab)
    }

    /**
     * Update the generated source code
     */
    fun updateSourceCode(code: Map<String, String>) {
        state = state.copy(sourceCode = code)
    }

    /**
     * Update the selected artifact type
     */
    fun selectArtifactType(type: ArtifactType) {
        state = state.copy(selectedType = type)
    }

    /**
     * Start building an artifact
     */
    suspend fun buildArtifact(
        name: String,
        prompt: String,
        chatHistory: List<ChatMessage>
    ) {
        state = state.copy(isBuildingArtifact = true, buildProgress = 0f)

        try {
            // Create metadata
            val metadata = ArtifactMetadata(
                id = generateId(),
                name = name,
                type = state.selectedType,
                originalPrompt = prompt,
                chatHistory = chatHistory,
                userIntent = prompt,
                createdAt = getCurrentTimeMillis()
            )

            // Detect dependencies from source code
            val dependencies = detectDependencies(state.sourceCode, state.selectedType)

            // Create payload
            val payload = ArtifactPayload(
                metadata = metadata,
                sourceFiles = state.sourceCode,
                entryPoint = detectEntryPoint(state.sourceCode, state.selectedType),
                dependencies = dependencies
            )

            state = state.copy(
                currentPayload = payload,
                buildProgress = 0.5f
            )

            // Note: Actual building happens in platform-specific code
            // Here we just prepare the payload
            state = state.copy(
                isBuildingArtifact = false,
                buildProgress = 1f,
                lastBuiltPayload = payload
            )

            _notificationEvent.emit("Build Ready" to "Artifact is ready to export")
        } catch (e: Exception) {
            state = state.copy(
                isBuildingArtifact = false,
                buildProgress = 0f,
                buildError = e.message
            )
            _notificationEvent.emit("Build Failed" to (e.message ?: "Unknown error"))
        }
    }

    /**
     * Import an artifact from binary data
     */
    suspend fun importArtifact(binaryData: ByteArray) {
        state = state.copy(isImportingArtifact = true)

        try {
            // Note: Actual extraction happens in platform-specific code
            // For now, we just show a placeholder
            _notificationEvent.emit("Import" to "Artifact import not yet implemented")
            state = state.copy(isImportingArtifact = false)
        } catch (e: Exception) {
            state = state.copy(isImportingArtifact = false)
            _notificationEvent.emit("Import Failed" to (e.message ?: "Unknown error"))
        }
    }

    /**
     * Clear the current artifact state
     */
    fun clearArtifact() {
        state = state.copy(
            sourceCode = emptyMap(),
            currentPayload = null,
            lastBuiltPayload = null,
            buildError = null,
            buildProgress = 0f
        )
    }

    /**
     * Detect dependencies from source code
     */
    private fun detectDependencies(
        sourceCode: Map<String, String>,
        type: ArtifactType
    ): DependencySpec {
        return when (type) {
            ArtifactType.PYTHON_SCRIPT -> {
                // Check main file for PEP 723 metadata
                val mainFile = sourceCode.values.firstOrNull() ?: ""
                Pep723Parser.parse(mainFile) ?: DependencySpec(
                    type = DependencyType.NONE,
                    content = ""
                )
            }
            ArtifactType.WEB_APP, ArtifactType.NODE_APP -> {
                // Look for package.json
                val packageJson = sourceCode["package.json"] ?: ""
                if (packageJson.isNotEmpty()) {
                    DependencySpec(
                        type = DependencyType.PACKAGE_JSON,
                        content = packageJson
                    )
                } else {
                    DependencySpec(type = DependencyType.NONE, content = "")
                }
            }
            else -> DependencySpec(type = DependencyType.NONE, content = "")
        }
    }

    /**
     * Detect entry point file
     */
    private fun detectEntryPoint(
        sourceCode: Map<String, String>,
        type: ArtifactType
    ): String {
        return when (type) {
            ArtifactType.PYTHON_SCRIPT -> {
                sourceCode.keys.firstOrNull { it.endsWith(".py") } ?: "main.py"
            }
            ArtifactType.WEB_APP -> {
                sourceCode.keys.firstOrNull { it == "index.html" } ?: "index.html"
            }
            ArtifactType.NODE_APP -> {
                sourceCode.keys.firstOrNull { it == "index.js" || it == "main.js" } ?: "index.js"
            }
            else -> sourceCode.keys.firstOrNull() ?: "main"
        }
    }

    private fun generateId(): String {
        return "artifact-${getCurrentTimeMillis()}"
    }

    private fun getCurrentTimeMillis(): Long {
        // Platform-independent time - to be implemented per platform
        return 0L
    }

    /**
     * Cleanup resources
     */
    fun dispose() {
        // Cleanup if needed
    }
}

/**
 * State for the Artifact Unit page
 */
data class ArtifactUnitState(
    /**
     * Selected workbench tab
     */
    val selectedTab: WorkbenchTab = WorkbenchTab.SOURCE_CODE,

    /**
     * Selected artifact type
     */
    val selectedType: ArtifactType = ArtifactType.PYTHON_SCRIPT,

    /**
     * Generated source code (filename -> content)
     */
    val sourceCode: Map<String, String> = emptyMap(),

    /**
     * Current payload being built
     */
    val currentPayload: ArtifactPayload? = null,

    /**
     * Last successfully built payload
     */
    val lastBuiltPayload: ArtifactPayload? = null,

    /**
     * Whether artifact is being built
     */
    val isBuildingArtifact: Boolean = false,

    /**
     * Build progress (0.0 to 1.0)
     */
    val buildProgress: Float = 0f,

    /**
     * Build error message
     */
    val buildError: String? = null,

    /**
     * Whether artifact is being imported
     */
    val isImportingArtifact: Boolean = false
)

/**
 * Workbench tabs
 */
enum class WorkbenchTab(val displayName: String) {
    SOURCE_CODE("Source Code"),
    RUN_PREVIEW("Run Preview"),
    BINARY_METADATA("Binary Metadata")
}
