package com.parsfilo.contentapp.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.parsfilo.contentapp.R
import com.parsfilo.contentapp.core.designsystem.tokens.LocalDimens
import com.parsfilo.contentapp.navigation.AppRoute

@Composable
fun AppBottomBarWithFab(
    currentDestination: NavDestination?,
    onNavigateToDestination: (AppRoute) -> Unit,
    unreadNotificationCount: Int,
    unreadMessageCount: Int,
    newOtherAppsCount: Int,
    shouldShowSubscriptionBadge: Boolean,
) {
    val dimens = LocalDimens.current
    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val view = LocalView.current
    val systemNavBottomInsetPx =
        ViewCompat
            .getRootWindowInsets(view)
            ?.getInsets(WindowInsetsCompat.Type.navigationBars())
            ?.bottom
            ?.toFloat() ?: with(density) { 48.dp.toPx() }

    val destinations =
        listOf(
            BottomDestination(
                "subscription",
                AppRoute.Subscription,
                stringResource(R.string.nav_premium),
                R.drawable.ic_star,
            ),
            BottomDestination(
                "other_apps",
                AppRoute.OtherApps,
                stringResource(R.string.nav_apps),
                R.drawable.ic_apps,
            ),
            BottomDestination(
                "home_graph",
                AppRoute.HomeGraph,
                stringResource(R.string.nav_home),
                R.drawable.ic_home,
            ),
            BottomDestination(
                "messages_graph",
                AppRoute.MessagesGraph,
                stringResource(R.string.nav_messages),
                R.drawable.ic_email,
            ),
            BottomDestination(
                "notifications_graph",
                AppRoute.NotificationsGraph,
                stringResource(R.string.nav_alerts),
                R.drawable.ic_notifications,
            ),
        )

    val selectedId =
        destinations
            .firstOrNull { destination ->
                currentDestination?.hierarchy?.any { it.route == destination.route.route } == true
            }?.id ?: destinations[2].id

    NavigationBar(
        modifier =
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    val stroke = 2.dp.toPx()
                    val insetTop = size.height - systemNavBottomInsetPx - (stroke / 2f)
                    drawLine(
                        color = colorScheme.onSurface.copy(alpha = 0.82f),
                        start = Offset(0f, insetTop),
                        end = Offset(size.width, insetTop),
                        strokeWidth = stroke,
                    )
                },
        tonalElevation = dimens.elevationLow,
        containerColor = colorScheme.surface,
    ) {
        destinations.forEach { destination ->
            val badge =
                when (destination.route) {
                    AppRoute.NotificationsGraph ->
                        if (BadgeFeatureFlags.SHOW_NOTIFICATIONS_BADGE) {
                            unreadNotificationCount.toBadgeText()
                        } else {
                            null
                        }

                    AppRoute.OtherApps ->
                        if (BadgeFeatureFlags.SHOW_OTHER_APPS_BADGE) {
                            newOtherAppsCount.toBadgeText()
                        } else {
                            null
                        }

                    AppRoute.MessagesGraph ->
                        if (BadgeFeatureFlags.SHOW_MESSAGES_BADGE) {
                            unreadMessageCount.toBadgeText()
                        } else {
                            null
                        }

                    AppRoute.Subscription ->
                        if (
                            BadgeFeatureFlags.SHOW_SUBSCRIPTION_BADGE && shouldShowSubscriptionBadge
                        ) {
                            "!"
                        } else {
                            null
                        }

                    else -> null
                }

            val selected = destination.id == selectedId
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigateToDestination(destination.route) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (!badge.isNullOrBlank()) {
                                Badge(
                                    containerColor = colorScheme.error,
                                    contentColor = colorScheme.onError,
                                ) {
                                    Text(text = badge)
                                }
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(destination.icon),
                            contentDescription = destination.title,
                        )
                    }
                },
                label = {
                    Text(
                        text = destination.title,
                        fontSize = 10.sp,
                    )
                },
                alwaysShowLabel = true,
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = colorScheme.onSecondaryContainer,
                        selectedTextColor = colorScheme.onSecondaryContainer,
                        indicatorColor = colorScheme.secondaryContainer,
                        unselectedIconColor = colorScheme.onSurfaceVariant,
                        unselectedTextColor = colorScheme.onSurfaceVariant,
                    ),
            )
        }
    }
}

private data class BottomDestination(
    val id: String,
    val route: AppRoute,
    val title: String,
    val icon: Int,
)

private fun Int.toBadgeText(): String? =
    when {
        this <= 0 -> null
        this > 9 -> "9+"
        else -> toString()
    }
