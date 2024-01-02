package at.asitplus.wallet.pupilid

import at.asitplus.KmmResult
import at.asitplus.crypto.datatypes.jws.JsonWebKey
import at.asitplus.crypto.datatypes.jws.JwsAlgorithm
import at.asitplus.crypto.datatypes.jws.JwsHeader
import at.asitplus.crypto.datatypes.jws.JwsSigned
import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.DefaultCryptoService
import at.asitplus.wallet.lib.agent.DefaultVerifierCryptoService
import at.asitplus.wallet.lib.agent.VerifierCryptoService
import at.asitplus.wallet.lib.jws.DefaultJwsService
import at.asitplus.wallet.lib.jws.DefaultVerifierJwsService
import at.asitplus.wallet.lib.jws.JwsService
import at.asitplus.wallet.lib.jws.VerifierJwsService
import io.github.aakira.napier.Napier

/**
 * Combines [QrCodeContentService] and [JwsService] from vclib
 * to implement creating and validating QR Codes for verification of PupilIds.
 */
class PupilIdQrCodeService(
    private val jwsService: JwsService = DefaultJwsService(DefaultCryptoService()),
    private val verifierJwsService: VerifierJwsService = DefaultVerifierJwsService(DefaultVerifierCryptoService())
) {

    companion object {
        /**
         * Creates new instance of [PupilIdQrCodeService] for Wallet apps
         */
        fun newInstance(cryptoService: CryptoService): PupilIdQrCodeService =
            PupilIdQrCodeService(jwsService = DefaultJwsService(cryptoService))

        /**
         * Creates new instance of [PupilIdQrCodeService] for Verifier apps
         */
        fun newInstance(verifierCryptoService: VerifierCryptoService): PupilIdQrCodeService =
            PupilIdQrCodeService(verifierJwsService = DefaultVerifierJwsService(verifierCryptoService))

        fun newInstance(
            cryptoService: CryptoService,
            verifierCryptoService: VerifierCryptoService
        ): PupilIdQrCodeService = PupilIdQrCodeService(
            jwsService = DefaultJwsService(cryptoService),
            verifierJwsService = DefaultVerifierJwsService(verifierCryptoService)
        )

        private val CHARSET = ('0'..'9') + ('a'..'z') + ('A'..'Z')
        private const val LENGTH_RANDOM_PREFIX = 8
    }

    /**
     * Creates a list of QR Codes to display in the Wallet App.
     *
     * Calls [QrCodeContentService.encodeInQrCode] and implements its callback
     * with the local [jwsService].
     */
    suspend fun encodeInQrCode(
        content: CredentialContent,
        picture: ByteArray,
        splitLength: Int = 1701,
    ): KmmResult<List<String>> = QrCodeContentService.encodeInQrCode(
        content = content,
        picture = picture,
        splitLength = splitLength,
        prefix = "PI1:${List(LENGTH_RANDOM_PREFIX) { CHARSET.random() }.joinToString("")}:"
    ) { input ->
        val jwsHeader = JwsHeader(algorithm = JwsAlgorithm.ES256)
        val jws = jwsService.createSignedJws(header = jwsHeader, payload = input.encodeToByteArray())
        jws?.let { KmmResult.success(it.serialize()) } ?: KmmResult.failure(Throwable("Could not sign JWS"))
    }

    /**
     * Decodes a list of QR Codes by calling [QrCodeContentService.decodeQrCode],
     * and verifying the JWS inside it by extracting the custom structures of [CredentialContent]
     * and [AttestedPublicKey], ultimately calling [verifierJwsService].
     * Verifies signatures of [AttestedPublicKey] using the list of known issuers in [pinnedAttestedKeyIssuers].
     *
     * Custom exceptions returned in the [KmmResult] may be:
     * - [QrCodeDecodeError] if the format is not correct
     * - [JwsNotVerifiedError] if the JWS can not be verified
     * - [AttestedKeyNotVerifiedError] if no issuer from [pinnedAttestedKeyIssuers] signed the attested key
     */
    fun decodeQrCode(
        input: Collection<String>,
        prefix: String = "PI1:",
        stripSecondPrefix: Boolean = false,
        pinnedAttestedKeyIssuers: Collection<JsonWebKey>,
    ): KmmResult<QrCodeContentService.DecodeResult> {
        val callback = object : QrCodeContentService.DecodeCallback {
            override fun verifyJwsExtractPayload(input: String): KmmResult<String> {
                val parsedJws = JwsSigned.parse(input)
                    ?: Napier.d("decodeQrCode: Can not parse JWS: $input")
                        .run { return KmmResult.failure(QrCodeDecodeError()) }
                val content = CredentialContent.deserialize(parsedJws.payload.decodeToString())
                    ?: Napier.d("decodeQrCode: Can not parse CredentialContent: ${parsedJws.payload.decodeToString()}")
                        .run { return KmmResult.failure(QrCodeDecodeError()) }
                val jwsAttestedKey = JwsSigned.parse(content.key)
                    ?: Napier.d("decodeQrCode: Can not parse JWS: ${content.key}")
                        .run { return KmmResult.failure(QrCodeDecodeError()) }
                val attestedKeyVerified =
                    pinnedAttestedKeyIssuers.any { verifierJwsService.verifyJws(jwsAttestedKey, it) }
                if (!attestedKeyVerified || pinnedAttestedKeyIssuers.isEmpty()) {
                    Napier.d("decodeQrCode: Could not verify JWS of attested public key")
                    return KmmResult.failure(AttestedKeyNotVerifiedError())
                }
                val attestedKey = AttestedPublicKey.deserialize(jwsAttestedKey.payload.decodeToString())
                    ?: Napier.d("decodeQrCode: Could not deserialize AttestedPublicKey: ${jwsAttestedKey.payload.decodeToString()}")
                        .run { return KmmResult.failure(QrCodeDecodeError()) }
                val jsonWebKey = JsonWebKey.fromKeyId(attestedKey.kid).getOrNull()
                    ?: Napier.d("decodeQrCode: Could not read JWK from ${attestedKey.kid}")
                        .run { return KmmResult.failure(QrCodeDecodeError()) }
                if (!verifierJwsService.verifyJws(parsedJws, jsonWebKey)) {
                    Napier.d("decodeQrCode: JWS not verified: $parsedJws with $jsonWebKey")
                    return KmmResult.failure(JwsNotVerifiedError())
                }
                val result = parsedJws.payload.decodeToString()
                Napier.d("decodeQrCode: Success")
                Napier.v("... returning $result")
                return KmmResult.success(result)
            }
        }
        return QrCodeContentService.decodeQrCode(
            input = input,
            prefix = prefix,
            stripSecondPrefix = stripSecondPrefix,
            callback = callback
        )
    }

}

class QrCodeDecodeError : Throwable("QR Code decoding failed")

class JwsNotVerifiedError : Throwable("JWS not verified")

class AttestedKeyNotVerifiedError : Throwable("Attested key not verified")
