package com.hamyareman.ir.ui

import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Guards the object initialization order in AppTypography.
 *
 * The legacy alias MutableState delegates must exist before syncAliases() runs.
 * A regression here previously caused ExceptionInInitializerError during app startup.
 */
class AppTypographyInitializationTest {
    // Keep this test in CI so static object initialization is exercised on every release branch update.

    @Test
    fun `legacy aliases are initialized before first sync`() {
        assertNotNull(AppTypography.greeting)
        assertNotNull(AppTypography.clock)
        assertNotNull(AppTypography.heading)
        assertNotNull(AppTypography.tile)
        assertNotNull(AppTypography.body)
        assertNotNull(AppTypography.material())
    }
}
