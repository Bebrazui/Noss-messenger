import asyncio
import websockets
import json
import os
import sys

sys.path.append('C:/Users/ttt79/.gemini/antigravity/scratch/nostr-expressive-messenger')
import bip340_ref as bip
from bob_listener import derive_conversation_key, encrypt_inner, create_gift_wrap, BOB_PK

ALICE_SK = os.urandom(32)
ALICE_PK = bip.pubkey_gen(ALICE_SK).hex()

async def send():
    chat_key = derive_conversation_key(ALICE_PK, BOB_PK)
    inner = encrypt_inner("Привет Боб, это проверка реальной доставки через nos.lol!", chat_key, ALICE_PK)
    wrap = create_gift_wrap(inner, BOB_PK)
    print(f"Alice pubkey: {ALICE_PK}")
    print(f"Sending Kind 1059 (id={wrap['id']}) to Bob on wss://nos.lol...")
    async with websockets.connect('wss://nos.lol') as ws:
        await ws.send(json.dumps(['EVENT', wrap]))
        resp = await ws.recv()
        print(f"Relay response: {resp}")

if __name__ == '__main__':
    asyncio.run(send())
