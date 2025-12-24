package cc.unitmesh.agent.artifact

/**
 * Interface for extracting artifacts back into their components
 *
 * This enables the "reversible" workflow where users can drag & drop
 * a previously generated artifact back into AutoDev to restore the
 * chat context and iterate on the code.
 */
interface ArtifactExtractor {
    /**
     * Extract an artifact from binary data
     *
     * @param binaryData The artifact binary data
     * @return Result containing the extracted payload or error
     */
    suspend fun extract(binaryData: ByteArray): ArtifactExtractionResult

    /**
     * Extract an artifact from a file path
     *
     * @param filePath Path to the artifact file
     * @return Result containing the extracted payload or error
     */
    suspend fun extractFromFile(filePath: String): ArtifactExtractionResult

    /**
     * Check if a file is a valid artifact
     *
     * @param binaryData The binary data to check
     * @return True if the data appears to be a valid artifact
     */
    fun isValidArtifact(binaryData: ByteArray): Boolean

    /**
     * Check if a file is a valid artifact
     *
     * @param filePath Path to the file to check
     * @return True if the file appears to be a valid artifact
     */
    suspend fun isValidArtifactFile(filePath: String): Boolean

    /**
     * Extract only metadata without decompressing the full payload
     *
     * @param binaryData The artifact binary data
     * @return Result containing just the metadata or error
     */
    suspend fun extractMetadata(binaryData: ByteArray): ArtifactMetadataResult
}

/**
 * Result of an artifact extraction operation
 */
sealed class ArtifactExtractionResult {
    /**
     * Successful extraction with payload
     */
    data class Success(
        val payload: ArtifactPayload
    ) : ArtifactExtractionResult()

    /**
     * Extraction failed with error
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : ArtifactExtractionResult()

    /**
     * Extraction in progress with status update
     */
    data class Progress(
        val stage: ExtractionStage,
        val progress: Float, // 0.0 to 1.0
        val message: String
    ) : ArtifactExtractionResult()
}

/**
 * Extraction stages for progress reporting
 */
enum class ExtractionStage {
    READING_FILE,
    LOCATING_DELIMITER,
    READING_FOOTER,
    VERIFYING_CHECKSUMS,
    EXTRACTING_METADATA,
    DECOMPRESSING_PAYLOAD,
    PARSING_FILES,
    COMPLETE
}

/**
 * Result of metadata-only extraction
 */
sealed class ArtifactMetadataResult {
    /**
     * Successful extraction with metadata
     */
    data class Success(
        val metadata: ArtifactMetadata
    ) : ArtifactMetadataResult()

    /**
     * Extraction failed with error
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : ArtifactMetadataResult()
}
