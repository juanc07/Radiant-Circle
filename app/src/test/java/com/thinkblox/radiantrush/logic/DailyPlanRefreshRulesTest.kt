package com.thinkblox.radiantrush.logic

import com.thinkblox.radiantrush.data.FirebaseStatus
import com.thinkblox.radiantrush.data.PreviewContent
import com.thinkblox.radiantrush.data.QuestIds
import com.thinkblox.radiantrush.data.QuestStatus
import com.thinkblox.radiantrush.data.RadiantChestPreview
import com.thinkblox.radiantrush.data.RadiantChestStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyPlanRefreshRulesTest {
    private val day = "2026-09-14"

    @Test
    fun completedDailyCheckInDoesNotFlickerBackToReadyDuringRefresh() {
        val current = state(
            questStatuses = mapOf(QuestIds.DAILY_CHECK_IN to QuestStatus.Completed),
        )
        val incoming = state(
            questStatuses = mapOf(QuestIds.DAILY_CHECK_IN to QuestStatus.Ready),
            backgroundSync = true,
        )

        val merged = DailyPlanRefreshRules.stabilize(current, incoming)

        assertEquals(
            QuestStatus.Completed,
            merged.quests.first { it.id == QuestIds.DAILY_CHECK_IN }.status,
        )
    }

    @Test
    fun completedDailyActionDoesNotRegressToHydrationSpinner() {
        val current = state(
            questStatuses = mapOf(QuestIds.DAILY_CHECK_IN to QuestStatus.Completed),
        )
        val incoming = state(
            questStatuses = mapOf(QuestIds.DAILY_CHECK_IN to QuestStatus.Syncing),
            backgroundSync = true,
        )

        val merged = DailyPlanRefreshRules.stabilize(current, incoming)

        assertEquals(
            QuestStatus.Completed,
            merged.quests.first { it.id == QuestIds.DAILY_CHECK_IN }.status,
        )
    }

    @Test
    fun readyChestDoesNotFlashLockedWhileCompletionHistoryHydrates() {
        val current = state(
            chest = RadiantChestPreview(status = RadiantChestStatus.Ready, buttonLabel = "Open Chest"),
        )
        val incoming = state(
            chest = RadiantChestPreview(status = RadiantChestStatus.Locked, buttonLabel = "Locked"),
            backgroundSync = true,
        )

        val merged = DailyPlanRefreshRules.stabilize(current, incoming)

        assertEquals(RadiantChestStatus.Ready, merged.radiantChest.status)
        assertEquals("Open Chest", merged.radiantChest.buttonLabel)
    }

    @Test
    fun claimedChestDoesNotRegressDuringRefresh() {
        val current = state(
            chest = RadiantChestPreview(status = RadiantChestStatus.Claimed, buttonLabel = "Come Back Tomorrow"),
        )
        val incoming = state(
            chest = RadiantChestPreview(status = RadiantChestStatus.Locked, buttonLabel = "Locked"),
            backgroundSync = true,
        )

        val merged = DailyPlanRefreshRules.stabilize(current, incoming)

        assertEquals(RadiantChestStatus.Claimed, merged.radiantChest.status)
    }

    @Test
    fun walletQuestCanBecomeReadyAgainAfterDisconnect() {
        val connected = PreviewContent.user.copy(
            walletStatus = "Wallet connected",
            walletAddress = "Wallet111",
        )
        val disconnected = PreviewContent.user.copy(
            walletStatus = "Wallet not connected yet",
            walletAddress = "Connect your Solana wallet",
        )
        val current = state(
            user = connected,
            questStatuses = mapOf(QuestIds.WALLET_CONNECT to QuestStatus.Completed),
        )
        val incoming = state(
            user = disconnected,
            questStatuses = mapOf(QuestIds.WALLET_CONNECT to QuestStatus.Ready),
        )

        val merged = DailyPlanRefreshRules.stabilize(current, incoming)

        assertEquals(
            QuestStatus.Ready,
            merged.quests.first { it.id == QuestIds.WALLET_CONNECT }.status,
        )
    }


    @Test
    fun backgroundRefreshErrorKeepsLastGoodDailyPlanUsable() {
        val current = state(
            questStatuses = mapOf(QuestIds.DAILY_CHECK_IN to QuestStatus.Completed),
            backgroundSync = true,
        )
        val incoming = PreviewContent.defaultState().copy(
            firebaseStatus = FirebaseStatus.Error,
            todayKey = day,
            lastMessage = "Temporary refresh failure",
        )

        val merged = DailyPlanRefreshRules.stabilize(current, incoming)

        assertEquals(FirebaseStatus.Ready, merged.firebaseStatus)
        assertEquals("Temporary refresh failure", merged.lastMessage)
        assertEquals(
            QuestStatus.Completed,
            merged.quests.first { it.id == QuestIds.DAILY_CHECK_IN }.status,
        )
    }

    @Test
    fun newDayAcceptsIncomingReset() {
        val current = state(
            questStatuses = mapOf(QuestIds.DAILY_CHECK_IN to QuestStatus.Completed),
        )
        val incoming = state(
            day = "2026-09-15",
            questStatuses = mapOf(QuestIds.DAILY_CHECK_IN to QuestStatus.Ready),
        )

        val merged = DailyPlanRefreshRules.stabilize(current, incoming)

        assertEquals(
            QuestStatus.Ready,
            merged.quests.first { it.id == QuestIds.DAILY_CHECK_IN }.status,
        )
    }

    private fun state(
        day: String = this.day,
        user: com.thinkblox.radiantrush.data.UserPreview = PreviewContent.user,
        questStatuses: Map<String, QuestStatus> = emptyMap(),
        chest: RadiantChestPreview = PreviewContent.radiantChest,
        backgroundSync: Boolean = false,
    ) = PreviewContent.defaultState().copy(
        firebaseStatus = FirebaseStatus.Ready,
        todayKey = day,
        user = user,
        quests = PreviewContent.quests.map { quest ->
            quest.copy(status = questStatuses[quest.id] ?: quest.status)
        },
        radiantChest = chest,
        backgroundSyncInProgress = backgroundSync,
    )
}
