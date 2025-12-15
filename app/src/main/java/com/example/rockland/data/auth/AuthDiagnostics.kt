package com.example.rockland.data.auth

/**
 * Global auth diagnostics toggle.
 *
 * Keep this disabled by default to keep production logs clean.
 * Enable from your entry point (e.g., MainActivity) when debugging.
 */
object AuthDiagnostics {
    @Volatile var enabled: Boolean = false
}


