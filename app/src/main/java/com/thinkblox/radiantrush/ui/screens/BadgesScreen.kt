package com.thinkblox.radiantrush.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thinkblox.radiantrush.data.BadgePreview
import com.thinkblox.radiantrush.ui.components.BadgeMedallion
import com.thinkblox.radiantrush.ui.components.rememberResponsiveUiSpec

@Composable
fun BadgesScreen(
    contentPadding: PaddingValues,
    badges: List<BadgePreview>,
) {
    val responsive = rememberResponsiveUiSpec()

    // Badges are a visual collection, so phones should preserve a gallery
    // instead of collapsing into a one-card-per-row settings-style list.
    // Only extremely narrow layouts fall back to one column.
    val columnCount = when {
        responsive.screenWidthDp < 300 -> 1
        responsive.screenWidthDp >= 840 -> 4
        responsive.screenWidthDp >= 600 -> 3
        else -> 2
    }

    val gridGap = if (responsive.isTiny) 8.dp else 12.dp

    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Fixed(columnCount),
        contentPadding = PaddingValues(
            start = responsive.screenPadding,
            top = contentPadding.calculateTopPadding() + 14.dp,
            end = responsive.screenPadding,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(gridGap),
        verticalArrangement = Arrangement.spacedBy(gridGap),
    ) {
        items(
            items = badges,
            key = { badge -> badge.title },
        ) { badge ->
            BadgeMedallion(
                modifier = Modifier.fillMaxWidth(),
                title = badge.title,
                description = badge.description,
                unlocked = badge.unlocked,
            )
        }
    }
}
