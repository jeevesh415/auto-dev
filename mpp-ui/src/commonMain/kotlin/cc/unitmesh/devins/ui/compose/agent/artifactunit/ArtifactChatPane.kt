package cc.unitmesh.devins.ui.compose.agent.artifactunit

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.workspace.Workspace
import cc.unitmesh.llm.KoogLLMService

/**
 * Artifact Chat Pane - Left side chat interface for code generation
 *
 * This is integrated with the LLM service to generate code based on user prompts.
 * The generated code is then displayed in the workbench on the right side.
 */
@Composable
fun ArtifactChatPane(
    llmService: KoogLLMService?,
    workspace: Workspace?,
    onCodeGenerated: (Map<String, String>) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Chat,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Chat Interface",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Interact with AI to generate code for your artifact",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            // Placeholder for future chat integration
            // This will be similar to CodingAgentPage but specialized for artifact generation
            OutlinedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Example prompts:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ExamplePrompt("Create a Hacker News scraper in Python")
                    ExamplePrompt("Build a simple TODO list web app")
                    ExamplePrompt("Generate a markdown to PDF converter")
                }
            }
        }
    }
}

@Composable
private fun ExamplePrompt(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text("•", modifier = Modifier.padding(end = 8.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
