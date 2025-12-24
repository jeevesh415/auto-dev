package cc.unitmesh.agent.artifact

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.*
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * JVM implementation of ArtifactBuilder
 *
 * Builds executable artifacts by appending payload to runtime shells
 */
class JvmArtifactBuilder : ArtifactBuilder {
    private val json = Json { prettyPrint = true }

    override suspend fun build(
        payload: ArtifactPayload,
        shellTemplate: String
    ): ArtifactBuildResult = withContext(Dispatchers.IO) {
        try {
            // Validate payload first
            val validation = validate(payload)
            if (!validation.isValid) {
                return@withContext ArtifactBuildResult.Error(
                    "Validation failed: ${validation.errors.joinToString(", ")}"
                )
            }

            // Load runtime shell
            val shellFile = File(shellTemplate)
            if (!shellFile.exists()) {
                return@withContext ArtifactBuildResult.Error(
                    "Runtime shell not found: $shellTemplate"
                )
            }

            val shellBytes = shellFile.readBytes()

            // Compress payload
            val payloadBytes = compressPayload(payload)

            // Serialize metadata
            val metadataJson = json.encodeToString(payload.metadata)
            val metadataBytes = metadataJson.toByteArray(Charsets.UTF_8)

            // Calculate checksums
            val payloadChecksum = calculateSHA256(payloadBytes)
            val metadataChecksum = calculateSHA256(metadataBytes)

            // Create footer
            val delimiterOffset = shellBytes.size.toLong()
            val payloadOffset = delimiterOffset + ArtifactBinaryFormat.MAGIC_DELIMITER.length
            val metadataOffset = payloadOffset + payloadBytes.size
            
            val footer = ArtifactFooter(
                formatVersion = ArtifactBinaryFormat.FORMAT_VERSION,
                delimiterOffset = delimiterOffset,
                payloadOffset = payloadOffset,
                payloadSize = payloadBytes.size.toLong(),
                metadataOffset = metadataOffset,
                metadataSize = metadataBytes.size.toLong(),
                payloadChecksum = payloadChecksum,
                metadataChecksum = metadataChecksum
            )

            // Assemble binary
            val binaryData = ByteArrayOutputStream().use { output ->
                // Runtime shell
                output.write(shellBytes)
                
                // Magic delimiter
                output.write(ArtifactBinaryFormat.MAGIC_DELIMITER.toByteArray(Charsets.UTF_8))
                
                // Payload
                output.write(payloadBytes)
                
                // Metadata
                output.write(metadataBytes)
                
                // Footer
                output.write(footer.toBytes())
                
                output.toByteArray()
            }

            val fileName = "${payload.metadata.name}${payload.metadata.type.fileExtension}"
            
            ArtifactBuildResult.Success(
                binaryData = binaryData,
                fileName = fileName,
                metadata = payload.metadata
            )
        } catch (e: Exception) {
            ArtifactBuildResult.Error(
                message = "Build failed: ${e.message}",
                cause = e
            )
        }
    }

    override fun validate(payload: ArtifactPayload): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Check if source files exist
        if (payload.sourceFiles.isEmpty()) {
            errors.add("No source files provided")
        }

        // Check if entry point exists
        if (!payload.sourceFiles.containsKey(payload.entryPoint)) {
            errors.add("Entry point file not found: ${payload.entryPoint}")
        }

        // Warn if no dependencies
        if (payload.dependencies.type == DependencyType.NONE) {
            warnings.add("No dependencies specified")
        }

        // Check file sizes
        val totalSize = payload.sourceFiles.values.sumOf { it.length }
        if (totalSize > ArtifactBinaryFormat.MAX_PAYLOAD_SIZE) {
            errors.add("Payload too large: $totalSize bytes")
        }

        return if (errors.isEmpty()) {
            ValidationResult(true, warnings = warnings)
        } else {
            ValidationResult(false, errors = errors, warnings = warnings)
        }
    }

    override suspend fun getAvailableShells(type: ArtifactType): List<ShellTemplate> {
        // In a real implementation, this would scan a directory of runtime shells
        // For now, return an empty list as shells need to be provided separately
        return emptyList()
    }

    /**
     * Compress payload to ZIP format
     */
    private fun compressPayload(payload: ArtifactPayload): ByteArray {
        return ByteArrayOutputStream().use { baos ->
            ZipOutputStream(baos).use { zip ->
                // Add source files
                payload.sourceFiles.forEach { (path, content) ->
                    val entry = ZipEntry(path)
                    zip.putNextEntry(entry)
                    zip.write(content.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }

                // Add dependency spec if present
                if (payload.dependencies.type != DependencyType.NONE) {
                    val depFileName = when (payload.dependencies.type) {
                        DependencyType.PEP_723 -> "requirements.txt"
                        DependencyType.PACKAGE_JSON -> "package.json"
                        DependencyType.REQUIREMENTS_TXT -> "requirements.txt"
                        else -> "dependencies.txt"
                    }
                    val entry = ZipEntry(depFileName)
                    zip.putNextEntry(entry)
                    zip.write(payload.dependencies.content.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }

                // Add assets
                payload.assets.forEach { (path, base64Content) ->
                    val entry = ZipEntry("assets/$path")
                    zip.putNextEntry(entry)
                    // Decode base64 and write
                    zip.write(java.util.Base64.getDecoder().decode(base64Content))
                    zip.closeEntry()
                }
            }
            baos.toByteArray()
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
}
