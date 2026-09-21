package com.thinkblox.radiantrush.logic

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Pure Phase 14C rules for the lightweight Daily Radiance ritual.
 *
 * Daily Radiance is social/account progression only. It grants no token value,
 * never changes wallet identity, and is intentionally independent from Radiant
 * Rush / Weekly Cup trust and payout state.
 */
object DailyRadianceRules {
    data class Content(
        val id: String,
        val category: String,
        val message: String,
    )

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * 15 categories x 10 messages = 150 unique Daily Radiance messages.
     *
     * The original Phase 13A IDs are intentionally preserved so previously
     * stored Daily Radiance records continue to resolve after this expansion.
     */
    private val messages = listOf(
        // Courage
        Content("courage-small-step", "Courage", "Your next opportunity may begin with one small action today."),
        Content("courage-begin-before-ready", "Courage", "You do not need to feel completely ready before you begin."),
        Content("courage-honest-choice", "Courage", "Choose the honest next step, even when the easier one is louder."),
        Content("courage-try-once", "Courage", "Give yourself permission to try once before deciding you cannot."),
        Content("courage-speak-clearly", "Courage", "Say what matters with clarity and kindness today."),
        Content("courage-new-route", "Courage", "A different route can still lead somewhere worth reaching."),
        Content("courage-one-more", "Courage", "One more thoughtful attempt can change the shape of the day."),
        Content("courage-uncertain-step", "Courage", "A careful step into uncertainty is still progress."),
        Content("courage-protect-priority", "Courage", "Protect one priority that deserves your attention today."),
        Content("courage-choose-forward", "Courage", "You can acknowledge the fear and still choose forward."),

        // Focus
        Content("focus-one-thing", "Focus", "Give one important thing your full attention before the day gets noisy."),
        Content("focus-next-action", "Focus", "Find the next useful action and let the rest wait for its turn."),
        Content("focus-fewer-tabs", "Focus", "Fewer open loops can make room for better work."),
        Content("focus-protect-hour", "Focus", "Protect one quiet block of time for what matters most."),
        Content("focus-finish-before-more", "Focus", "Finishing one thing can be more powerful than starting five."),
        Content("focus-small-target", "Focus", "Make the target smaller until the next step becomes obvious."),
        Content("focus-remove-one-distraction", "Focus", "Remove one distraction before asking yourself for more discipline."),
        Content("focus-return-gently", "Focus", "When your attention drifts, simply bring it back without judgment."),
        Content("focus-decision", "Focus", "A clear decision can save more energy than another hour of hesitation."),
        Content("focus-important-not-urgent", "Focus", "Give something important attention before it becomes urgent."),

        // Kindness
        Content("kindness-soft-word", "Kindness", "A thoughtful word can travel farther than you expect today."),
        Content("kindness-small-help", "Kindness", "A small act of help can make someone’s difficult day lighter."),
        Content("kindness-yourself-too", "Kindness", "Offer yourself the same patience you would give someone you care about."),
        Content("kindness-listen-first", "Kindness", "Listening fully can be a generous thing to give."),
        Content("kindness-credit", "Kindness", "Give credit freely when someone helped move things forward."),
        Content("kindness-check-in", "Kindness", "A simple check-in can remind someone they are not overlooked."),
        Content("kindness-assume-room", "Kindness", "Leave room for the possibility that someone is carrying more than you can see."),
        Content("kindness-thank-specific", "Kindness", "Make one thank-you specific enough to be remembered."),
        Content("kindness-pause-reply", "Kindness", "A short pause before replying can turn tension into understanding."),
        Content("kindness-share-win", "Kindness", "Celebrate someone else’s progress without making yours feel smaller."),

        // Creativity
        Content("creativity-first-idea", "Creativity", "The first idea does not need to be perfect. It only needs somewhere to begin."),
        Content("creativity-combine", "Creativity", "Try combining two ordinary ideas in a way you have not seen before."),
        Content("creativity-bad-draft", "Creativity", "A rough draft gives your better ideas something to improve."),
        Content("creativity-change-constraint", "Creativity", "If you feel stuck, change one constraint instead of forcing the same approach."),
        Content("creativity-collect", "Creativity", "Collect one interesting detail today; it may become useful later."),
        Content("creativity-play", "Creativity", "Make a little room for play before demanding an answer."),
        Content("creativity-other-angle", "Creativity", "Look at the problem from the user’s side, the beginner’s side, or the opposite side."),
        Content("creativity-make-visible", "Creativity", "Put the idea on paper or screen so your mind can react to something real."),
        Content("creativity-quiet-idea", "Creativity", "The quieter idea may deserve a second look."),
        Content("creativity-iterate", "Creativity", "You are allowed to improve the idea after you start."),

        // Calm
        Content("calm-own-pace", "Calm", "You do not have to match anyone else’s pace to make meaningful progress."),
        Content("calm-one-breath", "Calm", "Give yourself one slow breath before the next decision."),
        Content("calm-not-all-now", "Calm", "Not everything needs to be solved today."),
        Content("calm-name-next", "Calm", "Name the next step instead of carrying the whole problem at once."),
        Content("calm-space", "Calm", "A few quiet minutes can be productive when they help you return with clarity."),
        Content("calm-lower-volume", "Calm", "Lower the noise around you before assuming the answer is missing."),
        Content("calm-steady", "Calm", "Steady effort can take you farther than urgency sustained too long."),
        Content("calm-release-rush", "Calm", "You can move with purpose without rushing yourself."),
        Content("calm-reset", "Calm", "A difficult hour does not have to decide the rest of your day."),
        Content("calm-simple", "Calm", "Choose the simplest next move that still respects what matters."),

        // Curiosity
        Content("curiosity-one-question", "Curiosity", "One good question can open a door that certainty would have missed."),
        Content("curiosity-why-not", "Curiosity", "Ask what would change if one assumption turned out to be wrong."),
        Content("curiosity-learn-one", "Curiosity", "Learn one small thing today that yesterday’s version of you did not know."),
        Content("curiosity-person", "Curiosity", "Someone around you knows something worth asking about."),
        Content("curiosity-observe", "Curiosity", "Notice what actually happens before deciding what it means."),
        Content("curiosity-beginner", "Curiosity", "Look at something familiar as if you were seeing it for the first time."),
        Content("curiosity-follow-thread", "Curiosity", "Follow one interesting thread a little farther than usual."),
        Content("curiosity-better-question", "Curiosity", "When an answer feels weak, try improving the question."),
        Content("curiosity-test", "Curiosity", "Turn one assumption into a small experiment."),
        Content("curiosity-listen-difference", "Curiosity", "A different viewpoint can teach you something without requiring you to adopt it."),

        // Momentum
        Content("momentum-finish-small", "Momentum", "Finish one small thing today and let that win create the next one."),
        Content("momentum-five-minutes", "Momentum", "Five focused minutes are enough to restart something that has stalled."),
        Content("momentum-show-up", "Momentum", "Showing up consistently often matters more than having a perfect day."),
        Content("momentum-next-inch", "Momentum", "Move the work forward one visible inch."),
        Content("momentum-start-easy", "Momentum", "Start with the easiest meaningful step and build from there."),
        Content("momentum-close-loop", "Momentum", "Close one open loop that has been taking up mental space."),
        Content("momentum-keep-chain", "Momentum", "A modest win today can protect the rhythm you built yesterday."),
        Content("momentum-after-miss", "Momentum", "Missing once does not erase your progress; restart with the next action."),
        Content("momentum-use-energy", "Momentum", "Use the energy you have now on something that will matter later."),
        Content("momentum-done-visible", "Momentum", "Make progress visible so your effort has something to point to."),

        // Connection
        Content("connection-reach-out", "Connection", "Someone may be glad you reached out first."),
        Content("connection-shared-spark", "Connection", "A shared interest can be the beginning of a meaningful conversation."),
        Content("connection-ask-real", "Connection", "Ask one question today that invites a real answer."),
        Content("connection-presence", "Connection", "Being fully present can matter more than finding the perfect words."),
        Content("connection-follow-up", "Connection", "Remembering to follow up is one way to show that a conversation mattered."),
        Content("connection-introduce", "Connection", "Introduce two people who might genuinely help or understand each other."),
        Content("connection-small-opening", "Connection", "A simple hello can create an opening that did not exist a moment ago."),
        Content("connection-listen-detail", "Connection", "Remember one small detail someone shares with you today."),
        Content("connection-no-performance", "Connection", "You do not have to perform to be worth knowing."),
        Content("connection-make-time", "Connection", "Make a little time for someone who makes life feel more human."),

        // Confidence
        Content("confidence-earned", "Confidence", "Trust the skills you have already earned, then take the next step."),
        Content("confidence-evidence", "Confidence", "Remember one hard thing you already learned how to do."),
        Content("confidence-not-certainty", "Confidence", "Confidence does not require certainty; it requires willingness to act and adjust."),
        Content("confidence-voice", "Confidence", "Your perspective deserves a clear sentence, not an apology for existing."),
        Content("confidence-practice", "Confidence", "Practice is evidence that you are becoming more capable."),
        Content("confidence-prepare", "Confidence", "Preparation can carry you when confidence feels quiet."),
        Content("confidence-comparison", "Confidence", "Measure today against your own direction, not someone else’s highlight reel."),
        Content("confidence-decide", "Confidence", "Make one decision today without asking fear to approve it first."),
        Content("confidence-recover", "Confidence", "Knowing you can recover is sometimes more useful than knowing you will be perfect."),
        Content("confidence-take-space", "Confidence", "Take the space needed to explain your idea clearly."),

        // Perspective
        Content("perspective-room", "Perspective", "Leave a little room today for a better answer than the first one you see."),
        Content("perspective-zoom-out", "Perspective", "Zoom out before letting one difficult moment define the whole day."),
        Content("perspective-future-self", "Perspective", "Ask what your future self would be glad you handled today."),
        Content("perspective-control", "Perspective", "Put your energy into what you can influence and name the rest honestly."),
        Content("perspective-not-personal", "Perspective", "Not every delay, silence, or setback is a judgment about you."),
        Content("perspective-long-game", "Perspective", "A slower choice can still be the stronger move in the long game."),
        Content("perspective-reframe", "Perspective", "Try describing the problem without using the word impossible."),
        Content("perspective-two-truths", "Perspective", "Two things can be true: this is hard, and you can still move forward."),
        Content("perspective-enough", "Perspective", "What feels small today may be exactly enough for this stage."),
        Content("perspective-lesson", "Perspective", "Ask what this moment is teaching before deciding what it cost."),

        // Gratitude
        Content("gratitude-notice", "Gratitude", "Notice one ordinary thing that quietly made your day better."),
        Content("gratitude-person", "Gratitude", "Think of one person whose effort made something easier for you."),
        Content("gratitude-progress", "Gratitude", "Notice one ability you have now that once took effort to learn."),
        Content("gratitude-place", "Gratitude", "Appreciate one place that gives you room to think, work, or rest."),
        Content("gratitude-tool", "Gratitude", "Notice one simple tool or habit that quietly supports your day."),
        Content("gratitude-body", "Gratitude", "Thank your body for one ordinary thing it helped you do today."),
        Content("gratitude-memory", "Gratitude", "Let one good memory have your full attention for a moment."),
        Content("gratitude-enough", "Gratitude", "Name one thing you already have that you once hoped for."),
        Content("gratitude-effort", "Gratitude", "Recognize the effort behind something you usually take for granted."),
        Content("gratitude-share", "Gratitude", "If someone helped you, let them know while the gratitude is fresh."),

        // Hope
        Content("hope-next-page", "Hope", "A difficult chapter is still only one part of the story you are building."),
        Content("hope-small-light", "Hope", "Look for one small sign that tomorrow does not have to repeat today."),
        Content("hope-options", "Hope", "You may have more options than the tired version of you can see right now."),
        Content("hope-rebuild", "Hope", "Something can be rebuilt differently instead of merely restored."),
        Content("hope-keep-door", "Hope", "Keep one door open for an outcome better than the one you fear."),
        Content("hope-morning", "Hope", "A new day does not solve everything, but it gives you another place to begin."),
        Content("hope-progress-hidden", "Hope", "Some progress is happening before it becomes visible."),
        Content("hope-one-reason", "Hope", "One honest reason to continue is enough for the next step."),
        Content("hope-unwritten", "Hope", "The part you cannot predict is also the part where new possibilities can appear."),
        Content("hope-build", "Hope", "Hope becomes stronger when you give it one practical action."),

        // Resilience
        Content("resilience-return", "Resilience", "Your strength is not never falling behind; it is knowing how to return."),
        Content("resilience-adjust", "Resilience", "Changing the plan is not the same as abandoning the goal."),
        Content("resilience-rest", "Resilience", "Rest can protect the effort you want to sustain."),
        Content("resilience-learn", "Resilience", "Take the lesson from the setback without making it your identity."),
        Content("resilience-next-round", "Resilience", "You are allowed to regroup before the next round."),
        Content("resilience-flex", "Resilience", "Being flexible can be a form of strength."),
        Content("resilience-after-no", "Resilience", "One no does not decide every future answer."),
        Content("resilience-repair", "Resilience", "Repair what you can, release what you cannot, and keep moving."),
        Content("resilience-small-recovery", "Resilience", "A small recovery today is still a recovery."),
        Content("resilience-proof", "Resilience", "You have already adapted to things you once thought would stop you."),

        // Growth
        Content("growth-one-percent", "Growth", "A small improvement repeated often can become a major difference."),
        Content("growth-feedback", "Growth", "Useful feedback is information, not a verdict on your worth."),
        Content("growth-beginner", "Growth", "Being new at something is evidence that you are expanding your range."),
        Content("growth-practice-edge", "Growth", "Spend a little time today just beyond what already feels easy."),
        Content("growth-update", "Growth", "You are allowed to update an old belief when new evidence arrives."),
        Content("growth-compare-past", "Growth", "Look back far enough to notice what now feels easier than it once did."),
        Content("growth-question-habit", "Growth", "Question one habit that no longer fits where you are going."),
        Content("growth-slow-skill", "Growth", "Some skills grow quietly before they become obvious."),
        Content("growth-teach", "Growth", "Explaining what you know can reveal what you are ready to learn next."),
        Content("growth-next-version", "Growth", "Build today for the version of you who will use what you learn tomorrow."),

        // Self-Belief
        Content("self-belief-capable", "Self-Belief", "You can be unsure and still be capable."),
        Content("self-belief-own-proof", "Self-Belief", "Your past effort is evidence that you can learn the next thing too."),
        Content("self-belief-not-late", "Self-Belief", "You are not late to a path that is still yours to choose."),
        Content("self-belief-worth-trying", "Self-Belief", "Your idea is worth testing before you talk yourself out of it."),
        Content("self-belief-trust-judgment", "Self-Belief", "Trust your judgment enough to make one thoughtful choice today."),
        Content("self-belief-room", "Self-Belief", "You do not need to shrink your contribution to make others comfortable."),
        Content("self-belief-learnable", "Self-Belief", "What you do not know yet can still be learnable."),
        Content("self-belief-own-pace", "Self-Belief", "Your pace does not cancel the value of your progress."),
        Content("self-belief-restart", "Self-Belief", "Starting again can be evidence of belief, not failure."),
        Content("self-belief-keep-promise", "Self-Belief", "Keep one small promise to yourself today and let trust grow from there."),
    )

    private val messagesByCategory: List<List<Content>> = messages
        .groupBy { it.category }
        .values
        .toList()

    /**
     * Chooses a stable message from Radiant Circle account identity + local day.
     * Wallet address is deliberately not part of the seed, so switching wallets
     * can never change a user's Daily Radiance identity or daily message.
     *
     * For valid ISO dates, categories rotate daily and the message within each
     * category advances when that category returns. With 15 categories x 10
     * messages, the same account receives every message before the 150-day cycle
     * repeats. This removes the frequent near-term repetition from Phase 13A.
     */
    fun contentFor(accountId: String, dayKey: String): Content {
        val normalizedAccount = accountId.trim().ifBlank { "radiant-circle" }
        val parsedDay = parseDay(dayKey)

        if (parsedDay != null) {
            val accountSeed = stableSeed(normalizedAccount)
            val epochDay = parsedDay.toEpochDay()
            val categoryIndex = Math.floorMod(
                accountSeed + epochDay,
                messagesByCategory.size.toLong(),
            ).toInt()
            val categoryMessages = messagesByCategory[categoryIndex]
            val categoryRotation = Math.floorDiv(epochDay, messagesByCategory.size.toLong())
            val messageIndex = Math.floorMod(
                mixSeed(accountSeed) + categoryRotation,
                categoryMessages.size.toLong(),
            ).toInt()
            return categoryMessages[messageIndex]
        }

        // Defensive fallback for malformed/non-ISO day keys.
        val normalizedDay = dayKey.trim()
        val seed = stableSeed("$normalizedAccount|$normalizedDay")
        return messages[Math.floorMod(seed, messages.size.toLong()).toInt()]
    }

    fun contentById(id: String?): Content? {
        val cleanId = id?.trim().orEmpty()
        if (cleanId.isBlank()) return null
        return messages.firstOrNull { it.id == cleanId }
    }

    /** Exposed to unit tests so accidental catalog shrinkage is caught. */
    internal fun contentCount(): Int = messages.size

    /** Exposed to unit tests so category coverage stays intentional. */
    internal fun categoryCount(): Int = messagesByCategory.size

    /**
     * Streak state is intentionally nonfinancial. A same-day retry is idempotent;
     * yesterday continues the streak; any larger gap starts again at one.
     */
    fun nextStreak(
        lastOpenedDay: String?,
        todayKey: String,
        currentStreak: Int,
    ): Int {
        if (lastOpenedDay == todayKey) return currentStreak.coerceAtLeast(1)

        val last = parseDay(lastOpenedDay)
        val today = parseDay(todayKey)
        return if (last != null && today != null && last.plusDays(1) == today) {
            currentStreak.coerceAtLeast(0) + 1
        } else {
            1
        }
    }

    /**
     * Before today's reveal, yesterday's streak remains visible as momentum to
     * continue. Older streaks are shown as zero rather than pretending they are live.
     */
    fun visibleStreak(
        lastOpenedDay: String?,
        todayKey: String,
        savedStreak: Int,
    ): Int {
        val today = parseDay(todayKey) ?: return 0
        val last = parseDay(lastOpenedDay) ?: return 0
        return when {
            last == today -> savedStreak.coerceAtLeast(0)
            last.plusDays(1) == today -> savedStreak.coerceAtLeast(0)
            else -> 0
        }
    }

    private fun stableSeed(value: String): Long {
        var seed = 1125899906842597L
        value.forEach { character ->
            seed = seed * 31L + character.code.toLong()
        }
        return seed
    }

    private fun mixSeed(seed: Long): Long {
        var value = seed
        value = value xor (value ushr 33)
        value *= -49064778989728563L
        value = value xor (value ushr 33)
        value *= -4265267296055464877L
        value = value xor (value ushr 33)
        return value
    }

    private fun parseDay(value: String?): LocalDate? = runCatching {
        value?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { LocalDate.parse(it, dateFormatter) }
    }.getOrNull()
}
