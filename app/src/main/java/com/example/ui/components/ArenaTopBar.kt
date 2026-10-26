package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusRose
import com.example.ui.theme.StatusSky
import com.example.viewmodel.AutomationStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArenaTopBar(
    status: AutomationStatus,
    countdownSeconds: Int,
    isAutoSendEnabled: Boolean,
    isLoggedIn: Boolean,
    onToggleAutoSend: () -> Unit,
    onResetSession: () -> Unit,
    onOpenAccount: () -> Unit
) {
    val statusColor = when (status) {
        AutomationStatus.STREAMING -> StatusSky
        AutomationStatus.COUNTDOWN -> StatusAmber
        AutomationStatus.SENDING -> MaterialTheme.colorScheme.primary
        AutomationStatus.IDLE -> if (isAutoSendEnabled) StatusEmerald else Color.Gray
        AutomationStatus.PAUSED -> StatusAmber
        AutomationStatus.LIMIT_REACHED -> StatusRose
    }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "ArenaPilot",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "ArenaPilot",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val text = if (status == AutomationStatus.COUNTDOWN && countdownSeconds > 0) {
                            "${countdownSeconds}s"
                        } else {
                            status.label
                        }
                        Text(
                            text = text,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = statusColor
                        )
                    }
                }
            }
        },
        actions = {
            // Account & Login Button
            IconButton(
                onClick = onOpenAccount,
                modifier = Modifier.testTag("account_profile_btn")
            ) {
                Box {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Arena Account",
                        tint = if (isLoggedIn) StatusEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isLoggedIn) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(StatusEmerald)
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }

            IconButton(
                onClick = onToggleAutoSend,
                modifier = Modifier.testTag("auto_send_toggle_btn")
            ) {
                Icon(
                    imageVector = if (isAutoSendEnabled) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isAutoSendEnabled) "Pause Auto-Pilot" else "Resume Auto-Pilot",
                    tint = if (isAutoSendEnabled) StatusEmerald else Color.Gray
                )
            }

            IconButton(
                onClick = onResetSession,
                modifier = Modifier.testTag("reset_session_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Session",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}
