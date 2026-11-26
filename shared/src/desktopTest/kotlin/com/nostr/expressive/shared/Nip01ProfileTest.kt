package com.nostr.expressive.shared

import com.nostr.expressive.shared.model.NostrKinds
import com.nostr.expressive.shared.protocol.Nip01Profile
import com.nostr.expressive.shared.protocol.ProfileMetadata
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Nip01ProfileTest {

    @Test
    fun testBuildAndParseProfileMetadata() {
        val pubkey = "npub1satoshi00000000000000000000000000000000000000000000000000"
        val metadata = ProfileMetadata(
            name = "satoshi",
            display_name = "Satoshi Nakamoto",
            about = "Building zero-server decentralized tools",
            picture = "https://example.com/avatar.webp",
            nip05 = "satoshi@example.com"
        )

        val event = Nip01Profile.buildProfileEvent(metadata, pubkey)

        assertEquals(NostrKinds.METADATA, event.kind)
        assertEquals(pubkey, event.pubkey)
        assertTrue(event.id.isNotEmpty())

        val parsed = Nip01Profile.parseMetadata(event.content)
        assertEquals("satoshi", parsed.name)
        assertEquals("Satoshi Nakamoto", parsed.display_name)
        assertEquals("satoshi@example.com", parsed.nip05)
    }
}
