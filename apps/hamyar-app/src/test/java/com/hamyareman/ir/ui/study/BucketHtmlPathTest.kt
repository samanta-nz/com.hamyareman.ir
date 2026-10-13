package com.hamyareman.ir.ui.study

import org.junit.Assert.*
import org.junit.Test

class BucketHtmlPathTest {
    @Test fun namesQueriesAndRelativeLinksUseTheSameDocumentContract() {
        assertFalse(BucketHtmlPath.encrypted("https://bucket/lesson.html?x=1#part"))
        assertTrue(BucketHtmlPath.isHtml("lesson.html.hmk1?x=1#part"))
        assertTrue(BucketHtmlPath.encrypted("LESSON.HTML.HMK1"))
        assertEquals("https://bucket/lesson.html.hmk1?x=1#part", BucketHtmlPath.alternative("https://bucket/lesson.html?x=1#part"))
        assertEquals("https://bucket/lesson.html?x=1#part", BucketHtmlPath.alternative("https://bucket/lesson.html.hmk1?x=1#part"))
        assertNull(BucketHtmlPath.alternative("photo.jpg"))
        assertEquals("folder/lesson.html", BucketHtmlPath.plainPath("folder/lesson.html.hmk1"))
    }
}
