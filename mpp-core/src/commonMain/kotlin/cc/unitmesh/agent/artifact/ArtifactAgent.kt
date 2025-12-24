package cc.unitmesh.agent.artifact

import cc.unitmesh.agent.model.AgentContext
import cc.unitmesh.llm.LLMMessage

/**
 * Artifact Agent - specialized agent for creating reversible executable artifacts
 *
 * This agent combines code generation capabilities with artifact building
 * to produce standalone executables that contain their own source code
 * and generation history.
 */
interface ArtifactAgent {
    /**
     * Generate code and build an artifact from a user prompt
     *
     * @param prompt The user's request
     * @param context The agent context
     * @param type The type of artifact to build
     * @return Result of the artifact generation
     */
    suspend fun generateArtifact(
        prompt: String,
        context: AgentContext,
        type: ArtifactType = ArtifactType.PYTHON_SCRIPT
    ): ArtifactGenerationResult

    /**
     * Restore a chat session from an extracted artifact
     *
     * @param payload The extracted artifact payload
     * @return Restored messages for the chat interface
     */
    fun restoreChatFromArtifact(payload: ArtifactPayload): List<LLMMessage>

    /**
     * Update an existing artifact with new code changes
     *
     * @param originalPayload The original artifact payload
     * @param newCode The updated code
     * @param updateMessage Description of the update
     * @return Result of the artifact update
     */
    suspend fun updateArtifact(
        originalPayload: ArtifactPayload,
        newCode: Map<String, String>,
        updateMessage: String
    ): ArtifactGenerationResult
}

/**
 * Result of artifact generation
 */
sealed class ArtifactGenerationResult {
    /**
     * Generation successful
     */
    data class Success(
        val payload: ArtifactPayload,
        val generatedCode: String
    ) : ArtifactGenerationResult()

    /**
     * Generation failed
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : ArtifactGenerationResult()

    /**
     * Generation in progress
     */
    data class Progress(
        val stage: GenerationStage,
        val progress: Float,
        val message: String
    ) : ArtifactGenerationResult()
}

/**
 * Generation stages
 */
enum class GenerationStage {
    ANALYZING_PROMPT,
    GENERATING_CODE,
    DETECTING_DEPENDENCIES,
    CREATING_PAYLOAD,
    COMPLETE
}
