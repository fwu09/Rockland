package com.example.rockland.data.auth

// Global toggle for auth diagnostics; enable only from the entry point in debug builds.
object AuthDiagnostics {
    @Volatile var enabled: Boolean = false
}


