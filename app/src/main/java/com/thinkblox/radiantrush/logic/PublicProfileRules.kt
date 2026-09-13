package com.thinkblox.radiantrush.logic

/**
 * Phase 11F public-profile rules.
 *
 * The Firebase anonymous UID remains private storage/session ownership. Public identity is
 * deliberately small: a player-chosen display name plus one bundled animal avatar. No photo
 * upload, device identifier, private wallet material, or location is stored here.
 */
object PublicProfileRules {
    const val DEFAULT_AVATAR_ID = "fox"
    const val MAX_DISPLAY_NAME_LENGTH = 20

    data class AnimalAvatar(
        val id: String,
        val label: String,
        val symbol: String,
    )

    val animalAvatars: List<AnimalAvatar> = listOf(
        AnimalAvatar("fox", "Fox", "🦊"),
        AnimalAvatar("cat", "Cat", "🐱"),
        AnimalAvatar("dog", "Dog", "🐶"),
        AnimalAvatar("panda", "Panda", "🐼"),
        AnimalAvatar("owl", "Owl", "🦉"),
        AnimalAvatar("rabbit", "Rabbit", "🐰"),
        AnimalAvatar("tiger", "Tiger", "🐯"),
        AnimalAvatar("koala", "Koala", "🐨"),
    )

    fun sanitizeDisplayName(raw: String): String {
        val clean = raw
            .replace(Regex("[\\r\\n\\t]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(MAX_DISPLAY_NAME_LENGTH)
        return clean.ifBlank { "Radiant Rookie" }
    }

    fun normalizeAvatarId(raw: String?): String =
        raw?.trim()?.takeIf { candidate -> animalAvatars.any { it.id == candidate } }
            ?: DEFAULT_AVATAR_ID

    fun avatarFor(id: String?): AnimalAvatar {
        val normalized = normalizeAvatarId(id)
        return animalAvatars.first { it.id == normalized }
    }
}
