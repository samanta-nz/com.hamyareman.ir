package com.hamyareman.ir.platform.feature.pairing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PairingCodeTest {

    @Test
    fun `accepts six latin digits`() {
        assertEquals("123456", AppwritePairingRepository.normalizeCode("123456"))
    }

    @Test
    fun `normalises persian and arabic digits`() {
        assertEquals("123456", AppwritePairingRepository.normalizeCode("۱۲۳۴۵۶"))
        assertEquals("123456", AppwritePairingRepository.normalizeCode("١٢٣٤٥٦"))
    }

    @Test
    fun `ignores spaces and dashes`() {
        assertEquals("123456", AppwritePairingRepository.normalizeCode(" 123-456 "))
    }

    @Test
    fun `rejects wrong length`() {
        assertNull(AppwritePairingRepository.normalizeCode("12345"))
        assertNull(AppwritePairingRepository.normalizeCode("1234567"))
        assertNull(AppwritePairingRepository.normalizeCode(""))
        assertNull(AppwritePairingRepository.normalizeCode("abcdef"))
    }

    @Test
    fun `pairing state is linked only when active and has partner`() {
        val linked = PairingState(partnerId = "u1", status = PairingState.STATUS_ACTIVE)
        assertEquals(true, linked.isLinked)
        assertEquals(false, PairingState().isLinked)
        assertEquals(false, PairingState(partnerId = "u1", status = PairingState.STATUS_REVOKED).isLinked)
    }
}
