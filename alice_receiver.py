import asyncio
import websockets
import json
import hashlib
import sys

sys.path.append('C:/Users/ttt79/.gemini/antigravity/scratch/nostr-expressive-messenger')
from bob_listener import derive_conversation_key, decrypt_inner, BOB_PK

ALICE_PK = '08fe12673cee0ae8bdfc3423502b14b0ab1ba58bc374b86da6bf105fa39090e7'

async def receive_reply():
    print(f"Alice checking inbox on wss://nos.lol for {ALICE_PK}...")
    async with websockets.connect('wss://nos.lol') as ws:
        req = ['REQ', 'sub_alice_check', {'kinds': [1059], '#p': [ALICE_PK]}]
        await ws.send(json.dumps(req))
        while True:
            msg = await ws.recv()
            data = json.loads(msg)
            if data[0] == 'EVENT':
                event = data[2]
                content = event.get('content', '')
                try:
                    envelope = json.loads(content)
                except Exception:
                    continue
                sender = envelope.get('sender_pubkey')
                if sender == BOB_PK:
                    chat_key = derive_conversation_key(ALICE_PK, BOB_PK)
                    text = decrypt_inner(envelope['ciphertext'], chat_key, envelope['iv'])
                    print("==================================================")
                    print(f"[ALICE RECEIVED REPLY FROM BOB]: '{text}'")
                    print("==================================================")
                    return
            elif data[0] == 'EOSE':
                print("[Alice] Reached EOSE, waiting for new events...")

if __name__ == '__main__':
    asyncio.run(receive_reply())
