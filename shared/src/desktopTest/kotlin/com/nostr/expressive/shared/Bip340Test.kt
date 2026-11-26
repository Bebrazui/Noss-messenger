package com.nostr.expressive.shared

import java.math.BigInteger
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Bip340Test {

    private val p = BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F", 16)
    private val n = BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16)
    private val Gx = BigInteger("79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798", 16)
    private val Gy = BigInteger("483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8", 16)

    data class Point(val x: BigInteger, val y: BigInteger)

    private fun sha256(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(data)

    private fun taggedHash(tag: String, msg: ByteArray): ByteArray {
        val tagHash = sha256(tag.encodeToByteArray())
        return sha256(tagHash + tagHash + msg)
    }

    private fun inv(a: BigInteger, m: BigInteger): BigInteger = a.modInverse(m)

    private fun pointAdd(p1: Point?, p2: Point?): Point? {
        if (p1 == null) return p2
        if (p2 == null) return p1
        if (p1.x == p2.x && p1.y != p2.y) return null
        val lam = if (p1.x == p2.x && p1.y == p2.y) {
            val num = BigInteger.valueOf(3).multiply(p1.x).multiply(p1.x).mod(p)
            val den = BigInteger.valueOf(2).multiply(p1.y).mod(p)
            num.multiply(inv(den, p)).mod(p)
        } else {
            val num = p2.y.subtract(p1.y).mod(p)
            val den = p2.x.subtract(p1.x).mod(p)
            num.multiply(inv(den, p)).mod(p)
        }
        val x3 = lam.multiply(lam).subtract(p1.x).subtract(p2.x).mod(p)
        val y3 = lam.multiply(p1.x.subtract(x3)).subtract(p1.y).mod(p)
        return Point(x3, y3)
    }

    private fun pointMul(p: Point, k: BigInteger): Point? {
        var r: Point? = null
        var t: Point? = p
        for (i in 0 until 256) {
            if (k.testBit(i)) {
                r = pointAdd(r, t)
            }
            t = pointAdd(t, t)
        }
        return r
    }

    private fun toHex32(b: BigInteger): String = b.toString(16).padStart(64, '0')

    private fun getPublicKey(privHex: String): String {
        val d0 = BigInteger(privHex, 16)
        val p = pointMul(Point(Gx, Gy), d0) ?: error("Invalid point")
        return toHex32(p.x)
    }

    private fun signSchnorr(msgHex: String, privHex: String, auxHex: String): String {
        val d0 = BigInteger(privHex, 16)
        val P = pointMul(Point(Gx, Gy), d0) ?: error("Point at infinity")
        val d = if (P.y.testBit(0)) n.subtract(d0) else d0
        val dBytes = hexToBytes(toHex32(d))
        val auxHash = taggedHash("BIP0340/aux", hexToBytes(auxHex))
        val tBytes = ByteArray(32) { i -> (dBytes[i].toInt() xor auxHash[i].toInt()).toByte() }
        val pxBytes = hexToBytes(toHex32(P.x))
        val msgBytes = hexToBytes(msgHex)
        val k0 = BigInteger(1, taggedHash("BIP0340/nonce", tBytes + pxBytes + msgBytes)).mod(n)
        val R = pointMul(Point(Gx, Gy), k0) ?: error("R is infinity")
        val k = if (R.y.testBit(0)) n.subtract(k0) else k0
        val rxBytes = hexToBytes(toHex32(R.x))
        val e = BigInteger(1, taggedHash("BIP0340/challenge", rxBytes + pxBytes + msgBytes)).mod(n)
        val s = k.add(e.multiply(d)).mod(n)
        return toHex32(R.x) + toHex32(s)
    }

    private fun verifySchnorr(msgHex: String, pubHex: String, sigHex: String): Boolean {
        if (pubHex.length != 64 || sigHex.length != 128) return false
        val px = BigInteger(pubHex, 16)
        if (px >= p) return false
        val ySq = px.modPow(BigInteger.valueOf(3), p).add(BigInteger.valueOf(7)).mod(p)
        var y = ySq.modPow(p.add(BigInteger.ONE).divide(BigInteger.valueOf(4)), p)
        if (y.modPow(BigInteger.valueOf(2), p) != ySq) return false
        if (y.testBit(0)) y = p.subtract(y)
        val P = Point(px, y)
        val rx = BigInteger(sigHex.substring(0, 64), 16)
        val s = BigInteger(sigHex.substring(64, 128), 16)
        if (rx >= p || s >= n) return false
        val rxBytes = hexToBytes(sigHex.substring(0, 64))
        val pxBytes = hexToBytes(pubHex)
        val msgBytes = hexToBytes(msgHex)
        val e = BigInteger(1, taggedHash("BIP0340/challenge", rxBytes + pxBytes + msgBytes)).mod(n)
        val sG = pointMul(Point(Gx, Gy), s)
        val eP = pointMul(P, n.subtract(e))
        val R = pointAdd(sG, eP)
        return R != null && !R.y.testBit(0) && R.x == rx
    }

    private fun hexToBytes(hex: String): ByteArray {
        val result = ByteArray(hex.length / 2)
        for (i in result.indices) {
            result[i] = hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return result
    }

    @Test
    fun testBip340OfficialVector() {
        val sk = "B7E151628AED2A6ABF7158809CF4F3C762E7160F38B4DA56A784D9045190CFEF"
        val msg = "243F6A8885A308D313198A2E03707344A4093822299F31D0082EFA98EC4E6C89"
        val aux = "00".repeat(32)

        val pk = getPublicKey(sk)
        assertEquals("dff1d77f2a671c5f36183726db2341be58feae1da2deced843240f7b502ba659", pk.lowercase())

        val sig = signSchnorr(msg, sk, aux)
        val expectedSig = "eb8eadc001fa1f3d08f19db7027ddb0affa61c0357d4b577f8bb1978837382c85ae9ccc675360d9055cb2a2bda001bc5c62df9b5ed936caccfd00b169ede131d"
        assertEquals(expectedSig.lowercase(), sig.lowercase())

        val valid = verifySchnorr(msg, pk, sig)
        assertTrue(valid)
    }
}
