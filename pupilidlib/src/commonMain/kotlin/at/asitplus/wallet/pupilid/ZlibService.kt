package at.asitplus.wallet.pupilid

interface ZlibService {

    fun compress(input: ByteArray): ByteArray?

    fun decompress(input: ByteArray): ByteArray?

}

expect class DefaultZlibService() : ZlibService