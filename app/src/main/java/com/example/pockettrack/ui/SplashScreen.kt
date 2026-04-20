package com.example.pockettrack.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pockettrack.R
import com.example.pockettrack.ui.theme.Emerald400
import com.example.pockettrack.ui.theme.Emerald600
import kotlinx.coroutines.delay

private val DarkSurface = Color(0xFF050E0A)
private val DarkCenter  = Color(0xFF0F2D1E)

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var started by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        started = true
        delay(2500)
        onFinished()
    }

    val iconScale by animateFloatAsState(
        targetValue = if (started) 1f else 0.5f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "iconScale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "alpha"
    )

    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "pulseRadius"
    )
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.55f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(DarkCenter, DarkSurface),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // Icon with pulsing ring
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(152.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                    val baseRadius = 58.dp.toPx()
                    // Pulsing outer ring
                    drawCircle(
                        color = Emerald400.copy(alpha = pulseAlpha),
                        radius = baseRadius * pulseRadius,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    // Static subtle ring
                    drawCircle(
                        color = Emerald600.copy(alpha = 0.3f),
                        radius = baseRadius,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
                // Icon card
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .scale(iconScale)
                        .background(
                            Brush.radialGradient(listOf(Color(0xFF103525), DarkSurface)),
                            RoundedCornerShape(28.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(76.dp)
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // App name
            Text(
                text = "POCKETTRACK",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 5.sp,
                modifier = Modifier.alpha(contentAlpha)
            )

            Spacer(Modifier.height(10.dp))

            // Tagline
            Text(
                text = "TRACK  ·  SAVE  ·  GROW",
                color = Emerald400.copy(alpha = 0.75f),
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.alpha(contentAlpha)
            )

            Spacer(Modifier.height(20.dp))

            // Decorative center glow line
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .height(1.dp)
                    .alpha(contentAlpha)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Emerald400, Color.Transparent)
                        )
                    )
            )
        }
    }
}