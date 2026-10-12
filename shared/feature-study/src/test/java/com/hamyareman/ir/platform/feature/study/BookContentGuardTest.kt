package com.hamyareman.ir.platform.feature.study

import org.junit.Assert.*
import org.junit.Test

class BookContentGuardTest {
    private fun ids(nodes: List<BookToc.TocNode>): List<String> = nodes.flatMap { listOfNotNull(it.packId) + ids(it.children) }
    @Test fun navigationMetadataCoversEveryBookWithoutBundledPayloads() {
        val modules = BookModuleRegistry.modules
        assertEquals(14, modules.size)
        assertEquals(modules.size, modules.map { it.bookCode }.toSet().size)
        for (module in modules) {
            assertEquals(ids(BookToc.forBook(module.bookCode)).toSet(), module.packs.map { it.packId }.toSet())
            assertTrue(module.title.isNotBlank())
            for (pack in module.packs) {
                assertTrue(pack.title.isNotBlank())
                assertEquals(module.bookCode, pack.bookCode)
                assertTrue(pack.sections.isEmpty()); assertTrue(pack.questions.isEmpty())
                assertTrue(pack.flashcards.isEmpty()); assertTrue(pack.solutions.isEmpty())
                assertTrue(pack.teachHtml.isBlank()); assertTrue(pack.teachText.isBlank())
                assertTrue(pack.summary.isBlank()); assertTrue(pack.examTips.isBlank())
                assertTrue(pack.exercises.isEmpty())
            }
        }
    }
}
