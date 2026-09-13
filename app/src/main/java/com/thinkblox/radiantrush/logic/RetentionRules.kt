package com.thinkblox.radiantrush.logic

/** Phase 11F retention goals. These are app-progression guidance only, never payout authority. */
object RetentionRules {
    data class ProgressGoal(
        val id: String,
        val title: String,
        val detail: String,
        val progress: Int,
        val target: Int,
    ) {
        val completed: Boolean get() = progress >= target
        val safeProgress: Int get() = progress.coerceIn(0, target.coerceAtLeast(0))
    }

    data class Milestone(
        val target: Int,
        val title: String,
        val unlocked: Boolean,
    )

    private data class GoalSpec(
        val id: String,
        val title: String,
        val detail: String,
        val target: Int,
        val progress: (RetentionInput) -> Int,
    )

    data class RetentionInput(
        val utcDayKey: String,
        val currentStreak: Int,
        val collectionOwned: Int,
        val collectionTotal: Int,
        val dailyRuns: Int,
        val dailyPerfectHits: Int,
        val dailyBestScore: Int,
        val dailyBestCombo: Int,
        val dailyGameplayXp: Int,
        val weeklyRuns: Int,
        val weeklyRankedRuns: Int,
        val weeklyChests: Int,
    )

    fun dailyGoals(input: RetentionInput): List<ProgressGoal> {
        val specs = listOf(
            GoalSpec("run_once", "Run the light", "Finish 1 Radiant Run today.", 1) { it.dailyRuns },
            GoalSpec("perfect_five", "Perfect pulse", "Land 5 PERFECT hits today.", 5) { it.dailyPerfectHits },
            GoalSpec("xp_hundred", "Momentum", "Earn 100 gameplay XP today.", 100) { it.dailyGameplayXp },
            GoalSpec("score_1200", "Bright score", "Reach 1,200 in one run today.", 1200) { it.dailyBestScore },
            GoalSpec("combo_eight", "Combo spark", "Reach an 8-hit combo today.", 8) { it.dailyBestCombo },
        )
        // Stable UTC-day rotation. Using character codes keeps the selection deterministic
        // across devices without locale/time-zone differences.
        val seed = input.utcDayKey.fold(0) { acc, char -> (acc * 31 + char.code) and 0x7fffffff }
        val start = seed % specs.size
        return (0 until 3).map { offset ->
            val spec = specs[(start + offset * 2) % specs.size]
            ProgressGoal(
                id = spec.id,
                title = spec.title,
                detail = spec.detail,
                progress = spec.progress(input).coerceAtLeast(0),
                target = spec.target,
            )
        }.distinctBy { it.id }.let { goals ->
            // The step of two is coprime with five, so this should always be three unique goals.
            // Keep a defensive fallback in case the pool size changes later.
            if (goals.size == 3) goals else specs.take(3).map { spec ->
                ProgressGoal(spec.id, spec.title, spec.detail, spec.progress(input).coerceAtLeast(0), spec.target)
            }
        }
    }

    fun weeklyGoals(input: RetentionInput): List<ProgressGoal> = listOf(
        ProgressGoal("weekly_runs", "Weekly runner", "Finish 5 Radiant Runs this week.", input.weeklyRuns, 5),
        ProgressGoal("weekly_ranked", "Cup contender", "Finish 3 Ranked runs this week.", input.weeklyRankedRuns, 3),
        ProgressGoal("weekly_chests", "Chest rhythm", "Open 3 Daily Radiant Chests this week.", input.weeklyChests, 3),
    )

    fun nextStreakMilestone(currentStreak: Int): Milestone {
        val safe = currentStreak.coerceAtLeast(0)
        val milestones = listOf(
            3 to "Spark Flame",
            7 to "Radiant Flame",
            14 to "Aurora Flame",
            30 to "Solar Crown",
        )
        val next = milestones.firstOrNull { safe < it.first } ?: milestones.last()
        return Milestone(next.first, next.second, safe >= next.first)
    }

    fun nextCollectionMilestone(collectionOwned: Int, collectionTotal: Int): Milestone {
        val safeOwned = collectionOwned.coerceAtLeast(0)
        val safeTotal = collectionTotal.coerceAtLeast(1)
        val firstTarget = minOf(3, safeTotal)
        return if (safeOwned < firstTarget) {
            Milestone(firstTarget, "Collector Crest", false)
        } else {
            Milestone(safeTotal, "Radiant Vault Complete", safeOwned >= safeTotal)
        }
    }

    fun nextAction(dailyGoals: List<ProgressGoal>, weeklyGoals: List<ProgressGoal>): ProgressGoal? =
        dailyGoals.firstOrNull { !it.completed } ?: weeklyGoals.firstOrNull { !it.completed }
}
