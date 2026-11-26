import asyncio
import websockets
import json
import os
import sys

sys.path.append('C:/Users/ttt79/.gemini/antigravity/scratch/nostr-expressive-messenger')
from bob_listener import derive_conversation_key, encrypt_inner, create_gift_wrap, BOB_PK

ALICE_PK = '6b541ef7796627a06c7f43c1e48ab24495a9acf29eb7de225bf6f48c6e11f51a'

async def send_to_web_alice():
    chat_key = derive_conversation_key(BOB_PK, ALICE_PK)
    text = "Привет Алиса! Это живое сообщение от Боба через nos.lol! Доставка работает!"
    inner = encrypt_inner(text, chat_key, BOB_PK)
    wrap = create_gift_wrap(inner, ALICE_PK)
    event_id = wrap['id']
    print(f"Sending Kind 1059 to Alice web ({ALICE_PK}) on wss://nos.lol... event_id={event_id}")
    async with websockets.connect('wss://nos.lol') as ws:
        await ws.send(json.dumps(['EVENT', wrap]))
        resp = await ws.recv()
        print(f"Relay response: {resp}")

if __name__ == '__main__':
    asyncio.run(send_to_web_alice())
