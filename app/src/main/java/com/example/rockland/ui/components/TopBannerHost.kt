package com.example.rockland.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rockland.presentation.model.UiBanner
import com.example.rockland.presentation.model.UiBannerType
import com.example.rockland.ui.theme.AccentBlue
import com.example.rockland.ui.theme.AccentGreen
import com.example.rockland.ui.theme.AccentRed
import kotlinx.coroutines.delay

@Composable
fun TopBannerHost(
    banner: UiBanner?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    autoDismissMs: Long = 2600L
) {
    LaunchedEffect(banner) {
        if (banner != null) {
            delay(autoDismissMs)
            onDismiss()
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = banner != null,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it })
        ) {
            val bg = when (banner?.type) {
                UiBannerType.Success -> AccentGreen
                UiBannerType.Error -> AccentRed
                UiBannerType.Info, null -> AccentBlue
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                color = bg,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 4.dp
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    text = banner?.text.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}


