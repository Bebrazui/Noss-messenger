package com.nostr.expressive.shared

import com.nostr.expressive.shared.crypto.KeyManager
import com.nostr.expressive.shared.crypto.Nip19
import com.nostr.expressive.shared.crypto.PlatformCrypto
import com.nostr.expressive.shared.crypto.toHex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class Bech32Test {

    @Test
    fun testNpubAndNsecRoundTrip() {
        val privBytes = PlatformCrypto.randomBytes(32)
        val privHex = privBytes.toHex()
        val nsec = Nip19.toNsec(privHex)

        assertTrue(nsec.startsWith("nsec1"))
        val decodedHex = Nip19.parseNsec(nsec)
        assertEquals(privHex, decodedHex)

        val pubBytes = PlatformCrypto.sha256(privBytes)
        val pubHex = pubBytes.toHex()
        val npub = Nip19.toNpub(pubHex)

        assertTrue(npub.startsWith("npub1"))
        val decodedPubHex = Nip19.parseNpub(npub)
        assertEquals(pubHex, decodedPubHex)
    }

    @Test
    fun testInvalidBech32ChecksumRejection() {
        val validNsec = Nip19.toNsec("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff")
        // Tamper with the last character
        val tampered = validNsec.dropLast(1) + if (validNsec.last() == 'q') 'p' else 'q'

        assertFails {
            Nip19.parseNsec(tampered)
        }
    }

    @Test
    fun testKeyManagerValidation() {
        val identity = KeyManager.generateNewIdentity()
        assertTrue(identity.nsec.startsWith("nsec1"))
        assertTrue(identity.npub.startsWith("npub1"))
        assertEquals(64, identity.privkeyHex.length)
        assertEquals(64, identity.pubkeyHex.length)

        // Invalid key import should fail
        assertFails {
            KeyManager.importIdentity("invalid_short_key")
        }
    }
}
