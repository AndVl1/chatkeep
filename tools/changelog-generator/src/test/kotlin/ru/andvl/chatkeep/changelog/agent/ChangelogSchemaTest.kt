package ru.andvl.chatkeep.changelog.agent

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChangelogSchemaTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parse valid changelog response`() {
        val jsonString = """
            {
                "production": [
                    {"title": "New feature", "details": "Added search"},
                    {"title": "Bug fix"}
                ],
                "internal": [
                    {"title": "Refactoring", "details": "Clean code"}
                ],
                "summary": "Added search and fixed bugs"
            }
        """.trimIndent()

        val result = json.decodeFromString<ChangelogResponse>(jsonString)

        assertEquals(2, result.production.size)
        assertEquals("New feature", result.production[0].title)
        assertEquals("Added search", result.production[0].details)
        assertEquals("Bug fix", result.production[1].title)
        assertNull(result.production[1].details)

        assertEquals(1, result.internal.size)
        assertEquals("Refactoring", result.internal[0].title)
        assertEquals("Clean code", result.internal[0].details)

        assertEquals("Added search and fixed bugs", result.summary)
    }

    @Test
    fun `parse changelog with missing optional fields`() {
        val jsonString = """
            {
                "production": [
                    {"title": "Feature without details"}
                ],
                "internal": [
                    {"title": "Internal change"}
                ],
                "summary": "Test summary"
            }
        """.trimIndent()

        val result = json.decodeFromString<ChangelogResponse>(jsonString)

        assertEquals(1, result.production.size)
        assertEquals("Feature without details", result.production[0].title)
        assertNull(result.production[0].details)

        assertEquals(1, result.internal.size)
        assertNull(result.internal[0].details)
    }

    @Test
    fun `parse changelog with empty arrays`() {
        val jsonString = """
            {
                "production": [],
                "internal": [],
                "summary": "No changes"
            }
        """.trimIndent()

        val result = json.decodeFromString<ChangelogResponse>(jsonString)

        assertEquals(0, result.production.size)
        assertEquals(0, result.internal.size)
        assertEquals("No changes", result.summary)
    }

    @Test
    fun `parse changelog with extra fields (ignoreUnknownKeys)`() {
        val jsonString = """
            {
                "production": [
                    {"title": "Feature", "details": "Details", "extra": "field"}
                ],
                "internal": [],
                "summary": "Summary",
                "extraTopLevel": "ignored"
            }
        """.trimIndent()

        val result = json.decodeFromString<ChangelogResponse>(jsonString)

        assertEquals(1, result.production.size)
        assertEquals("Feature", result.production[0].title)
        assertEquals("Details", result.production[0].details)
    }

    @Test
    fun `serialize and deserialize changelog`() {
        val original = ChangelogResponse(
            production = listOf(
                ChangelogEntry("Feature 1", "Details 1"),
                ChangelogEntry("Feature 2", null)
            ),
            internal = listOf(
                ChangelogEntry("Internal 1")
            ),
            summary = "Test summary"
        )

        val jsonString = json.encodeToString(ChangelogResponse.serializer(), original)
        val deserialized = json.decodeFromString<ChangelogResponse>(jsonString)

        assertEquals(original.production.size, deserialized.production.size)
        assertEquals(original.production[0].title, deserialized.production[0].title)
        assertEquals(original.production[0].details, deserialized.production[0].details)
        assertEquals(original.internal.size, deserialized.internal.size)
        assertEquals(original.summary, deserialized.summary)
    }
}
