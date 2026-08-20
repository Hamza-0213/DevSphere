package com.example.document.powerpoint

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Lightweight pure-Kotlin parser for OLE2 Compound File Binary Format (CFBF).
 * Standard container format for legacy Microsoft Office files (.ppt, .doc, .xls).
 */
object Ole2Reader {

    private val OLE2_SIGNATURE = byteArrayOf(
        0xD0.toByte(), 0xCF.toByte(), 0x11.toByte(), 0xE0.toByte(),
        0xA1.toByte(), 0xB1.toByte(), 0x1A.toByte(), 0xE1.toByte()
    )

    fun isOle2File(bytes: ByteArray): Boolean {
        if (bytes.size < 8) return false
        for (i in 0 until 8) {
            if (bytes[i] != OLE2_SIGNATURE[i]) return false
        }
        return true
    }

    /**
     * Extract named streams from an OLE2 binary file (e.g., "PowerPoint Document", "Current User").
     */
    fun extractStreams(bytes: ByteArray): Map<String, ByteArray> {
        val result = mutableMapOf<String, ByteArray>()
        if (!isOle2File(bytes) || bytes.size < 512) return result

        try {
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

            val sectorShift = buffer.getShort(30).toInt()
            val sectorSize = 1 shl sectorShift // 512 or 4096
            val miniSectorShift = buffer.getShort(32).toInt()
            val miniSectorSize = 1 shl miniSectorShift // 64
            val csectFat = buffer.getInt(44)
            val dirStartSector = buffer.getInt(48)
            val miniCutoff = buffer.getInt(56)
            val miniFatStartSector = buffer.getInt(60)
            val csectMiniFat = buffer.getInt(64)
            val difStartSector = buffer.getInt(68)
            val csectDif = buffer.getInt(72)

            // Read DIFAT (Master Sector Allocation Table)
            val fatSectors = mutableListOf<Int>()
            // First 109 entries in header
            for (i in 0 until 109) {
                val offset = 76 + i * 4
                if (offset + 4 <= 512) {
                    val sec = buffer.getInt(offset)
                    if (sec >= 0 && sec < 0xFFFFFFFA.toInt()) {
                        fatSectors.add(sec)
                    }
                }
            }

            // Additional DIFAT sectors if any
            var currentDifSector = difStartSector
            while (currentDifSector >= 0 && currentDifSector < 0xFFFFFFFA.toInt()) {
                val difOffset = 512 + currentDifSector * sectorSize
                if (difOffset + sectorSize > bytes.size) break
                val entriesInSector = (sectorSize / 4) - 1
                for (i in 0 until entriesInSector) {
                    val sec = buffer.getInt(difOffset + i * 4)
                    if (sec >= 0 && sec < 0xFFFFFFFA.toInt()) {
                        fatSectors.add(sec)
                    }
                }
                currentDifSector = buffer.getInt(difOffset + entriesInSector * 4)
            }

            // Build FAT chain lookup table
            val fatEntries = mutableListOf<Int>()
            for (fatSec in fatSectors) {
                val secOffset = 512 + fatSec * sectorSize
                if (secOffset + sectorSize > bytes.size) break
                for (i in 0 until (sectorSize / 4)) {
                    fatEntries.add(buffer.getInt(secOffset + i * 4))
                }
            }

            // Read Directory Stream
            val dirBytes = readStreamChain(bytes, dirStartSector, fatEntries, sectorSize)
            if (dirBytes.isEmpty()) return result

            // MiniFAT Stream
            val miniFatEntries = mutableListOf<Int>()
            if (miniFatStartSector >= 0 && csectMiniFat > 0) {
                val miniFatBytes = readStreamChain(bytes, miniFatStartSector, fatEntries, sectorSize)
                val miniBuffer = ByteBuffer.wrap(miniFatBytes).order(ByteOrder.LITTLE_ENDIAN)
                val count = miniFatBytes.size / 4
                for (i in 0 until count) {
                    miniFatEntries.add(miniBuffer.getInt(i * 4))
                }
            }

            // First entry in Directory is Root Entry (Entry 0)
            var miniStreamContainerBytes = ByteArray(0)
            if (dirBytes.size >= 128) {
                val rootBuffer = ByteBuffer.wrap(dirBytes, 0, 128).order(ByteOrder.LITTLE_ENDIAN)
                val rootStartSector = rootBuffer.getInt(116)
                val rootStreamSize = rootBuffer.getLong(120)
                if (rootStartSector >= 0 && rootStreamSize > 0) {
                    val rootChain = readStreamChain(bytes, rootStartSector, fatEntries, sectorSize)
                    miniStreamContainerBytes = if (rootChain.size > rootStreamSize) {
                        rootChain.copyOf(rootStreamSize.toInt())
                    } else {
                        rootChain
                    }
                }
            }

            // Parse directory entries (128 bytes each)
            val numEntries = dirBytes.size / 128
            for (i in 0 until numEntries) {
                val entryOffset = i * 128
                val entryBuffer = ByteBuffer.wrap(dirBytes, entryOffset, 128).order(ByteOrder.LITTLE_ENDIAN)

                val nameLength = entryBuffer.getShort(64).toInt()
                if (nameLength <= 2) continue

                // Extract UTF-16LE entry name
                val nameChars = CharArray((nameLength / 2) - 1)
                for (c in nameChars.indices) {
                    nameChars[c] = entryBuffer.getChar(c * 2)
                }
                val entryName = String(nameChars)

                val objType = entryBuffer.get(66).toInt() // 2 = Stream
                if (objType == 2) {
                    val startSector = entryBuffer.getInt(116)
                    val streamSize = entryBuffer.getLong(120).toInt()

                    if (streamSize > 0 && startSector >= 0) {
                        if (streamSize < miniCutoff && miniStreamContainerBytes.isNotEmpty() && miniFatEntries.isNotEmpty()) {
                            // Mini Stream
                            val streamData = readMiniStreamChain(
                                miniStreamContainerBytes,
                                startSector,
                                miniFatEntries,
                                miniSectorSize,
                                streamSize
                            )
                            result[entryName] = streamData
                        } else {
                            // Regular Stream
                            val streamChain = readStreamChain(bytes, startSector, fatEntries, sectorSize)
                            val actualData = if (streamChain.size > streamSize) {
                                streamChain.copyOf(streamSize)
                            } else {
                                streamChain
                            }
                            result[entryName] = actualData
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result
    }

    private fun readStreamChain(
        rawBytes: ByteArray,
        startSector: Int,
        fat: List<Int>,
        sectorSize: Int
    ): ByteArray {
        val out = mutableListOf<Byte>()
        var currentSector = startSector
        val visited = mutableSetOf<Int>()

        while (currentSector >= 0 && currentSector < 0xFFFFFFFA.toInt()) {
            if (!visited.add(currentSector)) break // Guard against cycles
            val offset = 512 + currentSector * sectorSize
            if (offset + sectorSize > rawBytes.size) {
                val available = (rawBytes.size - offset).coerceAtLeast(0)
                if (available > 0) {
                    for (b in offset until offset + available) {
                        out.add(rawBytes[b])
                    }
                }
                break
            }
            for (b in offset until offset + sectorSize) {
                out.add(rawBytes[b])
            }

            if (currentSector >= fat.size) break
            currentSector = fat[currentSector]
        }

        return out.toByteArray()
    }

    private fun readMiniStreamChain(
        miniStream: ByteArray,
        startSector: Int,
        miniFat: List<Int>,
        miniSectorSize: Int,
        expectedSize: Int
    ): ByteArray {
        val out = mutableListOf<Byte>()
        var currentSector = startSector
        val visited = mutableSetOf<Int>()

        while (currentSector >= 0 && currentSector < 0xFFFFFFFA.toInt() && out.size < expectedSize) {
            if (!visited.add(currentSector)) break
            val offset = currentSector * miniSectorSize
            if (offset + miniSectorSize > miniStream.size) {
                val available = (miniStream.size - offset).coerceAtLeast(0)
                for (b in offset until offset + available) {
                    out.add(miniStream[b])
                }
                break
            }
            for (b in offset until offset + miniSectorSize) {
                out.add(miniStream[b])
            }

            if (currentSector >= miniFat.size) break
            currentSector = miniFat[currentSector]
        }

        val result = out.toByteArray()
        return if (result.size > expectedSize) result.copyOf(expectedSize) else result
    }
}
