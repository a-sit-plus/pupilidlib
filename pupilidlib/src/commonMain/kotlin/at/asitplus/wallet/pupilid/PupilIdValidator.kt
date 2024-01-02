package at.asitplus.wallet.pupilid

import at.asitplus.wallet.lib.agent.Validator
import io.github.aakira.napier.Napier
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDate
import kotlinx.datetime.todayIn
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

/**
 * Code mainly imported from Android Verifier App
 */
class PupilIdValidator(
    private val clock: Clock = Clock.System,
    private val vcLibValidator: Validator = Validator.newDefaultInstance(),
    /**
     * Validity in hours for easy interop from Swift.
     */
    private val revocationListHardValidityHours: Int = 48,
    /**
     * Time leeway in seconds to add to time-related verifications.
     */
    private val timeLeewaySeconds: Int = 180,
    private val callback: Callback,
) {

    fun interface Callback {
        /**
         * Implementers: Load the revocation list from cache or download it,
         * parsing and verification happens afterwards by this library.
         */
        suspend fun loadRevocationList(url: String): RevocationList?
        data class RevocationList(
            val revocationList: String,
            val lastRetrieval: Instant
        )
    }

    /**
     * Calculates time validity and revocation validity of the [credential]
     */
    suspend fun validate(credential: CredentialContent): ValidityTuple {
        val revocationUri = credential.id
        if (!revocationUri.contains("#"))
            return ValidityTuple(TimeValidity.QrFormatInvalid("Could not parse revocation URI fragment"))
        val fragment = revocationUri.substringAfter("#")
        val revocationListIndex = fragment.toLongOrNull()
            ?: return ValidityTuple(TimeValidity.QrFormatInvalid("Could not parse revocation URI fragment"))
        val revocationUriWithoutId = revocationUri.removeSuffix("#$fragment")

        val timeValidity: TimeValidity = calcTimeValidity(credential)
            .also { Napier.d("TimeValidity is $it") }

        val revocationValidity = calcRevocationValidity(revocationUriWithoutId, revocationListIndex)
            .also { Napier.d("RevocationValidity is $it") }

        return ValidityTuple(timeValidity, revocationValidity)
    }

    private fun calcTimeValidity(credential: CredentialContent): TimeValidity {
        // start with valid, and let it be overwritten if following checks fail
        var timeValidity: TimeValidity

        // check short-lived time-validity of QR code (or rather its JWS payload)
        timeValidity = checkQrTimeValidity(credential)
            .also { Napier.d("QrCode TimeValidity set to $it") }

        val currentDate = clock.todayIn(TimeZone.currentSystemDefault())

        // check if ID is expired (hard, after grace period)
        val expiration = credential.expiration.toLocalDate()

        if (timeValidity is TimeValidity.CurrentlyValid && currentDate > expiration) {
            timeValidity = TimeValidity.IdExpiredAfterGracePeriod(expiration)
        }

        // check if ID is expired (soft, within grace period); needs to be after hard-expired check
        val validUntil = credential.validUntil.toLocalDate()
        if (timeValidity is TimeValidity.CurrentlyValid && currentDate > validUntil) {
            timeValidity = TimeValidity.IdExpiredInGracePeriod(validUntil)
        }
        return timeValidity
    }

    private suspend fun calcRevocationValidity(
        revocationUriWithoutId: String,
        revocationListIndex: Long,
    ): RevocationValidity {  //TODO correct error types?
        val revocationList = callback.loadRevocationList(revocationUriWithoutId)
            ?: return RevocationValidity.UnknownNoCrl
        if (!vcLibValidator.setRevocationList(revocationList.revocationList)) {
            return RevocationValidity.UnknownNoCrl
        }
        val revocationResult = vcLibValidator.checkRevocationStatus(revocationListIndex)
            .also { Napier.i("Revocation check returned $it") }

        return when (revocationResult) {
            Validator.RevocationStatus.UNKNOWN -> RevocationValidity.UnknownNoCrl
            Validator.RevocationStatus.REVOKED -> RevocationValidity.Revoked
            else -> {
                if (clock.now() - revocationList.lastRetrieval > revocationListHardValidityHours.hours) {
                    RevocationValidity.NotRevokedButOldCrl(revocationList.lastRetrieval)
                } else {
                    RevocationValidity.NotRevoked
                }
            }
        }
    }

    private fun checkQrTimeValidity(credential: CredentialContent): TimeValidity {
        val now = clock.now()
        val notBefore = runCatching { Instant.fromEpochSeconds(credential.qrCodeNotBefore.toLong()) }
            .getOrElse { return TimeValidity.QrFormatInvalid("Could not parse notBefore of QR") }
        val expires = runCatching { Instant.fromEpochSeconds(credential.qrCodeExpiration.toLong()) }
            .getOrElse { return TimeValidity.QrFormatInvalid("Could not parse expires of QR") }
        //TODO what does kotlin lib do on wrong values??
        return when {
            now > (expires + timeLeewaySeconds.seconds) -> TimeValidity.QrExpired(expires, clock)
            now < (notBefore - timeLeewaySeconds.seconds) -> TimeValidity.QrNotYetValid(notBefore, clock)
            else -> TimeValidity.CurrentlyValid
        }
    }
}


sealed class TimeValidity {
    data class QrFormatInvalid(val reason: String) : TimeValidity()
    data class QrNotYetValid(val notBefore: Instant, val clock: Clock = Clock.System) : TimeValidity() {
        fun toDuration(): Duration = (notBefore - clock.now())
    }

    data class QrExpired(val notAfter: Instant, val clock: Clock = Clock.System) : TimeValidity() {
        fun toDuration(): Duration = (clock.now() - notAfter)
    }

    data class IdExpiredInGracePeriod(val expiration: LocalDate) : TimeValidity()
    data class IdExpiredAfterGracePeriod(val expiration: LocalDate) : TimeValidity()
    data class IdNotAuthentic(val error: Throwable) : TimeValidity()
    object CurrentlyValid : TimeValidity()
}

sealed class RevocationValidity {
    object UnknownNoCrl : RevocationValidity()
    object Revoked : RevocationValidity()
    object NotRevoked : RevocationValidity()
    class NotRevokedButOldCrl(val lastUpdate: Instant) : RevocationValidity()
}

data class ValidityTuple(
    val timeValidity: TimeValidity,
    val revocationValidity: RevocationValidity = RevocationValidity.UnknownNoCrl
)
