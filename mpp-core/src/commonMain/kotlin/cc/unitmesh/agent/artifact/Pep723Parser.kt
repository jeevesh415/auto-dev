package cc.unitmesh.agent.artifact

/**
 * PEP 723 Parser - parses Python inline script metadata
 *
 * PEP 723 defines a format for embedding metadata in Python scripts:
 * # /// script
 * # requires-python = ">=3.11"
 * # dependencies = [
 * #   "requests<3",
 * #   "rich",
 * # ]
 * # ///
 */
object Pep723Parser {
    private val SCRIPT_BLOCK_START = Regex("""^#\s*///\s*script\s*$""", RegexOption.MULTILINE)
    private val SCRIPT_BLOCK_END = Regex("""^#\s*///\s*$""", RegexOption.MULTILINE)
    private val DEPENDENCY_LINE = Regex("""^#\s*"([^"]+)"\s*,?\s*$""")
    private val REQUIRES_PYTHON = Regex("""^#\s*requires-python\s*=\s*"([^"]+)"\s*$""")
    private val DEPENDENCIES_START = Regex("""^#\s*dependencies\s*=\s*\[\s*$""")
    private val DEPENDENCIES_END = Regex("""^#\s*]\s*$""")

    /**
     * Parse PEP 723 metadata from Python script
     *
     * @param scriptContent The Python script content
     * @return Parsed dependency specification or null if not found
     */
    fun parse(scriptContent: String): DependencySpec? {
        val lines = scriptContent.lines()
        var inScriptBlock = false
        var inDependencies = false
        val dependencies = mutableListOf<String>()
        var requiresPython = ""
        val rawMetadata = StringBuilder()

        for (line in lines) {
            if (SCRIPT_BLOCK_START.matches(line)) {
                inScriptBlock = true
                rawMetadata.appendLine(line)
                continue
            }

            if (SCRIPT_BLOCK_END.matches(line) && inScriptBlock) {
                rawMetadata.appendLine(line)
                break
            }

            if (!inScriptBlock) continue

            rawMetadata.appendLine(line)

            // Check for requires-python
            val pythonMatch = REQUIRES_PYTHON.find(line)
            if (pythonMatch != null) {
                requiresPython = pythonMatch.groupValues[1]
                continue
            }

            // Check for dependencies array start
            if (DEPENDENCIES_START.matches(line)) {
                inDependencies = true
                continue
            }

            // Check for dependencies array end
            if (DEPENDENCIES_END.matches(line) && inDependencies) {
                inDependencies = false
                continue
            }

            // Parse dependency line
            if (inDependencies) {
                val depMatch = DEPENDENCY_LINE.find(line)
                if (depMatch != null) {
                    dependencies.add(depMatch.groupValues[1])
                }
            }
        }

        if (rawMetadata.isEmpty()) {
            return null
        }

        // Parse dependencies into name -> version map
        val parsed = dependencies.associate { dep ->
            val parts = dep.split(Regex("[<>=!~]+"), limit = 2)
            val name = parts[0].trim()
            val version = if (parts.size > 1) parts[1].trim() else "*"
            name to version
        }

        return DependencySpec(
            type = DependencyType.PEP_723,
            content = rawMetadata.toString(),
            parsed = parsed
        )
    }

    /**
     * Generate PEP 723 metadata block
     *
     * @param dependencies Map of package name to version constraint
     * @param requiresPython Python version requirement
     * @return PEP 723 formatted string
     */
    fun generate(
        dependencies: Map<String, String>,
        requiresPython: String = ">=3.8"
    ): String {
        val sb = StringBuilder()
        sb.appendLine("# /// script")
        sb.appendLine("# requires-python = \"$requiresPython\"")
        
        if (dependencies.isNotEmpty()) {
            sb.appendLine("# dependencies = [")
            dependencies.forEach { (name, version) ->
                val versionSpec = if (version == "*") "" else version
                sb.appendLine("#   \"$name$versionSpec\",")
            }
            sb.appendLine("# ]")
        }
        
        sb.appendLine("# ///")
        return sb.toString()
    }

    /**
     * Extract PEP 723 block from script
     *
     * @param scriptContent The Python script content
     * @return The PEP 723 block or null if not found
     */
    fun extractBlock(scriptContent: String): String? {
        val lines = scriptContent.lines()
        var inScriptBlock = false
        val block = StringBuilder()

        for (line in lines) {
            if (SCRIPT_BLOCK_START.matches(line)) {
                inScriptBlock = true
                block.appendLine(line)
                continue
            }

            if (SCRIPT_BLOCK_END.matches(line) && inScriptBlock) {
                block.appendLine(line)
                break
            }

            if (inScriptBlock) {
                block.appendLine(line)
            }
        }

        return if (block.isEmpty()) null else block.toString()
    }

    /**
     * Check if script contains PEP 723 metadata
     *
     * @param scriptContent The Python script content
     * @return True if PEP 723 metadata is present
     */
    fun hasPep723Metadata(scriptContent: String): Boolean {
        return SCRIPT_BLOCK_START.find(scriptContent) != null
    }
}
