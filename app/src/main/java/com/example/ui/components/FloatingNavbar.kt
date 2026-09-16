package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.GlassNavbarBg
import com.example.ui.theme.PrimaryGlow
import com.example.ui.theme.Slate400

data class NavTabItem(
    val key: String,
    val title: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector,
    val hasBadge: Boolean = false
)

@Composable
fun FloatingNavbar(
    activeTab: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = remember {
        listOf(
            NavTabItem(
                key = "home",
                title = "Home",
                filledIcon = Icons.Filled.Home,
                outlinedIcon = Icons.Outlined.Home
            ),
            NavTabItem(
                key = "upcoming",
                title = "Upcoming",
                filledIcon = Icons.Filled.DateRange,
                outlinedIcon = Icons.Outlined.DateRange,
                hasBadge = true
            ),
            NavTabItem(
                key = "completed",
                title = "Completed",
                filledIcon = Icons.Filled.CheckCircle,
                outlinedIcon = Icons.Outlined.CheckCircle
            ),
            NavTabItem(
                key = "settings",
                title = "Settings",
                filledIcon = Icons.Filled.Settings,
                outlinedIcon = Icons.Outlined.Settings
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = PrimaryGlow.copy(alpha = 0.35f)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(GlassNavbarBg)
                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(28.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = activeTab == tab.key
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else Slate400,
                    label = "tab_color"
                )
                val bgModifier = if (isSelected) {
                    Modifier
                        .size(46.dp, 40.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0x26FFFFFF))
                        .border(0.5.dp, PrimaryGlow.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                } else {
                    Modifier.size(46.dp, 40.dp)
                }

                Box(
                    modifier = bgModifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(tab.key) }
                        .testTag("tab_${tab.key}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) tab.filledIcon else tab.outlinedIcon,
                        contentDescription = tab.title,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )

                    // Notification Dot (Instagram / FlowMind red dot)
                    if (tab.hasBadge) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 6.dp, end = 8.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFED4956))
                                .border(1.5.dp, Color(0x99000000), CircleShape)
                        )
                    }
                }
            }
        }
    }
}
