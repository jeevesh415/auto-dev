package cc.unitmesh.agent.artifact

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

/**
 * iOS implementation for time utilities
 */
actual fun getCurrentTimeMillis(): Long = (NSDate().timeIntervalSince1970() * 1000).toLong()
