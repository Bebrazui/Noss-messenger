import asyncio
import json
import logging
import websockets

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("NostrRelay")

events_store = []
subscriptions = {}

def matches_filter(event, f):
    if not isinstance(f, dict):
        return True
    if "kinds" in f and event.get("kind") not in f["kinds"]:
        return False
    if "authors" in f and event.get("pubkey") not in f["authors"]:
        return False
    if "#p" in f:
        event_p_tags = [t[1] for t in event.get("tags", []) if len(t) >= 2 and t[0] == "p"]
        if not any(p in f["#p"] for p in event_p_tags):
            return False
    if "#e" in f:
        event_e_tags = [t[1] for t in event.get("tags", []) if len(t) >= 2 and t[0] == "e"]
        if not any(e in f["#e"] for e in event_e_tags):
            return False
    return True

async def handle_connection(websocket):
    logger.info(f"Client connected: {websocket.remote_address}")
    subscriptions[websocket] = {}
    try:
        async for message in websocket:
            try:
                data = json.loads(message)
                if not isinstance(data, list) or len(data) == 0:
                    continue
                cmd = data[0]

                if cmd == "EVENT":
                    event = data[1]
                    event_id = event.get("id", "")
                    events_store.append(event)
                    logger.info(f"Broadcast EVENT kind={event.get('kind')} id={event_id[:8]} from={event.get('pubkey', '')[:8]}")
                    # Send OK ack
                    await websocket.send(json.dumps(["OK", event_id, True, ""]))

                    # Broadcast to matching subscriptions across all clients
                    for ws, client_subs in list(subscriptions.items()):
                        for sub_id, filters in list(client_subs.items()):
                            for f in filters:
                                if matches_filter(event, f):
                                    try:
                                        await ws.send(json.dumps(["EVENT", sub_id, event]))
                                    except Exception:
                                        pass
                                    break

                elif cmd == "REQ":
                    sub_id = data[1]
                    filters = data[2:]
                    subscriptions[websocket][sub_id] = filters
                    logger.info(f"Sub REQ sub_id={sub_id} filters={filters}")

                    # Replay matching historical events
                    for event in list(events_store):
                        for f in filters:
                            if matches_filter(event, f):
                                try:
                                    await websocket.send(json.dumps(["EVENT", sub_id, event]))
                                except Exception:
                                    pass
                                break
                    try:
                        await websocket.send(json.dumps(["EOSE", sub_id]))
                    except Exception:
                        pass

                elif cmd == "CLOSE":
                    sub_id = data[1]
                    subscriptions[websocket].pop(sub_id, None)

            except Exception as e:
                logger.error(f"Error handling message: {e}")

    except websockets.exceptions.ConnectionClosed:
        pass
    finally:
        subscriptions.pop(websocket, None)
        logger.info(f"Client disconnected: {websocket.remote_address}")

async def main():
    port = 7777
    logger.info(f"Starting Noss Local Nostr Relay on 0.0.0.0:{port}...")
    async with websockets.serve(handle_connection, "0.0.0.0", port):
        await asyncio.Future()

if __name__ == "__main__":
    asyncio.run(main())
