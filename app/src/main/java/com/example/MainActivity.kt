package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import kotlin.math.abs

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          FootballGameScreen(modifier = Modifier.padding(innerPadding))
        }
      }
    }
  }
}

@Composable
fun FootballGameScreen(modifier: Modifier = Modifier) {
    var score by remember { mutableIntStateOf(0) }
    var attempts by remember { mutableIntStateOf(0) }
    var showResult by remember { mutableStateOf(false) }
    var isGoal by remember { mutableStateOf(false) }

    val ballPosition = remember { Animatable(Offset(0f, 0f), Offset.VectorConverter) }
    val scope = rememberCoroutineScope()
    var isKicked by remember { mutableStateOf(false) }

    // Colors
    val fieldColor = Color(0xFF4CAF50)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(fieldColor)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isKicked) {
                    if (!isKicked) {
                        var dragOffset = Offset.Zero
                        detectDragGestures(
                            onDragCancel = { dragOffset = Offset.Zero },
                            onDragStart = { dragOffset = Offset.Zero },
                            onDragEnd = {
                                if (dragOffset.y < -20f) { // Detect swipe up
                                    isKicked = true
                                    scope.launch {
                                        val canvasWidth = size.width.toFloat()
                                        val canvasHeight = size.height.toFloat()
                                        
                                        val startY = canvasHeight * 0.8f
                                        val startX = canvasWidth / 2f
                                        
                                        // Calculate relative goal position
                                        val goalTop = canvasHeight * 0.1f
                                        val goalBottom = canvasHeight * 0.2f
                                        val goalCenterY = (goalTop + goalBottom) / 2
                                        
                                        // Target distance
                                        val targetDistanceY = goalCenterY - startY
                                        
                                        // Target x distance is based on the swipe's slope
                                        val ratio = dragOffset.x / abs(dragOffset.y)
                                        // Amplify the horizontal drift slightly for better gameplay feel
                                        val targetDistanceX = targetDistanceY * ratio * 1.5f

                                        ballPosition.animateTo(
                                            targetValue = Offset(targetDistanceX, targetDistanceY),
                                            animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing)
                                        )

                                        // Goal detection
                                        val finalX = startX + targetDistanceX
                                        val goalWidth = canvasWidth * 0.4f
                                        val goalLeft = (canvasWidth - goalWidth) / 2
                                        val goalRight = goalLeft + goalWidth

                                        isGoal = finalX in goalLeft..goalRight
                                        if (isGoal) score++
                                        attempts++
                                        showResult = true
                                    }
                                }
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            dragOffset += dragAmount
                        }
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // 1. Draw Field Stripes
            val stripeHeight = canvasHeight / 10
            for (i in 0..10) {
                if (i % 2 == 0) {
                    drawRect(
                        color = Color(0xFF43A047), // Slightly darker green
                        topLeft = Offset(0f, i * stripeHeight),
                        size = Size(canvasWidth, stripeHeight)
                    )
                }
            }

            // 2. Draw Penalty Box
            val goalWidth = canvasWidth * 0.4f
            val goalLeft = (canvasWidth - goalWidth) / 2
            val goalRight = goalLeft + goalWidth
            val goalBottom = canvasHeight * 0.2f
            
            drawRect(
                color = Color.White.copy(alpha = 0.8f),
                topLeft = Offset(canvasWidth * 0.2f, 0f),
                size = Size(canvasWidth * 0.6f, goalBottom + canvasHeight * 0.1f),
                style = Stroke(width = 3.dp.toPx())
            )
            
            // Penalty Arc
            drawArc(
                color = Color.White.copy(alpha = 0.8f),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(canvasWidth * 0.35f, goalBottom + canvasHeight * 0.05f),
                size = Size(canvasWidth * 0.3f, canvasWidth * 0.3f),
                style = Stroke(width = 3.dp.toPx())
            )

            // 3. Draw Goal Net
            val goalTop = canvasHeight * 0.1f
            // Net Background
            drawRect(
                color = Color.White.copy(alpha = 0.3f),
                topLeft = Offset(goalLeft, goalTop),
                size = Size(goalWidth, goalBottom - goalTop)
            )
            
            // Goal Posts and Crossbar
            val postThickness = 8.dp.toPx()
            // Left post
            drawRect(color = Color.White, topLeft = Offset(goalLeft - postThickness, goalTop - postThickness), size = Size(postThickness, goalBottom - goalTop + postThickness))
            // Right post
            drawRect(color = Color.White, topLeft = Offset(goalRight, goalTop - postThickness), size = Size(postThickness, goalBottom - goalTop + postThickness))
            // Crossbar
            drawRect(color = Color.White, topLeft = Offset(goalLeft - postThickness, goalTop - postThickness), size = Size(goalWidth + postThickness * 2, postThickness))


            // 4. Draw Ball
            val ballRadius = 18.dp.toPx()
            val startY = canvasHeight * 0.8f
            val startX = canvasWidth / 2f
            
            val currentX = startX + ballPosition.value.x
            val currentY = startY + ballPosition.value.y
            
            // Shrink the ball slightly as it goes into the distance
            val distanceRatio = 1f - (ballPosition.value.y / (goalTop - startY)) * 0.4f
            val currentRadius = ballRadius * distanceRatio.coerceIn(0.5f, 1f)

            // Ball Shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.4f),
                radius = currentRadius,
                center = Offset(currentX, currentY + 5.dp.toPx())
            )

            // Ball Body
            drawCircle(
                color = Color.White,
                radius = currentRadius,
                center = Offset(currentX, currentY)
            )
            
            // Ball Design (simple center patch)
            drawCircle(
                color = Color.Black,
                radius = currentRadius * 0.35f,
                center = Offset(currentX, currentY)
            )
        }

        // Overlay UI
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 32.dp, end = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Score: $score / $attempts", 
                    fontSize = 24.sp, 
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }

            if (!isKicked) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Swipe up to shoot!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 60.dp)
                )
            }

            if (showResult) {
                Spacer(modifier = Modifier.height(32.dp))
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = if (isGoal) "GOAL!" else "MISS",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isGoal) Color(0xFFFFD700) else Color(0xFFFF5252)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    ballPosition.snapTo(Offset(0f, 0f))
                                    isKicked = false
                                    showResult = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White, 
                                contentColor = Color.Black
                            ),
                            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
                        ) {
                            Text("Next Kick", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
