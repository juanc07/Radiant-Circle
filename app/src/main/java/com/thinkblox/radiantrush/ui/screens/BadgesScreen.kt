package com.thinkblox.radiantrush.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
    val columns = if (responsive.isCompact || responsive.hasLargeText) {
        GridCells.Fixed(1)
    } else {
        GridCells.Adaptive(minSize = 172.dp)
    }

    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = columns,
        contentPadding = PaddingValues(
            start = responsive.screenPadding,
            top = 14.dp,
            end = responsive.screenPadding,
            bottom = 24.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(badges) { badge ->
            BadgeMedallion(
                title = badge.title,
                description = badge.description,
                unlocked = badge.unlocked,
            )
        }
    }
}
