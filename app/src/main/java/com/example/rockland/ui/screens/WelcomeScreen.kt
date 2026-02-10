package com.example.rockland.ui.screens

import android.media.MediaPlayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rockland.R
import com.example.rockland.ui.theme.Rock1
import com.example.rockland.ui.theme.TextLight

// 🪨 Rock Font
val RockFont = FontFamily(
    Font(R.font.typoster_rock_on)
)

@Composable
fun WelcomeScreen(
    onSignInClick: () -> Unit = {},
    onSignUpClick: () -> Unit = {}
) {
    // 🔊 Welcome Sound
    val playWelcomeSound = rememberSoundPlayer(R.raw.welcome)
    LaunchedEffect(Unit) { playWelcomeSound() }

    Box(modifier = Modifier.fillMaxSize()) {

        // 🌄 FULLSCREEN CANYON IMAGE
        Image(
            painter = painterResource(R.drawable.rock_welcome),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 🌫 Dark overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.55f)
                        )
                    )
                )
        )

        // 📦 Foreground Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(80.dp))

            // 🪨 ROCKLAND Title with Stone Font
            Text(
                text = stringResource(R.string.app_name),
                fontFamily = RockFont,
                fontSize = 50.sp,
                fontWeight = FontWeight.Normal,
                color = TextLight,
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Identify rocks. Collect discoveries. Earn rewards.",
                fontSize = 16.sp,
                color = TextLight.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sign Up
                Button(
                    onClick = onSignUpClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Rock1,
                        contentColor = TextLight
                    )
                ) {
                    Text(
                        text = stringResource(R.string.sign_up),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Sign In
                OutlinedButton(
                    onClick = onSignInClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextLight
                    )
                ) {
                    Text(
                        text = stringResource(R.string.sign_in),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun rememberSoundPlayer(resId: Int): () -> Unit {
    val context = LocalContext.current
    val player = remember { MediaPlayer.create(context, resId) }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    return {
        if (player.isPlaying) player.seekTo(0)
        player.start()
    }
}
