package com.example.evenly

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var animationStarted by remember { mutableStateOf(false) }
    
    // Animation for circle expansion - starts from 0.0 and expands to 3.0 to fill most of the screen
    val circleScale by animateFloatAsState(
        targetValue = if (animationStarted) 3f else 0f,
        animationSpec = tween(2000, easing = EaseOutExpo),
        label = "circleScale"
    )
    
    // Text fade in animation
    val textAlpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(1000, delayMillis = 500),
        label = "textAlpha"
    )
    
    LaunchedEffect(Unit) {
        delay(100) // Small delay to ensure animation triggers
        animationStarted = true
        delay(2000) // Wait for 2 seconds
        onAnimationComplete()
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFE2F0E8)),
        contentAlignment = Alignment.Center
    ) {
        // Orange Circle (expanding from center)
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(circleScale) // Start from 0.0 and expand to 3.0
                .clip(CircleShape)
                .background(Color(0xFFFF7026)),
            contentAlignment = Alignment.Center
        ) {
            // Empty content for the orange circle
        }
        
        // Green Circle (expanding from center, slightly smaller)
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(circleScale * 0.8f) // Slightly smaller than orange circle
                .clip(CircleShape)
                .background(Color(0xFF55BF6E)),
            contentAlignment = Alignment.Center
        ) {
            // Empty content for the green circle
        }
        
        // Welcome Text
        Text(
            text = "Welcome to Evenly",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = textAlpha),
            textAlign = TextAlign.Center,
            fontSize = 32.sp
        )
    }
} 