package com.cloudxtreme.pro.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudxtreme.pro.R
import com.cloudxtreme.pro.ui.theme.*

@Composable
fun HeaderBar(
    title: String = "Cloud Xtreme Pro",
    subtitle: String = "Android Virtualization & Cloud Phone",
    onActionClick: (() -> Unit)? = null
) {
    Surface(
        color = VoidSurface,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // App Logo
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, ElectricBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(listOf(VoidSurfaceVariant, VoidDark))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "Cloud Xtreme Pro Logo",
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = DeepViolet.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LavenderGlow.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "PRO",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = LavenderGlow,
                                fontSize = 9.sp
                            )
                        }
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            IconButton(
                onClick = { onActionClick?.invoke() }
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Isolation Engine Shield",
                    tint = ElectricBlue
                )
            }
        }
    }
}
