package com.thinkblox.radiantrush.logic

import com.thinkblox.radiantrush.data.DailyRadiancePreview
import com.thinkblox.radiantrush.data.FirebaseStatus
import com.thinkblox.radiantrush.data.QuestIds
import com.thinkblox.radiantrush.data.QuestPreview
import com.thinkblox.radiantrush.data.QuestStatus
import com.thinkblox.radiantrush.data.RadiantChestPreview
import com.thinkblox.radiantrush.data.RadiantChestStatus
import com.thinkblox.radiantrush.data.RushUiState

/**
 * Prevents background Firebase hydration from visually rolling today's progress backward.
 *
 * Repository bootstrapping intentionally emits a fast profile-backed state before slower
 * completion-history / leaderboard / Cup reads finish. Those intermediate snapshots are useful
 * for keeping the app responsive, but they must not make already-known daily actions briefly look
 * incomplete or clickable again.
 *
 * These rules are a UI consistency layer only. Firestore remains the source of truth; the merge
 * applies only within the same UTC/local day key already selected by the repository. A new day
 * always accepts the incoming state so daily progress can reset normally.
 */
object DailyPlanRefreshRules {
    fun stabilize(current: RushUiState, incoming: RushUiState): RushUiState {
        if (current.todayKey != incoming.todayKey) return incoming
        if (current.todayKey == "local-preview") return incoming

        // A failed background refresh must not destroy a previously usable snapshot or disable
        // wallet/actions. Keep the last good screen and surface only the latest message.
        if (
            current.firebaseStatus == FirebaseStatus.Ready &&
            incoming.firebaseStatus == FirebaseStatus.Error
        ) {
            return current.copy(
                dailyRadiance = current.dailyRadiance.copy(opening = false),
                backgroundSyncInProgress = false,
                lastMessage = incoming.lastMessage,
            )
        }

        val currentById = current.quests.associateBy { it.id }
        val mergedQuests = incoming.quests.map { nextQuest ->
            val previousQuest = currentById[nextQuest.id] ?: return@map nextQuest
            stabilizeQuest(
                previous = previousQuest,
                incoming = nextQuest,
            )
        }

        return incoming.copy(
            quests = mergedQuests,
            dailyRadiance = stabilizeDailyRadiance(current.dailyRadiance, incoming.dailyRadiance),
            radiantChest = stabilizeChest(current.radiantChest, incoming.radiantChest),
        )
    }

    private fun stabilizeQuest(
        previous: QuestPreview,
        incoming: QuestPreview,
    ): QuestPreview {
        // Wallet connection is intentionally reversible. If the user disconnects, the current
        // wallet CTA must become available again even if it had been completed earlier today.
        if (incoming.id == QuestIds.WALLET_CONNECT) {
            return incoming
        }

        // All other daily completions are monotonic for a given day. Intermediate profile-only
        // snapshots are not allowed to turn Done back into Ready / Wallet / Syncing.
        if (previous.status == QuestStatus.Completed && incoming.status != QuestStatus.Completed) {
            return incoming.copy(status = QuestStatus.Completed)
        }

        // A profile-first bootstrap can mark a not-yet-hydrated quest as Syncing. If we already
        // know a stable state for that quest, keep it until the authoritative history read lands.
        if (incoming.status == QuestStatus.Syncing && previous.status != QuestStatus.Syncing) {
            return incoming.copy(status = previous.status)
        }

        return incoming
    }

    private fun stabilizeDailyRadiance(
        previous: DailyRadiancePreview,
        incoming: DailyRadiancePreview,
    ): DailyRadiancePreview {
        // Revealing today's Radiance is monotonic for the selected day. A slower profile
        // snapshot must not briefly hide the message after the successful write returns.
        if (previous.revealedToday && !incoming.revealedToday) {
            return previous.copy(opening = incoming.opening)
        }
        return incoming
    }

    private fun stabilizeChest(
        previous: RadiantChestPreview,
        incoming: RadiantChestPreview,
    ): RadiantChestPreview = when {
        previous.status == RadiantChestStatus.Claimed &&
            incoming.status != RadiantChestStatus.Claimed -> previous

        previous.status == RadiantChestStatus.Ready &&
            incoming.status == RadiantChestStatus.Locked -> previous

        else -> incoming
    }
}
