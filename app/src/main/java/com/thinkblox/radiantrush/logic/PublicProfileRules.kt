package com.thinkblox.radiantrush.logic

/**
 * Phase 11F public-profile rules.
 *
 * Firebase Anonymous Auth remains the private storage/session owner. Public identity is
 * deliberately small: a player-chosen display name plus one bundled emoji avatar. No photo
 * upload, device identifier, private wallet material, or location is stored here.
 */
object PublicProfileRules {
    const val DEFAULT_AVATAR_ID = "fox"
    const val MAX_DISPLAY_NAME_LENGTH = 20

    data class ProfileAvatar(
        val id: String,
        val label: String,
        val symbol: String,
        val category: String,
    )

    val avatarCategories: List<String> = listOf(
        "Animals",
        "Cosmic",
        "Mystic",
        "Spooky",
        "Zodiac",
        "Faces",
        "Nature",
        "Food",
        "Weird",
    )

    val profileAvatars: List<ProfileAvatar> = listOf(
        // Animals
        ProfileAvatar("fox", "Fox", "🦊", "Animals"),
        ProfileAvatar("cat", "Cat", "🐱", "Animals"),
        ProfileAvatar("dog", "Dog", "🐶", "Animals"),
        ProfileAvatar("panda", "Panda", "🐼", "Animals"),
        ProfileAvatar("owl", "Owl", "🦉", "Animals"),
        ProfileAvatar("rabbit", "Rabbit", "🐰", "Animals"),
        ProfileAvatar("tiger", "Tiger", "🐯", "Animals"),
        ProfileAvatar("koala", "Koala", "🐨", "Animals"),
        ProfileAvatar("lion", "Lion", "🦁", "Animals"),
        ProfileAvatar("monkey", "Monkey", "🐵", "Animals"),
        ProfileAvatar("frog", "Frog", "🐸", "Animals"),
        ProfileAvatar("bear", "Bear", "🐻", "Animals"),

        // Cosmic / stars
        ProfileAvatar("star", "Star", "⭐", "Cosmic"),
        ProfileAvatar("shooting_star", "Shooting Star", "🌠", "Cosmic"),
        ProfileAvatar("moon", "Moon", "🌙", "Cosmic"),
        ProfileAvatar("sun", "Sun", "☀️", "Cosmic"),
        ProfileAvatar("rocket", "Rocket", "🚀", "Cosmic"),
        ProfileAvatar("planet", "Planet", "🪐", "Cosmic"),
        ProfileAvatar("alien", "Alien", "👽", "Cosmic"),
        ProfileAvatar("ufo", "UFO", "🛸", "Cosmic"),

        // Mystic / radiant
        ProfileAvatar("sparkles", "Sparkles", "✨", "Mystic"),
        ProfileAvatar("crystal", "Crystal", "🔮", "Mystic"),
        ProfileAvatar("fire", "Fire", "🔥", "Mystic"),
        ProfileAvatar("rainbow", "Rainbow", "🌈", "Mystic"),
        ProfileAvatar("crown", "Crown", "👑", "Mystic"),
        ProfileAvatar("lightning", "Lightning", "⚡", "Mystic"),
        ProfileAvatar("snowflake", "Snowflake", "❄️", "Mystic"),
        ProfileAvatar("four_leaf", "Lucky", "🍀", "Mystic"),

        // Spooky — playful, non-graphic choices only.
        ProfileAvatar("ghost", "Ghost", "👻", "Spooky"),
        ProfileAvatar("skull", "Skull", "💀", "Spooky"),
        ProfileAvatar("pumpkin", "Pumpkin", "🎃", "Spooky"),
        ProfileAvatar("vampire", "Vampire", "🧛", "Spooky"),
        ProfileAvatar("zombie", "Zombie", "🧟", "Spooky"),
        ProfileAvatar("bat", "Bat", "🦇", "Spooky"),
        ProfileAvatar("spider", "Spider", "🕷️", "Spooky"),
        ProfileAvatar("clown", "Clown", "🤡", "Spooky"),

        // Zodiac / horoscope
        ProfileAvatar("aries", "Aries", "♈", "Zodiac"),
        ProfileAvatar("taurus", "Taurus", "♉", "Zodiac"),
        ProfileAvatar("gemini", "Gemini", "♊", "Zodiac"),
        ProfileAvatar("cancer", "Cancer", "♋", "Zodiac"),
        ProfileAvatar("leo", "Leo", "♌", "Zodiac"),
        ProfileAvatar("virgo", "Virgo", "♍", "Zodiac"),
        ProfileAvatar("libra", "Libra", "♎", "Zodiac"),
        ProfileAvatar("scorpio", "Scorpio", "♏", "Zodiac"),
        ProfileAvatar("sagittarius", "Sagittarius", "♐", "Zodiac"),
        ProfileAvatar("capricorn", "Capricorn", "♑", "Zodiac"),
        ProfileAvatar("aquarius", "Aquarius", "♒", "Zodiac"),
        ProfileAvatar("pisces", "Pisces", "♓", "Zodiac"),


        // Faces / moods
        ProfileAvatar("cool", "Cool", "😎", "Faces"),
        ProfileAvatar("star_eyes", "Star Eyes", "🤩", "Faces"),
        ProfileAvatar("party", "Party", "🥳", "Faces"),
        ProfileAvatar("angel", "Angel", "😇", "Faces"),
        ProfileAvatar("devil", "Mischief", "😈", "Faces"),
        ProfileAvatar("cowboy", "Cowboy", "🤠", "Faces"),
        ProfileAvatar("disguise", "Disguise", "🥸", "Faces"),
        ProfileAvatar("frozen", "Frozen", "🥶", "Faces"),

        // Nature
        ProfileAvatar("flower", "Flower", "🌸", "Nature"),
        ProfileAvatar("sunflower", "Sunflower", "🌻", "Nature"),
        ProfileAvatar("cactus", "Cactus", "🌵", "Nature"),
        ProfileAvatar("tree", "Tree", "🌲", "Nature"),
        ProfileAvatar("maple", "Maple", "🍁", "Nature"),
        ProfileAvatar("wave", "Wave", "🌊", "Nature"),
        ProfileAvatar("volcano", "Volcano", "🌋", "Nature"),
        ProfileAvatar("cloud", "Cloud", "☁️", "Nature"),

        // Food / fun
        ProfileAvatar("pizza", "Pizza", "🍕", "Food"),
        ProfileAvatar("burger", "Burger", "🍔", "Food"),
        ProfileAvatar("sushi", "Sushi", "🍣", "Food"),
        ProfileAvatar("donut", "Donut", "🍩", "Food"),
        ProfileAvatar("boba", "Boba", "🧋", "Food"),
        ProfileAvatar("strawberry", "Strawberry", "🍓", "Food"),
        ProfileAvatar("avocado", "Avocado", "🥑", "Food"),
        ProfileAvatar("taco", "Taco", "🌮", "Food"),

        // Weird / fun
        ProfileAvatar("robot", "Robot", "🤖", "Weird"),
        ProfileAvatar("eye", "Eye", "👁️", "Weird"),
        ProfileAvatar("brain", "Brain", "🧠", "Weird"),
        ProfileAvatar("mushroom", "Mushroom", "🍄", "Weird"),
        ProfileAvatar("octopus", "Octopus", "🐙", "Weird"),
        ProfileAvatar("poop", "Silly", "💩", "Weird"),
        ProfileAvatar("ninja", "Ninja", "🥷", "Weird"),
        ProfileAvatar("mask", "Mask", "🎭", "Weird"),
    )

    // Kept for source compatibility with the first Phase 11F implementation.
    val animalAvatars: List<ProfileAvatar>
        get() = profileAvatars.filter { it.category == "Animals" }

    fun sanitizeDisplayName(raw: String): String {
        val clean = raw
            .replace(Regex("[\r\n\t]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(MAX_DISPLAY_NAME_LENGTH)
        return clean.ifBlank { "Radiant Rookie" }
    }

    fun normalizeAvatarId(raw: String?): String =
        raw?.trim()?.takeIf { candidate -> profileAvatars.any { it.id == candidate } }
            ?: DEFAULT_AVATAR_ID

    fun avatarFor(id: String?): ProfileAvatar {
        val normalized = normalizeAvatarId(id)
        return profileAvatars.first { it.id == normalized }
    }

    fun avatarsForCategory(category: String): List<ProfileAvatar> =
        profileAvatars.filter { it.category == category }
}
