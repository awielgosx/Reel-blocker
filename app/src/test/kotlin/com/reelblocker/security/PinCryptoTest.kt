package com.reelblocker.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

class PinCryptoTest {

    @Test
    fun `hashing the same pin and salt twice gives the same result`() {
        val salt = PinCrypto.randomSalt()
        assertEquals(PinCrypto.hash("1234", salt), PinCrypto.hash("1234", salt))
    }

    @Test
    fun `different pins hash differently with the same salt`() {
        val salt = PinCrypto.randomSalt()
        assertNotEquals(PinCrypto.hash("1234", salt), PinCrypto.hash("4321", salt))
    }

    @Test
    fun `the same pin hashes differently with different salts`() {
        val hashA = PinCrypto.hash("1234", PinCrypto.randomSalt())
        val hashB = PinCrypto.hash("1234", PinCrypto.randomSalt())
        assertNotEquals(hashA, hashB)
    }

    @Test
    fun `random salts are not reused across calls`() {
        val salts = (1..50).map { PinCrypto.randomSalt() }
        assertEquals(salts.size, salts.toSet().size)
    }

    @Test
    fun `recovery codes use only the unambiguous alphabet and the expected shape`() {
        repeat(100) {
            val code = PinCrypto.generateRecoveryCode()
            assertTrue("'$code' doesn't match XXXXX-XXXXX", Regex("^[A-Z2-9]{5}-[A-Z2-9]{5}$").matches(code))
            assertFalse("recovery code should never contain ambiguous characters", code.any { it in "0O1I" })
        }
    }

    @Test
    fun `recovery codes are not reused across calls`() {
        val codes = (1..50).map { PinCrypto.generateRecoveryCode() }
        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun `recovery code matching is case-insensitive and trims whitespace`() {
        val code = PinCrypto.generateRecoveryCode()
        assertTrue(PinCrypto.matchesRecoveryCode(code, code.lowercase()))
        assertTrue(PinCrypto.matchesRecoveryCode(code, "  $code  "))
    }

    @Test
    fun `recovery code matching rejects a wrong code`() {
        val code = PinCrypto.generateRecoveryCode(SecureRandom())
        assertFalse(PinCrypto.matchesRecoveryCode(code, "WRONG-CODE"))
    }
}
