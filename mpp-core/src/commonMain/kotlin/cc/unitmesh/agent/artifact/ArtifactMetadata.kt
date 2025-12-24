package cc.unitmesh.agent.artifact

import kotlinx.serialization.Serializable

/**
 * Metadata for an AI-generated artifact
 *
 * This contains all the information needed to reconstruct the artifact's
 * generation context, including chat history, prompts, and configuration.
 */
@Serializable
data class ArtifactMetadata(
    /**
     * Unique identifier for this artifact
     */
    val id: String,

    /**
     * Display name for the artifact
     */
    val name: String,

    /**
     * Type of artifact (PYTHON_SCRIPT, WEB_APP, etc.)
     */
    val type: ArtifactType,

    /**
     * Original prompt that created this artifact
     */
    val originalPrompt: String,

    /**
     * Complete chat history used to generate this artifact
     */
    val chatHistory: List<ChatMessage>,

    /**
     * User intent description
     */
    val userIntent: String = "",

    /**
     * Timestamp when artifact was created (Unix timestamp in milliseconds)
     */
    val createdAt: Long,

    /**
     * Version of the artifact (for tracking iterations)
     */
    val version: String = "1.0.0",

    /**
     * Tags for categorization
     */
    val tags: List<String> = emptyList(),

    /**
     * Additional custom metadata
     */
    val customMetadata: Map<String, String> = emptyMap()
)

/**
 * Simple chat message representation for metadata
 */
@Serializable
data class ChatMessage(
    val role: String, // "user", "assistant", "system"
    val content: String,
    val timestamp: Long = 0L
)

/**
 * Type of artifact that can be built
 */
@Serializable
enum class ArtifactType(val displayName: String, val fileExtension: String) {
    /**
     * Python script with PEP 723 inline dependencies
     */
    PYTHON_SCRIPT("Python Script", ".exe"),

    /**
     * Web application (HTML/CSS/JS)
     */
    WEB_APP("Web Application", ".exe"),

    /**
     * Node.js application
     */
    NODE_APP("Node.js Application", ".exe"),

    /**
     * Generic executable
     */
    GENERIC("Generic Executable", ".exe");

    companion object {
        fun fromString(type: String): ArtifactType {
            return when (type.lowercase()) {
                "python", "python_script" -> PYTHON_SCRIPT
                "web", "web_app", "html" -> WEB_APP
                "node", "nodejs", "node_app" -> NODE_APP
                else -> GENERIC
            }
        }
    }
}
