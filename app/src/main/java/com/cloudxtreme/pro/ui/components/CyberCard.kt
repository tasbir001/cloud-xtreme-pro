package com.cloudxtreme.pro.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudxtreme.pro.core.model.InstanceStatus
import com.cloudxtreme.pro.ui.theme.*

@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    shapeRadius: Dp = 16.dp,
    borderColor: Color = GlassCardBorder,
    borderWidth: Dp = 1.dp,
    backgroundColor: Color = GlassCard,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        shape = RoundedCornerShape(shapeRadius),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun StatusBadge(
    status: InstanceStatus,
    modifier: Modifier = Modifier
) {
    val (statusColor, statusLabel) = when (status) {
        InstanceStatus.RUNNING -> StatusRunning to "RUNNING"
        InstanceStatus.STARTING -> StatusStarting to "STARTING"
        InstanceStatus.PAUSED -> StatusPaused to "PAUSED"
        InstanceStatus.STOPPED -> StatusStopped to "STOPPED"
        InstanceStatus.ERROR -> StatusStopped to "ERROR"
    }

    Surface(
        modifier = modifier,
        color = statusColor.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = HyperBlue,
    contentColor: Color = Color.White,
    icon: (@Composable () -> Unit)? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.4f)),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ResourceMeterBar(
    label: String,
    currentValueStr: String,
    percentage: Float,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Text(
                text = currentValueStr,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = (percentage / 100f).coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = barColor,
            trackColor = VoidSurfaceVariant
        )
    }
}
