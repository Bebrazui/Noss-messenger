package com.nostr.expressive.shared.crypto

/**
 * NIP-19 Bech32 implementation for Nostr entities (nsec, npub)
 */
object Nip19 {
    private const val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
    private val GENERATOR = intArrayOf(0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3)

    fun encode(hrp: String, data: ByteArray): String {
        val converted = convertBits(data, 8, 5, true)
        val values = hrpExpand(hrp) + converted
        val checksum = createChecksum(hrp, converted)
        val combined = converted + checksum
        val sb = StringBuilder(hrp).append('1')
        for (b in combined) {
            sb.append(CHARSET[b.toInt()])
        }
        return sb.toString()
    }

    fun decode(bech32: String): Pair<String, ByteArray> {
        val lower = bech32.lowercase()
        val pos = lower.lastIndexOf('1')
        require(pos in 1..lower.length - 7) { "Invalid Bech32 separator position" }
        val hrp = lower.substring(0, pos)
        val dataChars = lower.substring(pos + 1)
        val data = ByteArray(dataChars.length)
        for (i in dataChars.indices) {
            val c = dataChars[i]
            val idx = CHARSET.indexOf(c)
            require(idx != -1) { "Invalid Bech32 character: $c" }
            data[i] = idx.toByte()
        }
        require(verifyChecksum(hrp, data)) { "Invalid Bech32 checksum" }
        val payload5bit = data.copyOfRange(0, data.size - 6)
        val converted = convertBits(payload5bit, 5, 8, false)
        return hrp to converted
    }

    fun toNpub(pubkeyHex: String): String {
        return encode("npub", pubkeyHex.hexToByteArray())
    }

    fun toNsec(privkeyHex: String): String {
        return encode("nsec", privkeyHex.hexToByteArray())
    }

    fun parseNpub(npub: String): String {
        val (hrp, bytes) = decode(npub)
        require(hrp == "npub") { "Expected npub hrp, got $hrp" }
        return bytes.toHex()
    }

    fun parseNsec(nsec: String): String {
        val (hrp, bytes) = decode(nsec)
        require(hrp == "nsec") { "Expected nsec hrp, got $hrp" }
        return bytes.toHex()
    }

    private fun hrpExpand(hrp: String): ByteArray {
        val ret = ByteArray(hrp.length * 2 + 1)
        for (i in hrp.indices) {
            val c = hrp[i].code
            ret[i] = (c shr 5).toByte()
            ret[i + hrp.length + 1] = (c and 31).toByte()
        }
        ret[hrp.length] = 0
        return ret
    }

    private fun verifyChecksum(hrp: String, values: ByteArray): Boolean {
        val expanded = hrpExpand(hrp) + values
        return polymod(expanded) == 1
    }

    private fun createChecksum(hrp: String, values: ByteArray): ByteArray {
        val expanded = hrpExpand(hrp) + values + ByteArray(6)
        val poly = polymod(expanded) xor 1
        val ret = ByteArray(6)
        for (i in 0 until 6) {
            ret[i] = ((poly shr 5 * (5 - i)) and 31).toByte()
        }
        return ret
    }

    private fun polymod(values: ByteArray): Int {
        var chk = 1
        for (b in values) {
            val top = chk shr 25
            chk = (chk and 0x1ffffff shl 5) xor (b.toInt() and 0xff)
            for (i in 0 until 5) {
                if ((top shr i and 1) != 0) {
                    chk = chk xor GENERATOR[i]
                }
            }
        }
        return chk
    }

    private fun convertBits(data: ByteArray, fromBits: Int, toBits: Int, pad: Boolean): ByteArray {
        var acc = 0
        var bits = 0
        val maxv = (1 shl toBits) - 1
        val out = mutableListOf<Byte>()
        for (value in data) {
            val b = value.toInt() and 0xff
            acc = (acc shl fromBits) or b
            bits += fromBits
            while (bits >= toBits) {
                bits -= toBits
                out.add(((acc shr bits) and maxv).toByte())
            }
        }
        if (pad) {
            if (bits > 0) {
                out.add(((acc shl (toBits - bits)) and maxv).toByte())
            }
        } else {
            require(bits < fromBits) { "Excess padding" }
            require(((acc shl (toBits - bits)) and maxv) == 0) { "Non-zero padding" }
        }
        return out.toByteArray()
    }
}
