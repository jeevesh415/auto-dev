package cc.unitmesh.agent.artifact

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.*
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * JVM implementation of ArtifactExtractor
 *
 * Extracts artifacts back into their constituent parts for editing
 */
class JvmArtifactExtractor : ArtifactExtractor {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun extract(binaryData: ByteArray): ArtifactExtractionResult =
        withContext(Dispatchers.IO) {
            try {
                // Read footer from end of file
                if (binaryData.size < ArtifactBinaryFormat.FOOTER_SIZE) {
                    return@withContext ArtifactExtractionResult.Error(
                        "File too small to be a valid artifact"
                    )
                }

                val footerBytes = binaryData.sliceArray(
                    binaryData.size - ArtifactBinaryFormat.FOOTER_SIZE until binaryData.size
                )
                val footer = ArtifactFooter.fromBytes(footerBytes)

                // Verify format version
                if (footer.formatVersion != ArtifactBinaryFormat.FORMAT_VERSION) {
                    return@withContext ArtifactExtractionResult.Error(
                        "Unsupported format version: ${footer.formatVersion}"
                    )
                }

                // Extract metadata
                val metadataBytes = binaryData.sliceArray(
                    footer.metadataOffset.toInt() until (footer.metadataOffset + footer.metadataSize).toInt()
                )

                // Verify metadata checksum
                val metadataChecksum = calculateSHA256(metadataBytes)
                if (metadataChecksum != footer.metadataChecksum) {
                    return@withContext ArtifactExtractionResult.Error(
                        "Metadata checksum mismatch - file may be corrupted"
                    )
                }

                val metadataJson = metadataBytes.decodeToString()
                val metadata = json.decodeFromString<ArtifactMetadata>(metadataJson)

                // Extract payload
                val payloadBytes = binaryData.sliceArray(
                    footer.payloadOffset.toInt() until footer.metadataOffset.toInt()
                )

                // Verify payload checksum
                val payloadChecksum = calculateSHA256(payloadBytes)
                if (payloadChecksum != footer.payloadChecksum) {
                    return@withContext ArtifactExtractionResult.Error(
                        "Payload checksum mismatch - file may be corrupted"
                    )
                }

                // Decompress payload
                val sourceFiles = decompressPayload(payloadBytes)

                // Reconstruct dependencies
                val dependencies = detectDependencies(sourceFiles, metadata.type)

                val payload = ArtifactPayload(
                    metadata = metadata,
                    sourceFiles = sourceFiles,
                    entryPoint = detectEntryPoint(sourceFiles, metadata.type),
                    dependencies = dependencies
                )

                ArtifactExtractionResult.Success(payload)
            } catch (e: Exception) {
                ArtifactExtractionResult.Error(
                    message = "Extraction failed: ${e.message}",
                    cause = e
                )
            }
        }

    override suspend fun extractFromFile(filePath: String): ArtifactExtractionResult =
        withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    return@withContext ArtifactExtractionResult.Error(
                        "File not found: $filePath"
                    )
                }

                val binaryData = file.readBytes()
                extract(binaryData)
            } catch (e: Exception) {
                ArtifactExtractionResult.Error(
                    message = "Failed to read file: ${e.message}",
                    cause = e
                )
            }
        }

    override fun isValidArtifact(binaryData: ByteArray): Boolean {
        if (binaryData.size < ArtifactBinaryFormat.FOOTER_SIZE) {
            return false
        }

        try {
            // Check for magic delimiter
            val delimiterBytes = ArtifactBinaryFormat.MAGIC_DELIMITER.toByteArray(Charsets.UTF_8)
            val searchSpace = binaryData.sliceArray(
                0 until minOf(binaryData.size, binaryData.size - ArtifactBinaryFormat.FOOTER_SIZE)
            )

            return searchSpace.contains(delimiterBytes)
        } catch (e: Exception) {
            return false
        }
    }

    override suspend fun isValidArtifactFile(filePath: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    return@withContext false
                }

                // Read just enough to check for magic delimiter
                file.inputStream().use { input ->
                    val buffer = ByteArray(1024 * 1024) // Read first 1MB
                    val bytesRead = input.read(buffer)
                    if (bytesRead > 0) {
                        return@withContext isValidArtifact(buffer.sliceArray(0 until bytesRead))
                    }
                }

                false
            } catch (e: Exception) {
                false
            }
        }

    override suspend fun extractMetadata(binaryData: ByteArray): ArtifactMetadataResult =
        withContext(Dispatchers.IO) {
            try {
                if (binaryData.size < ArtifactBinaryFormat.FOOTER_SIZE) {
                    return@withContext ArtifactMetadataResult.Error(
                        "File too small to be a valid artifact"
                    )
                }

                val footerBytes = binaryData.sliceArray(
                    binaryData.size - ArtifactBinaryFormat.FOOTER_SIZE until binaryData.size
                )
                val footer = ArtifactFooter.fromBytes(footerBytes)

                val metadataBytes = binaryData.sliceArray(
                    footer.metadataOffset.toInt() until (footer.metadataOffset + footer.metadataSize).toInt()
                )

                val metadataJson = metadataBytes.decodeToString()
                val metadata = json.decodeFromString<ArtifactMetadata>(metadataJson)

                ArtifactMetadataResult.Success(metadata)
            } catch (e: Exception) {
                ArtifactMetadataResult.Error(
                    message = "Failed to extract metadata: ${e.message}",
                    cause = e
                )
            }
        }

    /**
     * Decompress ZIP payload
     */
    private fun decompressPayload(payloadBytes: ByteArray): Map<String, String> {
        val sourceFiles = mutableMapOf<String, String>()

        ByteArrayInputStream(payloadBytes).use { bais ->
            ZipInputStream(bais).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val content = zip.readBytes().decodeToString()
                        // Remove 'assets/' prefix if present
                        val path = entry.name.removePrefix("assets/")
                        sourceFiles[path] = content
                    }
                    entry = zip.nextEntry
                }
            }
        }

        return sourceFiles
    }

    /**
     * Detect dependencies from extracted files
     */
    private fun detectDependencies(
        sourceFiles: Map<String, String>,
        type: ArtifactType
    ): DependencySpec {
        return when (type) {
            ArtifactType.PYTHON_SCRIPT -> {
                val mainFile = sourceFiles.values.firstOrNull() ?: ""
                Pep723Parser.parse(mainFile) ?: DependencySpec(
                    type = DependencyType.NONE,
                    content = ""
                )
            }
            ArtifactType.WEB_APP, ArtifactType.NODE_APP -> {
                val packageJson = sourceFiles["package.json"] ?: ""
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
     * Detect entry point from extracted files
     */
    private fun detectEntryPoint(
        sourceFiles: Map<String, String>,
        type: ArtifactType
    ): String {
        return when (type) {
            ArtifactType.PYTHON_SCRIPT -> {
                sourceFiles.keys.firstOrNull { it.endsWith(".py") } ?: "main.py"
            }
            ArtifactType.WEB_APP -> {
                sourceFiles.keys.firstOrNull { it == "index.html" } ?: "index.html"
            }
            ArtifactType.NODE_APP -> {
                sourceFiles.keys.firstOrNull { it == "index.js" || it == "main.js" } ?: "index.js"
            }
            else -> sourceFiles.keys.firstOrNull() ?: "main"
        }
    }

    /**
     * Calculate SHA-256 checksum
     */
    private fun calculateSHA256(data: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Helper to check if byte array contains a subsequence
     */
    private fun ByteArray.contains(other: ByteArray): Boolean {
        if (other.isEmpty()) return true
        if (this.size < other.size) return false

        for (i in 0..this.size - other.size) {
            var match = true
            for (j in other.indices) {
                if (this[i + j] != other[j]) {
                    match = false
                    break
                }
            }
            if (match) return true
        }
        return false
    }
}
