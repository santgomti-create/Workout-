package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.WorkoutCategory
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.util.Locale

fun categoryIcon(category: WorkoutCategory): ImageVector {
    return when (category) {
        WorkoutCategory.WALKING -> Icons.Filled.DirectionsWalk
        WorkoutCategory.RUNNING -> Icons.Filled.DirectionsRun
        WorkoutCategory.CYCLING -> Icons.Filled.DirectionsBike
        WorkoutCategory.YOGA -> Icons.Filled.SelfImprovement
        WorkoutCategory.GYM -> Icons.Filled.FitnessCenter
        WorkoutCategory.HOME_WORKOUT -> Icons.Filled.Home
        WorkoutCategory.CUSTOM -> Icons.Filled.Bolt
    }
}

@Composable
fun categoryAccentColor(category: WorkoutCategory): Color {
    return when (category) {
        WorkoutCategory.WALKING -> MaterialTheme.colorScheme.secondary
        WorkoutCategory.RUNNING -> MaterialTheme.colorScheme.primary
        WorkoutCategory.CYCLING -> MaterialTheme.colorScheme.tertiary
        WorkoutCategory.YOGA -> Color(0xFF4CE0B3)
        WorkoutCategory.GYM -> Color(0xFF18C964)
        WorkoutCategory.HOME_WORKOUT -> Color(0xFF8BE85E)
        WorkoutCategory.CUSTOM -> MaterialTheme.colorScheme.primary
    }
}

fun formatDurationMinutes(seconds: Int): String {
    val mins = seconds / 60
    val remSec = seconds % 60
    return when {
        mins >= 60 -> "${mins / 60}h ${mins % 60}m"
        mins > 0 -> "${mins}m"
        else -> "${remSec}s"
    }
}

fun formatStopwatch(totalSeconds: Int): String {
    val hrs = totalSeconds / 3600
    val mins = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return if (hrs > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}

@Composable
fun AnimatedActivityRing(
    progress: Float,
    centerPrimaryText: String,
    centerSecondaryText: String,
    modifier: Modifier = Modifier,
    size: Dp = 132.dp,
    strokeWidth: Dp = 12.dp
) {
    val clamped = progress.coerceIn(0f, 1f)
    val animatedSweep by animateFloatAsState(
        targetValue = clamped * 360f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "activity_ring_sweep"
    )
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            if (animatedSweep > 0.5f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(primary, tertiary, primary)
                    ),
                    startAngle = -90f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerPrimaryText,
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = centerSecondaryText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AnimatedGoalProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
    fillColor: Color = MaterialTheme.colorScheme.primary
) {
    val clamped = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clamped,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "goal_progress_bar"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(fillColor, MaterialTheme.colorScheme.tertiary)
                    )
                )
        )
    }
}

data class ChartBarPoint(
    val label: String,
    val minutes: Int,
    val calories: Int,
    val sessions: Int
)

@Composable
fun InteractiveWorkoutBarChart(
    points: List<ChartBarPoint>,
    metricUnit: String = "min",
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return
    var selectedIndex by remember(points.size) { mutableIntStateOf(points.lastIndex) }
    val selected = points.getOrNull(selectedIndex) ?: points.last()
    val maxMinutes = (points.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(10)

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val mutedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Interactive tooltip pill for selected day/period
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = selected.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${selected.sessions} workout${if (selected.sessions == 1) "" else "s"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${selected.minutes} $metricUnit",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${selected.calories} kcal",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "Burned",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Tappable Bar Columns
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
                    .align(Alignment.TopCenter)
            ) {
                val lines = 3
                for (i in 0..lines) {
                    val y = size.height * (i / lines.toFloat())
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                points.forEachIndexed { idx, point ->
                    val isSelected = idx == selectedIndex
                    val targetFraction = if (point.minutes == 0) {
                        0.05f
                    } else {
                        (point.minutes.toFloat() / maxMinutes.toFloat()).coerceIn(0.10f, 1f)
                    }
                    val animatedHeight by animateFloatAsState(
                        targetValue = targetFraction,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "bar_$idx"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedIndex = idx }
                            .testTag("chart_bar_$idx"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val barHeight = size.height * animatedHeight
                                val barWidth = size.width.coerceAtMost(28.dp.toPx())
                                val left = (size.width - barWidth) / 2f
                                val top = size.height - barHeight
                                val brush = if (isSelected) {
                                    Brush.verticalGradient(listOf(tertiaryColor, primaryColor))
                                } else {
                                    Brush.verticalGradient(listOf(mutedColor, mutedColor))
                                }
                                drawRoundRect(
                                    brush = brush,
                                    topLeft = Offset(left, top),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = point.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryBadge(
    category: WorkoutCategory,
    modifier: Modifier = Modifier
) {
    val accent = categoryAccentColor(category)
    Surface(
        color = accent.copy(alpha = 0.16f),
        shape = CircleShape,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = categoryIcon(category),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
