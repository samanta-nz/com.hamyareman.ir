package com.hamyareman.ir.platform.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyPolicyTest {

    @Test
    fun `private tables never overlap with shareable ones`() {
        val overlap = PrivacyPolicy.neverSyncTables intersect PrivacyPolicy.weeklyShareableTables
        assertTrue("هیچ جدولی نباید هم خصوصی و هم قابل اشتراک باشد: $overlap", overlap.isEmpty())
    }

    @Test
    fun `all private tables are flagged`() {
        listOf(
            TableIds.CYCLE_ENTRIES,
            TableIds.MOOD_ENTRIES,
            TableIds.JOURNAL_ENTRIES,
            TableIds.SCREEN_TIME_LOGS,
            TableIds.CHAT_HISTORY,
        ).forEach { assertTrue(PrivacyPolicy.isNeverSynced(it)) }
    }

    @Test
    fun `shareable tables are not private`() {
        listOf(
            TableIds.ROUTINE_BLOCKS,
            TableIds.WATER_LOGS,
            TableIds.EXERCISE_LOGS,
            TableIds.BADGES,
            // آلبوم مشترک عمداً قابل اشتراک است (با تأیید زهرا)، پس خصوصی نیست.
            TableIds.ALBUM_ITEMS,
        ).forEach { assertFalse(PrivacyPolicy.isNeverSynced(it)) }
    }

    @Test
    fun `server tables never include a private table`() {
        // جدول‌های خصوصی هیچ‌وقت در appwrite.json ساخته نمی‌شوند.
        assertEquals(5, PrivacyPolicy.neverSyncTables.size)
        // v1.77: جدولِ `subscription_orders` (صف پرداخت/استرداد) ⇒ ۲۸ جدول.
        assertEquals(28, TableIds.serverTables.size)
        assertTrue(TableIds.SUBSCRIPTION_ORDERS in TableIds.serverTables)
        assertTrue(TableIds.serverTables.none { PrivacyPolicy.isNeverSynced(it) })
        assertEquals(PrivacyPolicy.neverSyncTables, TableIds.deviceOnlyTables)
    }

    @Test
    fun `username mapping table is a server table and never a private one`() {
        // لاگین با نام کاربری و بازیابیِ رمز به این جدول نیاز دارد؛ پس باید سمتِ سرور
        // ساخته شود — ولی هرگز جزوِ جدول‌های «هرگز سینک نمی‌شود» نیست.
        assertTrue(TableIds.USERS in TableIds.serverTables)
        assertFalse(PrivacyPolicy.isNeverSynced(TableIds.USERS))
        assertFalse(TableIds.USERS in PrivacyPolicy.weeklyShareableTables)
    }

    @Test
    fun `album items live on the server and never in the private set`() {
        assertTrue(TableIds.ALBUM_ITEMS in TableIds.serverTables)
        assertFalse(TableIds.ALBUM_ITEMS in TableIds.deviceOnlyTables)
    }
}
