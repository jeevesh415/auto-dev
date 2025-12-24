package cc.unitmesh.agent.artifact

import kotlin.js.Date

/**
 * WASM implementation for time utilities
 */
actual fun getCurrentTimeMillis(): Long = Date.now().toLong()
