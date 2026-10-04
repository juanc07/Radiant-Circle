package com.thinkblox.radiantrush.logic

import java.math.BigInteger

/**
 * Instruction data ABI pinned to the verified ore-stake 0.3.0 Mainnet deployment
 * source commit 7628af733d9338e9560b353ef731a66efe9db3c8.
 */
object OreStakeInstructionRules {
    const val DEPOSIT_DISCRIMINATOR: Byte = 10
    const val WITHDRAW_DISCRIMINATOR: Byte = 11
    const val CLAIM_DISCRIMINATOR: Byte = 12
    const val COMPOUND_DISCRIMINATOR: Byte = 13

    private val U64_MAX = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE)

    /** Deposit preserves the existing compound fee and adds no new fee reserve in Phase 15B. */
    fun depositData(
        amount: BigInteger,
        compoundFeeLamports: BigInteger = BigInteger.ZERO,
        compoundFeeDepositLamports: BigInteger = BigInteger.ZERO,
    ): ByteArray =
        byteArrayOf(DEPOSIT_DISCRIMINATOR) +
            u64LittleEndian(amount) +
            u64LittleEndian(compoundFeeLamports) +
            u64LittleEndian(compoundFeeDepositLamports)

    fun withdrawData(amount: BigInteger): ByteArray =
        byteArrayOf(WITHDRAW_DISCRIMINATOR) + u64LittleEndian(amount)

    fun claimData(amount: BigInteger): ByteArray =
        byteArrayOf(CLAIM_DISCRIMINATOR) + u64LittleEndian(amount)

    fun compoundData(): ByteArray = byteArrayOf(COMPOUND_DISCRIMINATOR)

    private fun u64LittleEndian(value: BigInteger): ByteArray {
        require(value >= BigInteger.ZERO && value <= U64_MAX) { "Value is outside the protocol u64 range." }
        val output = ByteArray(8)
        var remaining = value
        for (index in output.indices) {
            output[index] = remaining.and(BigInteger.valueOf(0xffL)).toByte()
            remaining = remaining.shiftRight(8)
        }
        return output
    }
}
