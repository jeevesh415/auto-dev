package cc.unitmesh.agent.artifact

import kotlinx.serialization.Serializable

/**
 * Payload structure for an artifact
 *
 * This contains all the files, dependencies, and configuration
 * that make up an executable artifact.
 */
@Serializable
data class ArtifactPayload(
    /**
     * Metadata about the artifact
     */
    val metadata: ArtifactMetadata,

    /**
     * Source files (key = relative path, value = content)
     */
    val sourceFiles: Map<String, String>,

    /**
     * Main entry point file path
     */
    val entryPoint: String,

    /**
     * Dependencies specification (PEP 723 for Python, package.json for Node, etc.)
     */
    val dependencies: DependencySpec,

    /**
     * Asset files (images, data files, etc.) as base64
     */
    val assets: Map<String, String> = emptyMap(),

    /**
     * Runtime configuration
     */
    val runtimeConfig: RuntimeConfig = RuntimeConfig()
)

/**
 * Dependency specification
 */
@Serializable
data class DependencySpec(
    /**
     * Type of dependency specification
     */
    val type: DependencyType,

    /**
     * Raw dependency content (PEP 723 block, package.json content, etc.)
     */
    val content: String,

    /**
     * Parsed dependencies (package name -> version)
     */
    val parsed: Map<String, String> = emptyMap()
)

/**
 * Type of dependency specification
 */
@Serializable
enum class DependencyType {
    /**
     * Python PEP 723 inline script metadata
     */
    PEP_723,

    /**
     * Node.js package.json
     */
    PACKAGE_JSON,

    /**
     * Requirements.txt
     */
    REQUIREMENTS_TXT,

    /**
     * No dependencies
     */
    NONE
}

/**
 * Runtime configuration for the artifact
 */
@Serializable
data class RuntimeConfig(
    /**
     * Python version requirement (e.g., "3.8+")
     */
    val pythonVersion: String = "",

    /**
     * Node.js version requirement (e.g., "18+")
     */
    val nodeVersion: String = "",

    /**
     * Environment variables
     */
    val envVars: Map<String, String> = emptyMap(),

    /**
     * Command line arguments
     */
    val args: List<String> = emptyList(),

    /**
     * Working directory
     */
    val workingDir: String = ".",

    /**
     * Whether to run in windowless mode (for GUI apps)
     */
    val windowless: Boolean = false
)
