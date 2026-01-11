package com.chatkeep.admin.core.common

/**
 * Data received from OAuth deeplink callback.
 *
 * Expected URL format:
 * chatkeep://auth/callback?id=123&first_name=John&last_name=Doe&username=john&photo_url=...&auth_date=1234567890&hash=abc123&state=xyz
 */
data class DeepLinkData(
    val id: Long,
    val firstName: String,
    val lastName: String?,
    val username: String?,
    val photoUrl: String?,
    val authDate: Long,
    val hash: String,
    val state: String
) {
    companion object {
        /**
         * Parses a deeplink URL and extracts authentication data.
         *
         * @param url The deeplink URL (e.g., "chatkeep://auth/callback?id=123&...")
         * @return DeepLinkData if URL is valid and contains required params, null otherwise
         */
        fun fromUrl(url: String): DeepLinkData? {
            try {
                // Extract query string
                val queryStart = url.indexOf('?')
                if (queryStart == -1) return null

                val queryString = url.substring(queryStart + 1)
                val params = parseQueryParams(queryString)

                // Extract required fields
                val id = params["id"]?.toLongOrNull() ?: return null
                val firstName = params["first_name"] ?: return null
                val authDate = params["auth_date"]?.toLongOrNull() ?: return null
                val hash = params["hash"] ?: return null
                val state = params["state"] ?: return null

                // Extract optional fields
                val lastName = params["last_name"]
                val username = params["username"]
                val photoUrl = params["photo_url"]

                return DeepLinkData(
                    id = id,
                    firstName = firstName,
                    lastName = lastName,
                    username = username,
                    photoUrl = photoUrl,
                    authDate = authDate,
                    hash = hash,
                    state = state
                )
            } catch (e: Exception) {
                return null
            }
        }

        private fun parseQueryParams(query: String): Map<String, String> {
            return query.split('&')
                .mapNotNull { param ->
                    val parts = param.split('=', limit = 2)
                    if (parts.size == 2) {
                        parts[0] to urlDecode(parts[1])
                    } else {
                        null
                    }
                }
                .toMap()
        }

        /**
         * Decodes URL-encoded strings.
         * Handles %XX escapes and + as space.
         */
        private fun urlDecode(value: String): String {
            return buildString {
                var i = 0
                while (i < value.length) {
                    when {
                        value[i] == '+' -> {
                            append(' ')
                            i++
                        }
                        value[i] == '%' && i + 2 < value.length -> {
                            try {
                                val hex = value.substring(i + 1, i + 3)
                                append(hex.toInt(16).toChar())
                                i += 3
                            } catch (e: NumberFormatException) {
                                append(value[i])
                                i++
                            }
                        }
                        else -> {
                            append(value[i])
                            i++
                        }
                    }
                }
            }
        }
    }
}
