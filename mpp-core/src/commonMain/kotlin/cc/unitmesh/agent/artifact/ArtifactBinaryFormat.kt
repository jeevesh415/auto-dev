package cc.unitmesh.agent.artifact

/**
 * Binary format constants for artifact packaging
 *
 * Defines the structure of the binary artifact file:
 * [Runtime Shell] + [MAGIC_DELIMITER] + [Payload ZIP] + [Metadata JSON] + [Footer]
 */
object ArtifactBinaryFormat {
    /**
     * Magic delimiter to separate runtime shell from payload
     * This is a unique byte sequence unlikely to appear in normal executables
     */
    const val MAGIC_DELIMITER = "@@AUTODEV_ARTIFACT_DATA@@"

    /**
     * Version of the binary format
     */
    const val FORMAT_VERSION = "1.0.0"

    /**
     * Maximum size of metadata JSON (to prevent reading entire file)
     */
    const val MAX_METADATA_SIZE = 10 * 1024 * 1024 // 10 MB

    /**
     * Maximum size of payload ZIP
     */
    const val MAX_PAYLOAD_SIZE = 100 * 1024 * 1024 // 100 MB

    /**
     * Footer size (contains offsets and checksums)
     */
    const val FOOTER_SIZE = 128

    /**
     * Checksum algorithm
     */
    const val CHECKSUM_ALGORITHM = "SHA-256"
}

/**
 * Footer structure for the artifact binary
 *
 * Located at the end of the file, contains pointers to locate the data sections
 */
data class ArtifactFooter(
    /**
     * Format version
     */
    val formatVersion: String,

    /**
     * Offset of the magic delimiter from start of file
     */
    val delimiterOffset: Long,

    /**
     * Offset of the payload ZIP from start of file
     */
    val payloadOffset: Long,

    /**
     * Size of the payload ZIP in bytes
     */
    val payloadSize: Long,

    /**
     * Offset of the metadata JSON from start of file
     */
    val metadataOffset: Long,

    /**
     * Size of the metadata JSON in bytes
     */
    val metadataSize: Long,

    /**
     * SHA-256 checksum of the payload
     */
    val payloadChecksum: String,

    /**
     * SHA-256 checksum of the metadata
     */
    val metadataChecksum: String
) {
    /**
     * Serialize footer to byte array
     */
    fun toBytes(): ByteArray {
        // Format: version(16) + delimiterOffset(8) + payloadOffset(8) + payloadSize(8) +
        //         metadataOffset(8) + metadataSize(8) + payloadChecksum(64) + metadataChecksum(64)
        val buffer = StringBuilder()
        buffer.append(formatVersion.padEnd(16, '\u0000'))
        buffer.append(delimiterOffset.toString().padEnd(8, '0'))
        buffer.append(payloadOffset.toString().padEnd(8, '0'))
        buffer.append(payloadSize.toString().padEnd(8, '0'))
        buffer.append(metadataOffset.toString().padEnd(8, '0'))
        buffer.append(metadataSize.toString().padEnd(8, '0'))
        buffer.append(payloadChecksum.padEnd(64, '0'))
        buffer.append(metadataChecksum.padEnd(64, '0'))
        
        return buffer.toString().encodeToByteArray()
    }

    companion object {
        /**
         * Parse footer from byte array
         */
        fun fromBytes(bytes: ByteArray): ArtifactFooter {
            require(bytes.size >= ArtifactBinaryFormat.FOOTER_SIZE) {
                "Invalid footer size: ${bytes.size}"
            }

            val str = bytes.decodeToString()
            return ArtifactFooter(
                formatVersion = str.substring(0, 16).trimEnd('\u0000'),
                delimiterOffset = str.substring(16, 24).toLong(),
                payloadOffset = str.substring(24, 32).toLong(),
                payloadSize = str.substring(32, 40).toLong(),
                metadataOffset = str.substring(40, 48).toLong(),
                metadataSize = str.substring(48, 56).toLong(),
                payloadChecksum = str.substring(56, 120).trimEnd('0'),
                metadataChecksum = str.substring(120, 184).trimEnd('0')
            )
        }
    }
}
