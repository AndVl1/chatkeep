package ru.andvl.chatkeep.domain.model.gated

enum class GatedFeatureKey(
    val key: String,
    val displayName: String,
    val description: String
) {
    TWITCH_INTEGRATION(
        key = "twitch_integration",
        displayName = "Twitch уведомления",
        description = "Автоматические уведомления о начале стримов в вашем чате"
    );

    companion object {
        fun fromKey(key: String): GatedFeatureKey? {
            return entries.firstOrNull { it.key == key }
        }

        fun allFeatures(): List<GatedFeatureKey> = entries
    }
}
