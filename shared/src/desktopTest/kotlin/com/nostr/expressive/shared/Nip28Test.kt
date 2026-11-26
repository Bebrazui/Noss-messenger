package com.nostr.expressive.shared

import com.nostr.expressive.shared.model.NostrKinds
import com.nostr.expressive.shared.protocol.Nip28
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Nip28Test {

    @Test
    fun testCreateChannelEventWithAppAntiSpamTag() {
        val pubkey = "npub1test_creator_pubkey_0000000000000000000000000000000000000"
        val channelName = "Kotlin & Nostr"
        val channelAbout = "Public channel about Kotlin Multiplatform and Nostr"

        val event = Nip28.buildCreateChannelEvent(channelName, channelAbout, null, pubkey)

        assertEquals(NostrKinds.CHANNEL_CREATE, event.kind)
        assertEquals(pubkey, event.pubkey)
        assertTrue(event.id.isNotEmpty())

        val meta = Nip28.parseMetadata(event.content)
        assertEquals(channelName, meta.name)
        assertEquals(channelAbout, meta.about)

        // Verify anti-spam client tag
        assertTrue(Nip28.isAppChannel(event))
    }

    @Test
    fun testChannelMessageEvent() {
        val pubkey = "npub1test_sender_pubkey_00000000000000000000000000000000000000"
        val channelId = "chan_id_12345678"
        val text = "Hello everyone in the public Nostr channel!"

        val msgEvent = Nip28.buildChannelMessageEvent(channelId, text, pubkey)

        assertEquals(NostrKinds.CHANNEL_MESSAGE, msgEvent.kind)
        assertEquals(text, msgEvent.content)
        assertEquals(pubkey, msgEvent.pubkey)

        val eTag = msgEvent.tags.find { it.getOrNull(0) == "e" }
        checkNotNull(eTag)
        assertEquals(channelId, eTag[1])
        assertEquals("root", eTag[3])
    }
}
