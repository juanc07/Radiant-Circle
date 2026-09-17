package com.thinkblox.radiantrush.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thinkblox.radiantrush.data.BadgePreview
import com.thinkblox.radiantrush.ui.components.RadiantBadgeCrest
import com.thinkblox.radiantrush.ui.components.RadiantBadgeGlyph
import com.thinkblox.radiantrush.ui.components.rememberResponsiveUiSpec

/**
 * Compact collection-first badge gallery. The old cards permanently rendered the
 * description and made six badges consume almost an entire phone screen. Details
 * are now progressive disclosure: tap a badge when you actually want the copy.
 */
@Composable
fun BadgesScreen(
    contentPadding: PaddingValues,
    badges: List<BadgePreview>,
) {
    val responsive = rememberResponsiveUiSpec()
    var selectedBadge by remember { mutableStateOf<BadgePreview?>(null) }

    val columnCount = when {
        responsive.screenWidthDp < 340 -> 2
        responsive.screenWidthDp >= 840 -> 5
        responsive.screenWidthDp >= 600 -> 4
        else -> 3
    }
    val gap = if (responsive.isTiny) 8.dp else 10.dp
    val tileHeight = if (responsive.hasLargeText) 154.dp else 142.dp

    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Fixed(columnCount),
        contentPadding = PaddingValues(
            start = responsive.screenPadding,
            top = contentPadding.calculateTopPadding() + 14.dp,
            end = responsive.screenPadding,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        items(
            items = badges,
            key = { badge -> badge.title },
        ) { badge ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(tileHeight)
                    .alpha(if (badge.unlocked) 1f else 0.66f)
                    .clickable { selectedBadge = badge },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (badge.unlocked) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    RadiantBadgeCrest(
                        glyph = RadiantBadgeGlyph.forTitle(badge.title),
                        unlocked = badge.unlocked,
                        modifier = Modifier.size(if (responsive.isTiny) 54.dp else 60.dp),
                    )
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = badge.title,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Clip,
                        )
                    }
                    Text(
                        text = if (badge.unlocked) "Earned" else "Locked",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    selectedBadge?.let { badge ->
        AlertDialog(
            onDismissRequest = { selectedBadge = null },
            icon = {
                RadiantBadgeCrest(
                    glyph = RadiantBadgeGlyph.forTitle(badge.title),
                    unlocked = badge.unlocked,
                    modifier = Modifier.size(76.dp),
                )
            },
            title = {
                Text(
                    text = badge.title,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = badge.description,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = if (badge.unlocked) "Unlocked" else "Not unlocked yet",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedBadge = null }) {
                    Text("Done")
                }
            },
        )
    }
}
