package cc.unitmesh.agent.artifact

/**
 * Interface for building executable artifacts from AI-generated code
 *
 * This is a platform-independent interface. Platform-specific implementations
 * handle the actual binary operations (file I/O, compression, etc.).
 */
interface ArtifactBuilder {
    /**
     * Build an artifact from a payload
     *
     * @param payload The artifact payload containing source code and metadata
     * @param shellTemplate Path to the runtime shell template (pre-built executable)
     * @return Result containing the artifact binary data or error
     */
    suspend fun build(payload: ArtifactPayload, shellTemplate: String): ArtifactBuildResult

    /**
     * Validate a payload before building
     *
     * @param payload The payload to validate
     * @return Validation result with any errors or warnings
     */
    fun validate(payload: ArtifactPayload): ValidationResult

    /**
     * Get available runtime shell templates for a given artifact type
     *
     * @param type The artifact type
     * @return List of available shell templates
     */
    suspend fun getAvailableShells(type: ArtifactType): List<ShellTemplate>
}

/**
 * Result of an artifact build operation
 */
sealed class ArtifactBuildResult {
    /**
     * Successful build with binary data
     */
    data class Success(
        val binaryData: ByteArray,
        val fileName: String,
        val metadata: ArtifactMetadata
    ) : ArtifactBuildResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Success

            if (!binaryData.contentEquals(other.binaryData)) return false
            if (fileName != other.fileName) return false
            if (metadata != other.metadata) return false

            return true
        }

        override fun hashCode(): Int {
            var result = binaryData.contentHashCode()
            result = 31 * result + fileName.hashCode()
            result = 31 * result + metadata.hashCode()
            return result
        }
    }

    /**
     * Build failed with error
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : ArtifactBuildResult()

    /**
     * Build in progress with status update
     */
    data class Progress(
        val stage: BuildStage,
        val progress: Float, // 0.0 to 1.0
        val message: String
    ) : ArtifactBuildResult()
}

/**
 * Build stages for progress reporting
 */
enum class BuildStage {
    VALIDATING,
    LOADING_SHELL,
    COMPRESSING_PAYLOAD,
    SERIALIZING_METADATA,
    INJECTING_PAYLOAD,
    WRITING_FOOTER,
    FINALIZING,
    COMPLETE
}

/**
 * Validation result
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
) {
    companion object {
        fun success() = ValidationResult(true)
        fun error(vararg errors: String) = ValidationResult(false, errors.toList())
        fun withWarnings(vararg warnings: String) = ValidationResult(true, emptyList(), warnings.toList())
    }
}

/**
 * Runtime shell template information
 */
data class ShellTemplate(
    val id: String,
    val name: String,
    val type: ArtifactType,
    val platform: Platform,
    val version: String,
    val path: String,
    val description: String = ""
) {
    enum class Platform {
        WINDOWS_X64,
        WINDOWS_ARM64,
        LINUX_X64,
        LINUX_ARM64,
        MACOS_X64,
        MACOS_ARM64,
        UNIVERSAL
    }
}
