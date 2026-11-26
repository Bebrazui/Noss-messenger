package com.nostr.expressive.shared

import com.nostr.expressive.shared.crypto.PlatformCrypto
import com.nostr.expressive.shared.database.LocalDatabase
import com.nostr.expressive.shared.repository.MessengerRepository
import com.nostr.expressive.shared.repository.UserProfile
import com.nostr.expressive.shared.repository.UsernameCheckResult
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileSetupTest {

    @Test
    fun testUsernameValidation() = runBlocking {
        val dummyKey = PlatformCrypto.randomBytes(32)
        val dummyDbKey = PlatformCrypto.randomBytes(32)
        val myPubkey = "npub1testprofile00000000000000000000000000000000000000000000000000"

        val repo = MessengerRepository(
            myPubkey = myPubkey,
            sessionKey = dummyKey,
            dbKey = dummyDbKey
        )

        // 1. Too short
        val resShort = repo.checkUsernameAvailability("ab")
        assertTrue(resShort is UsernameCheckResult.Invalid)

        // 2. Too long
        val resLong = repo.checkUsernameAvailability("a_very_long_username_exceeding_twenty_chars")
        assertTrue(resLong is UsernameCheckResult.Invalid)

        // 3. Invalid characters (spaces, special symbols)
        val resInvalidChars = repo.checkUsernameAvailability("satoshi nakamoto!")
        assertTrue(resInvalidChars is UsernameCheckResult.Invalid)

        // 4. Valid username syntax
        val resValid = repo.checkUsernameAvailability("@valid_user.123")
        assertTrue(resValid is UsernameCheckResult.Available || resValid is UsernameCheckResult.Taken)
    }

    @Test
    fun testProfileConfigurationPersistence() {
        val dummyDbKey = PlatformCrypto.randomBytes(32)
        val db = LocalDatabase(dummyDbKey)

        assertFalse(db.isProfileConfigured())

        val testProfile = UserProfile(
            pubkey = "npub1test123",
            name = "alice",
            displayName = "Alice In Chains",
            about = "Decentralized audio",
            picture = "https://example.com/alice.png",
            banner = "https://example.com/banner.png"
        )

        db.saveUserProfile(testProfile, isConfigured = true)

        assertTrue(db.isProfileConfigured())
        val loaded = db.getUserProfile()
        assertEquals("alice", loaded?.name)
        assertEquals("Alice In Chains", loaded?.displayName)
        assertEquals("https://example.com/alice.png", loaded?.picture)
        assertEquals("https://example.com/banner.png", loaded?.banner)
    }
}
