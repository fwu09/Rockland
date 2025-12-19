// Entry point that boots Firebase, App Check, and navigation for the Compose UI layer.
package com.example.rockland

import android.os.Bundle
import android.content.pm.ApplicationInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.rockland.navigation.AppNavigation
import com.example.rockland.ui.theme.RocklandTheme
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.example.rockland.data.auth.AuthDiagnostics

class MainActivity : ComponentActivity() {
    private companion object {
        // Turn this on only when debugging network/DNS issues.
        private const val ENABLE_AUTH_DIAGNOSTICS = false
    }

    private fun isDebuggableBuild(): Boolean {
        return (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        splashScreen.setKeepOnScreenCondition { false }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        android.util.Log.d("MainActivity", "debuggable=${isDebuggableBuild()} gmsStatus=${GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)}")
        AuthDiagnostics.enabled = ENABLE_AUTH_DIAGNOSTICS && isDebuggableBuild()

        if (isDebuggableBuild()) {
            // Initialize Firebase App Check (CRITICAL for Firebase Auth/Firestore to work)
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            android.util.Log.d("MainActivity", "Firebase App Check initialized with Debug provider")

            // Force an App Check token request early so failures show up immediately in Logcat.
            firebaseAppCheck.getToken(false)
                .addOnSuccessListener {
                    android.util.Log.d("MainActivity", "App Check token acquired (debug).")
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("MainActivity", "App Check token request failed (debug).", e)
                }
        } else {
            android.util.Log.w(
                "MainActivity",
                "App Check debug provider not installed (likely a release build). If Firebase App Check enforcement is enabled, Auth/Firestore may fail."
            )
        }

        setContent {
            RocklandTheme {
                AppNavigation()
            }
        }
    }
}