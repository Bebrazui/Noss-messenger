package com.nostr.expressive.shared.network

import com.nostr.expressive.shared.model.NostrEvent
import com.nostr.expressive.shared.model.NostrFilter
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

class NostrRelayPool(
    private val relayUrls: List<String> = listOf(
        "wss://nos.lol",
        "wss://relay.damus.io",
        "wss://relay.primal.net",
        "wss://search.nos.today",
        "wss://relay.snort.social"
    )
) {
    private val client = HttpClient {
        install(WebSockets) {
            pingIntervalMillis = 30_000
        }
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private val _eventsFlow = MutableSharedFlow<NostrEvent>(extraBufferCapacity = 128)
    val eventsFlow = _eventsFlow.asSharedFlow()

    private val _relayAcks = MutableSharedFlow<Pair<String, Boolean>>(extraBufferCapacity = 128)
    val relayAcks = _relayAcks.asSharedFlow()

    private val activeSessions = mutableListOf<DefaultClientWebSocketSession>()
    private val activeSubscriptions = mutableMapOf<String, String>() // subId -> subscription payload

    fun start() {
        relayUrls.forEach { url ->
            scope.launch {
                connectWithRetry(url)
            }
        }
    }

    private suspend fun connectWithRetry(url: String) {
        var backoffMs = 2000L
        while (scope.isActive) {
            try {
                client.webSocket(urlString = url) {
                    println("[NostrRelayPool] Connected to $url")
                    activeSessions.add(this)

                    // Re-send all active subscriptions to the newly connected relay
                    activeSubscriptions.values.toList().forEach { subPayload ->
                        launch {
                            try {
                                send(Frame.Text(subPayload))
                            } catch (_: Exception) {}
                        }
                    }

                    backoffMs = 2000L // reset backoff on successful connection

                    try {
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                handleMessage(frame.readText())
                            }
                        }
                    } finally {
                        activeSessions.remove(this)
                        println("[NostrRelayPool] Disconnected from $url")
                    }
                }
            } catch (e: Exception) {
                val errMsg = e.message ?: e.toString()
                if (errMsg.contains("SecurityError", ignoreCase = true) || errMsg.contains("insecure", ignoreCase = true)) {
                    println("[NostrRelayPool] Skipping insecure relay $url on secure HTTPS origin.")
                    return
                }
                println("[NostrRelayPool] Error on $url: ${e.message}. Retrying in ${backoffMs / 1000}s...")
                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(30_000L)
            }
        }
    }

    private suspend fun handleMessage(rawText: String) {
        try {
            val element = json.parseToJsonElement(rawText) as? JsonArray ?: return
            val messageType = element.getOrNull(0)?.jsonPrimitive?.content ?: return

            when (messageType) {
                "EVENT" -> {
                    // ["EVENT", <subscription_id>, <event JSON>]
                    val eventObj = element.getOrNull(2) ?: return
                    val event = json.decodeFromJsonElement<NostrEvent>(eventObj)
                    _eventsFlow.emit(event)
                }
                "OK" -> {
                    // ["OK", <event_id>, <true|false>, <message>]
                    val eventId = element.getOrNull(1)?.jsonPrimitive?.content ?: ""
                    val success = element.getOrNull(2)?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
                    _relayAcks.emit(eventId to success)
                }
            }
        } catch (_: Exception) {}
    }

    fun broadcastEvent(event: NostrEvent) {
        scope.launch {
            val eventPayload = json.encodeToString(listOf(
                JsonPrimitive("EVENT"),
                json.encodeToJsonElement(NostrEvent.serializer(), event)
            ))

            val sessionsCopy = activeSessions.toList()
            if (sessionsCopy.isEmpty()) {
                // If offline / connecting, still emit local ack for optimistic UI
                _relayAcks.emit(event.id to true)
            } else {
                sessionsCopy.forEach { session: DefaultClientWebSocketSession ->
                    try {
                        session.send(Frame.Text(eventPayload))
                    } catch (e: Exception) {
                        println("[NostrRelayPool] Failed to send to a session: ${e.message}")
                    }
                }
            }
        }
    }

    fun subscribe(
        subId: String,
        kinds: List<Int>,
        pTags: List<String> = emptyList(),
        eTags: List<String> = emptyList(),
        authors: List<String> = emptyList(),
        search: String? = null,
        limit: Int? = null
    ) {
        scope.launch {
            val filterMap = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()
            filterMap["kinds"] = JsonArray(kinds.map { JsonPrimitive(it) })

            if (authors.isNotEmpty()) {
                filterMap["authors"] = JsonArray(authors.map { JsonPrimitive(it.lowercase()) })
            }
            if (pTags.isNotEmpty()) {
                filterMap["#p"] = JsonArray(pTags.map { JsonPrimitive(it.lowercase()) })
            }
            if (eTags.isNotEmpty()) {
                filterMap["#e"] = JsonArray(eTags.map { JsonPrimitive(it.lowercase()) })
            }
            if (!search.isNullOrBlank()) {
                filterMap["search"] = JsonPrimitive(search)
            }
            if (limit != null && limit > 0) {
                filterMap["limit"] = JsonPrimitive(limit)
            }

            val rawFilter = json.encodeToString(listOf(
                JsonPrimitive("REQ"),
                JsonPrimitive(subId),
                JsonObject(filterMap)
            ))

            activeSubscriptions[subId] = rawFilter

            val sessionsCopy = activeSessions.toList()
            sessionsCopy.forEach { session: DefaultClientWebSocketSession ->
                try {
                    session.send(Frame.Text(rawFilter))
                } catch (_: Exception) {}
            }
        }
    }

    fun unsubscribe(subId: String) {
        scope.launch {
            activeSubscriptions.remove(subId)
            val closePayload = json.encodeToString(listOf(
                JsonPrimitive("CLOSE"),
                JsonPrimitive(subId)
            ))
            val sessionsCopy = activeSessions.toList()
            sessionsCopy.forEach { session: DefaultClientWebSocketSession ->
                try {
                    session.send(Frame.Text(closePayload))
                } catch (_: Exception) {}
            }
        }
    }

    fun close() {
        val sessionsCopy = activeSessions.toList()
        scope.launch {
            sessionsCopy.forEach { session: DefaultClientWebSocketSession ->
                try {
                    session.close()
                } catch (_: Exception) {}
            }
            client.close()
        }
    }
}
