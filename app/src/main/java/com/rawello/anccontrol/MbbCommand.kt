package com.rawello.anccontrol

import java.nio.ByteBuffer

class MbbCommand(
    private val serviceId: Int,
    private val commandId: Int,
    private val args: Map<Int, ByteArray>
) {
    fun toBytes(): ByteArray {
        val body = mutableListOf<Byte>()
        args.forEach { (pType, pValue) ->
            body.add(pType.toByte())
            body.add(pValue.size.toByte())
            body.addAll(pValue.toList())
        }

        val length = body.size + 2 + 1
        val result = mutableListOf<Byte>()
        result.add(90) // magic bytes
        result.add(0)
        result.add(length.toByte())
        result.add(0) // another magic byte
        result.add(serviceId.toByte())
        result.add(commandId.toByte())
        result.addAll(body)

        val crc = HuaweiSppPackage.crc16Xmodem(result.toByteArray())
        val crcBytes = ByteBuffer.allocate(2).putShort(crc).array()
        result.addAll(crcBytes.toList())

        return result.toByteArray()
    }
}