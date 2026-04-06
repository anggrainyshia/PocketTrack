package com.example.pockettrack.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pockettrack.ui.theme.SageDark
import com.example.pockettrack.ui.theme.SagePrimary
import kotlinx.coroutines.delay
import com.example.pockettrack.R

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true; delay(2000); onFinished() }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.5f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)
    )
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(SagePrimary, SageDark))),
        contentAlignment = Alignment.Center
    ) {
        Column(Modifier.scale(scale), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(100.dp).background(Color.White.copy(.18f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "App Icon",
                modifier = Modifier.size(64.dp)
            ) }
            Spacer(Modifier.height(20.dp))
            Text("PocketTrack", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("YOUR MONEY, SIMPLIFIED", color = Color.White.copy(.75f), fontSize = 12.sp, letterSpacing = 2.sp)
        }
    }
}