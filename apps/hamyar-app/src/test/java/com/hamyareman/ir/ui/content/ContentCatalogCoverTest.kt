package com.hamyareman.ir.ui.content

import org.junit.Assert.assertEquals
import org.junit.Test

class ContentCatalogCoverTest {
    private fun item(id: String, cat: String = "amozesh") = ContentItem(
        id = id,
        cat = cat,
        kind = "html",
        title = id,
        gender = "all",
        aw = "file",
        key = "content/$id.html",
    )

    @Test
    fun `all 34 academy lessons map to the semantically matching approved cover`() {
        val expected = listOf(
            "sk-speed", "sk-hand", "sk-type", "sk-cornell", "sk-summary",
            "sk-debate", "sk-fallacy", "sk-present", "sk-voice", "sk-email", "sk-listen",
            "sk-math", "sk-palace", "sk-lateral", "sk-chain", "sk-feel", "hyp-reframe",
            "sk-resilience", "sk-plan", "sk-habit", "sk-desk", "sk-digital", "sk-ai-what",
            "sk-search", "sk-privacy", "sk-fake", "sk-team", "sk-conflict", "sk-no",
            "sk-budget", "sk-need", "sk-storm", "sk-create", "sk-music",
        )
        val actual = (1..34).map { number ->
            ContentCatalog.coverId(item("amz-${number.toString().padStart(2, '0')}"))
        }
        assertEquals(expected, actual)
    }

    @Test
    fun `yoga and exercise menu covers are portrait two by three`() {
        assertEquals(2f / 3f, ContentCatalog.coverAspectRatio(item("yga-01", "yoga")))
        assertEquals(2f / 3f, ContentCatalog.coverAspectRatio(item("spo-01", "sport")))
        assertEquals(1f, ContentCatalog.coverAspectRatio(item("amz-01")))
    }
}
