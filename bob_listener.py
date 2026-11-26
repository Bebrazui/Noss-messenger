import asyncio
import json
import hashlib
import sys
import websockets
sys.path.append('C:/Users/ttt79/.gemini/antigravity/scratch/nostr-expressive-messenger')
import bip340_ref as bip

BOB_SK = bytes.fromhex('0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef')
BOB_PK = '4646ae5047316b4230d0086c8acec687f00b1cd9d1dc634f6cb358ac0a9a8fff'

def derive_conversation_key(pk_a: str, pk_b: str) -> bytes:
    sorted_pks = sorted([pk_a.lower(), pk_b.lower()])
    seed = (sorted_pks[0] + ":" + sorted_pks[1] + ":NOSS_CONVERSATION_KEY_V1").encode()
    return hashlib.sha256(seed).digest()

def decrypt_inner(ciphertext_hex: str, key: bytes, iv_hex: str) -> str:
    ciphertext = bytes.fromhex(ciphertext_hex)
    iv = bytes.fromhex(iv_hex)
    # Stream cipher match from PlatformCrypto
    output = bytearray(len(ciphertext))
    counter = 0
    keystream_index = 0
    current_block = hashlib.sha256(key + iv).digest()
    for i in range(len(ciphertext)):
        if keystream_index >= len(current_block):
            counter += 1
            current_block = hashlib.sha256(key + iv + str(counter).encode()).digest()
            keystream_index = 0
        output[i] = ciphertext[i] ^ current_block[keystream_index]
        keystream_index += 1
    return output.decode('utf-8', errors='replace')

def encrypt_inner(text: str, key: bytes, sender_pk: str) -> str:
    import os
    iv = os.urandom(12)
    plaintext = text.encode()
    ciphertext = bytearray(len(plaintext))
    counter = 0
    keystream_index = 0
    current_block = hashlib.sha256(key + iv).digest()
    for i in range(len(plaintext)):
        if keystream_index >= len(current_block):
            counter += 1
            current_block = hashlib.sha256(key + iv + str(counter).encode()).digest()
            keystream_index = 0
        ciphertext[i] = plaintext[i] ^ current_block[keystream_index]
        keystream_index += 1
    env = {
        'v': 1,
        'type': 'CHAT_MSG',
        'iv': iv.hex(),
        'ciphertext': ciphertext.hex(),
        'sender_pubkey': sender_pk
    }
    return json.dumps(env)

def create_gift_wrap(inner_json: str, recipient_pk: str) -> dict:
    import os, time, random
    sk_ephem = os.urandom(32)
    pk_ephem = bip.pubkey_gen(sk_ephem).hex()
    created_at = int(time.time()) + random.randint(-1000, 1000)
    kind = 1059
    tags = [['p', recipient_pk.lower()]]
    serialized = json.dumps([0, pk_ephem, created_at, kind, tags, inner_json], separators=(',', ':'), ensure_ascii=False)
    event_id = hashlib.sha256(serialized.encode()).hexdigest()
    sig = bip.schnorr_sign(bytes.fromhex(event_id), sk_ephem, os.urandom(32)).hex()
    return {
        'id': event_id,
        'pubkey': pk_ephem,
        'created_at': created_at,
        'kind': kind,
        'tags': tags,
        'content': inner_json,
        'sig': sig
    }

async def run_bob():
    print(f"Bob listening on wss://nos.lol for pubkey {BOB_PK}...")
    async with websockets.connect('wss://nos.lol') as ws:
        req = ['REQ', 'sub_bob_inbox', {'kinds': [1059], '#p': [BOB_PK]}]
        await ws.send(json.dumps(req))
        print("Subscription sent. Waiting for message from Alice...")

        while True:
            msg = await ws.recv()
            data = json.loads(msg)
            if data[0] == 'EVENT':
                event = data[2]
                print(f"[BOB] Received Kind 1059 Event: {event['id']}")
                try:
                    envelope = json.loads(event['content'])
                    if not isinstance(envelope, dict) or 'sender_pubkey' not in envelope:
                        continue
                    alice_pk = envelope['sender_pubkey']
                    chat_key = derive_conversation_key(alice_pk, BOB_PK)
                    decrypted_text = decrypt_inner(envelope['ciphertext'], chat_key, envelope['iv'])
                    print(f"==================================================")
                    print(f"[BOB SUCCESS] Received message from Alice ({alice_pk}):")
                    print(f"Text: '{decrypted_text}'")
                    print(f"==================================================")

                    with open('C:/Users/ttt79/.gemini/antigravity/scratch/nostr-expressive-messenger/bob_received.json', 'w', encoding='utf-8') as f:
                        json.dump({
                            'event_id': event['id'],
                            'sender': alice_pk,
                            'recipient': BOB_PK,
                            'text': decrypted_text
                        }, f, ensure_ascii=False, indent=2)

                    reply_text = f"Привет, Алиса! Я получил твое сообщение: '{decrypted_text}'. Доставка через nos.lol подтверждена!"
                    reply_inner = encrypt_inner(reply_text, chat_key, BOB_PK)
                    reply_wrap = create_gift_wrap(reply_inner, alice_pk)
                    await ws.send(json.dumps(['EVENT', reply_wrap]))
                    print(f"[BOB] Sent reply to Alice ({reply_wrap['id']})!")
                    return
                except Exception as e:
                    print(f"[BOB] Skipped event or error: {e}")

if __name__ == '__main__':
    asyncio.run(run_bob())
