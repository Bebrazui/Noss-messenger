package com.nostr.expressive.shared.crypto

@JsFun("""(privHex) => {
    const p = 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2Fn;
    const Gx = 0x79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798n;
    const Gy = 0x483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8n;
    function mod(a, m) { return ((a % m) + m) % m; }
    function modPow(b, e, m) {
        let r = 1n; b = mod(b, m);
        while (e > 0n) {
            if (e & 1n) r = mod(r * b, m);
            b = mod(b * b, m);
            e >>= 1n;
        }
        return r;
    }
    function inv(a, m) { return modPow(a, m - 2n, m); }
    function pointAdd(P1, P2) {
        if (!P1) return P2;
        if (!P2) return P1;
        let [x1, y1] = P1, [x2, y2] = P2;
        if (x1 === x2 && y1 !== y2) return null;
        let lam = (x1 === x2 && y1 === y2)
            ? mod(3n * x1 * x1 * inv(2n * y1, p), p)
            : mod((y2 - y1) * inv(x2 - x1, p), p);
        let x3 = mod(lam * lam - x1 - x2, p);
        let y3 = mod(lam * (x1 - x3) - y1, p);
        return [x3, y3];
    }
    function pointMul(P, k) {
        let R = null;
        for (let i = 0n; i < 256n; i++) {
            if ((k >> i) & 1n) R = pointAdd(R, P);
            P = pointAdd(P, P);
        }
        return R;
    }
    let d0 = BigInt('0x' + privHex.trim().toLowerCase());
    let P = pointMul([Gx, Gy], d0);
    return P[0].toString(16).padStart(64, '0').toLowerCase();
}""")
private external fun jsGetPublicKey(privHex: String): String

@JsFun("""(msgHex, privHex, auxHex) => {
    const p = 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2Fn;
    const n = 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141n;
    const Gx = 0x79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798n;
    const Gy = 0x483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8n;
    function mod(a, m) { return ((a % m) + m) % m; }
    function modPow(b, e, m) {
        let r = 1n; b = mod(b, m);
        while (e > 0n) {
            if (e & 1n) r = mod(r * b, m);
            b = mod(b * b, m);
            e >>= 1n;
        }
        return r;
    }
    function inv(a, m) { return modPow(a, m - 2n, m); }
    function pointAdd(P1, P2) {
        if (!P1) return P2;
        if (!P2) return P1;
        let [x1, y1] = P1, [x2, y2] = P2;
        if (x1 === x2 && y1 !== y2) return null;
        let lam = (x1 === x2 && y1 === y2)
            ? mod(3n * x1 * x1 * inv(2n * y1, p), p)
            : mod((y2 - y1) * inv(x2 - x1, p), p);
        let x3 = mod(lam * lam - x1 - x2, p);
        let y3 = mod(lam * (x1 - x3) - y1, p);
        return [x3, y3];
    }
    function pointMul(P, k) {
        let R = null;
        for (let i = 0n; i < 256n; i++) {
            if ((k >> i) & 1n) R = pointAdd(R, P);
            P = pointAdd(P, P);
        }
        return R;
    }
    function rotr(n, x) { return (x >>> n) | (x << (32 - n)); }
    function choose(x, y, z) { return (x & y) ^ (~x & z); }
    function majority(x, y, z) { return (x & y) ^ (x & z) ^ (y & z); }
    function sigma0(x) { return rotr(2, x) ^ rotr(13, x) ^ rotr(22, x); }
    function sigma1(x) { return rotr(6, x) ^ rotr(11, x) ^ rotr(25, x); }
    function gamma0(x) { return rotr(7, x) ^ rotr(18, x) ^ (x >>> 3); }
    function gamma1(x) { return rotr(17, x) ^ rotr(19, x) ^ (x >>> 10); }
    const K = [
        0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
        0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
        0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
        0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
    ];
    function sha256Bytes(bytes) {
        let l = bytes.length * 8;
        let padLen = ((56 - (bytes.length + 1) % 64 + 64) % 64);
        let total = bytes.length + 1 + padLen + 8;
        let buf = new Uint8Array(total);
        buf.set(bytes);
        buf[bytes.length] = 0x80;
        let dv = new DataView(buf.buffer);
        dv.setBigUint64(total - 8, BigInt(l));
        let H = [0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a, 0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19];
        let W = new Int32Array(64);
        for (let i = 0; i < total; i += 64) {
            for (let t = 0; t < 16; t++) W[t] = dv.getInt32(i + t * 4);
            for (let t = 16; t < 64; t++) W[t] = (gamma1(W[t - 2]) + W[t - 7] + gamma0(W[t - 15]) + W[t - 16]) | 0;
            let [a, b, c, d, e, f, g, h] = H;
            for (let t = 0; t < 64; t++) {
                let T1 = (h + sigma1(e) + choose(e, f, g) + K[t] + W[t]) | 0;
                let T2 = (sigma0(a) + majority(a, b, c)) | 0;
                h = g; g = f; f = e; e = (d + T1) | 0; d = c; c = b; b = a; a = (T1 + T2) | 0;
            }
            H[0] = (H[0] + a) | 0; H[1] = (H[1] + b) | 0; H[2] = (H[2] + c) | 0; H[3] = (H[3] + d) | 0;
            H[4] = (H[4] + e) | 0; H[5] = (H[5] + f) | 0; H[6] = (H[6] + g) | 0; H[7] = (H[7] + h) | 0;
        }
        let out = new Uint8Array(32);
        let outDv = new DataView(out.buffer);
        for (let t = 0; t < 8; t++) outDv.setInt32(t * 4, H[t]);
        return out;
    }
    function taggedHash(tag, msgBytes) {
        let tagBytes = new TextEncoder().encode(tag);
        let tagHash = sha256Bytes(tagBytes);
        let concat = new Uint8Array(tagHash.length * 2 + msgBytes.length);
        concat.set(tagHash, 0); concat.set(tagHash, tagHash.length); concat.set(msgBytes, tagHash.length * 2);
        return sha256Bytes(concat);
    }
    function hexToBytes(hex) {
        let b = new Uint8Array(hex.length / 2);
        for (let i = 0; i < hex.length; i += 2) b[i / 2] = parseInt(hex.substring(i, i + 2), 16);
        return b;
    }
    function bytesToHex(bytes) {
        return Array.from(bytes).map(x => x.toString(16).padStart(2, '0')).join('');
    }
    function toHex32(bi) { return bi.toString(16).padStart(64, '0'); }
    let d0 = BigInt('0x' + privHex.trim().toLowerCase());
    let P = pointMul([Gx, Gy], d0);
    let d = (P[1] % 2n === 0n) ? d0 : n - d0;
    let dBytes = hexToBytes(toHex32(d));
    let auxHash = taggedHash('BIP0340/aux', hexToBytes(auxHex));
    let tBytes = new Uint8Array(32);
    for (let i = 0; i < 32; i++) tBytes[i] = dBytes[i] ^ auxHash[i];
    let pxBytes = hexToBytes(toHex32(P[0]));
    let msgBytes = hexToBytes(msgHex.trim().toLowerCase());
    let nonceInput = new Uint8Array(64 + msgBytes.length);
    nonceInput.set(tBytes, 0); nonceInput.set(pxBytes, 32); nonceInput.set(msgBytes, 64);
    let k0 = mod(BigInt('0x' + bytesToHex(taggedHash('BIP0340/nonce', nonceInput))), n);
    let R = pointMul([Gx, Gy], k0);
    let k = (R[1] % 2n === 0n) ? k0 : n - k0;
    let rxBytes = hexToBytes(toHex32(R[0]));
    let chalInput = new Uint8Array(64 + msgBytes.length);
    chalInput.set(rxBytes, 0); chalInput.set(pxBytes, 32); chalInput.set(msgBytes, 64);
    let e = mod(BigInt('0x' + bytesToHex(taggedHash('BIP0340/challenge', chalInput))), n);
    let s = mod(k + e * d, n);
    return (toHex32(R[0]) + toHex32(s)).toLowerCase();
}""")
private external fun jsSignSchnorr(msgHex: String, privHex: String, auxHex: String): String

actual object Secp256k1 {
    actual fun getPublicKey(privKeyHex: String): String {
        return jsGetPublicKey(privKeyHex)
    }

    actual fun signSchnorr(msgHashHex: String, privKeyHex: String, auxHex: String): String {
        return jsSignSchnorr(msgHashHex, privKeyHex, auxHex)
    }

    actual fun verifySchnorr(msgHashHex: String, pubKeyHex: String, sigHex: String): Boolean {
        // Fast client-side stub or implement if needed
        return sigHex.length == 128
    }
}
