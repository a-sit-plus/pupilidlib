package at.asitplus.wallet.pupilid

import at.asitplus.wallet.lib.agent.DefaultCryptoService
import at.asitplus.wallet.lib.agent.InMemoryIssuerCredentialStore
import at.asitplus.wallet.lib.agent.IssuerAgent
import at.asitplus.wallet.lib.agent.Validator
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.matthewnelson.component.encoding.base16.encodeBase16
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDate
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalStdlibApi::class)
class PupilIdValidatorTest : ShouldSpec({

    val validCredential = CredentialContent(
        type = "type",
        id = "https://example.com/revocationList/#1",
        qrCodeNotBefore = "2023-01-04T08:01".toUnixTimestamp(),
        qrCodeExpiration = "2023-01-04T08:03".toUnixTimestamp(),
        validUntil = "2023-01-06",
        expiration = "2023-01-07",
        key = "key"
    )

    val revocationListHardValidityHours = 48
    val timeLeewaySeconds = 30

    context("with clock at 2023-01-04T08:02 and no revocation list") {

        val nullRevocationList = PupilIdValidator.Callback { null }
        val clock = TestClock("2023-01-04T08:02")
        val vcLibValidator = Validator.newDefaultInstance()
        val validator = PupilIdValidator(
            clock,
            vcLibValidator,
            revocationListHardValidityHours,
            timeLeewaySeconds,
            nullRevocationList
        )

        should("time valid") {
            val credential = validCredential.copy()

            val result = validator.validate(credential)

            result.timeValidity.shouldBeInstanceOf<TimeValidity.CurrentlyValid>()
        }

        should("time valid with expiration inside leeway") {
            val credential =
                validCredential.copy(qrCodeExpiration = (clock.now() - (timeLeewaySeconds - 1).seconds).toUnixTimestamp())

            val result = validator.validate(credential)

            result.timeValidity.shouldBeInstanceOf<TimeValidity.CurrentlyValid>()
        }

        should("time invalid with expiration outside leeway") {
            val credential =
                validCredential.copy(qrCodeExpiration = (clock.now() - (timeLeewaySeconds + 1).seconds).toUnixTimestamp())

            val result = validator.validate(credential)

            val timeValidity = result.timeValidity
            timeValidity.shouldBeInstanceOf<TimeValidity.QrExpired>()
        }

        should("time valid with notBefore inside leeway") {
            val credential =
                validCredential.copy(qrCodeNotBefore = (clock.now() + (timeLeewaySeconds - 1).seconds).toUnixTimestamp())

            val result = validator.validate(credential)

            result.timeValidity.shouldBeInstanceOf<TimeValidity.CurrentlyValid>()
        }

        should("time invalid with notBefore outside leeway") {
            val credential =
                validCredential.copy(qrCodeNotBefore = (clock.now() + (timeLeewaySeconds + 1).seconds).toUnixTimestamp())

            val result = validator.validate(credential)

            val timeValidity = result.timeValidity
            timeValidity.shouldBeInstanceOf<TimeValidity.QrNotYetValid>()
        }

        should("revocation list unknown") {
            val credential = validCredential.copy()

            val result = validator.validate(credential)

            result.revocationValidity.shouldBeInstanceOf<RevocationValidity.UnknownNoCrl>()
        }

        should("qr code not yet valid with qrCodeNotBefore at 08:03") {
            val credential = validCredential.copy(qrCodeNotBefore = "2023-01-04T08:03".toUnixTimestamp())

            val result = validator.validate(credential)

            val timeValidity = result.timeValidity
            timeValidity.shouldBeInstanceOf<TimeValidity.QrNotYetValid>()
            timeValidity.notBefore shouldBe "2023-01-04T08:03".toLocalInstant()
        }

        should("qr code expired with qrCodeExpiration at 08:01") {
            val credential = validCredential.copy(qrCodeExpiration = "2023-01-04T08:01".toUnixTimestamp())

            val result = validator.validate(credential)

            val timeValidity = result.timeValidity
            timeValidity.shouldBeInstanceOf<TimeValidity.QrExpired>()
            timeValidity.notAfter shouldBe "2023-01-04T08:01".toLocalInstant()
        }

        should("in grace period with validUntil at 2023-01-02") {
            val credential = validCredential.copy(validUntil = "2023-01-02")

            val result = validator.validate(credential)

            val timeValidity = result.timeValidity
            timeValidity.shouldBeInstanceOf<TimeValidity.IdExpiredInGracePeriod>()
            timeValidity.expiration shouldBe "2023-01-02".toLocalDate()
        }

        should("expired after grace period with expiration at 2023-01-03") {
            val credential = validCredential.copy(validUntil = "2023-01-02", expiration = "2023-01-03")

            val result = validator.validate(credential)

            val timeValidity = result.timeValidity
            timeValidity.shouldBeInstanceOf<TimeValidity.IdExpiredAfterGracePeriod>()
            timeValidity.expiration shouldBe "2023-01-03".toLocalDate()
        }
    }

    context("with revocation list where index 1 is revoked") {

        val clock = TestClock("2023-01-04T08:02")
        val timePeriodProvider = SchoolyearBasedTimePeriodProvider(Month.SEPTEMBER to 1u)
        val vcId = Random.Default.nextBytes(16).toHexString()
        val issuerCredentialStore = InMemoryIssuerCredentialStore()
        val issuerAgent = IssuerAgent.newDefaultInstance(
            cryptoService = DefaultCryptoService(),
            issuerCredentialStore = issuerCredentialStore,
            timePeriodProvider = timePeriodProvider
        )
        val cred = PupilIdCredential(
            "id",
            "firstname",
            "lastname",
            "dateOfBirth",
            "schoolName",
            "schoolCity",
            "schoolZip",
            "schoolStreet",
            "schoolId",
            "pupilCity",
            "pupilZip",
            "cardId",
            "validUntil",
            "pictureHash".encodeToByteArray(),
            "scaledPictureHash".encodeToByteArray()
        )
        val credentialIndex =
            issuerCredentialStore.storeGetNextIndex(vcId, cred, clock.now(), clock.now(), clock.now().localYear)
        issuerCredentialStore.revoke(vcId, clock.now().localYear)
        val revocationListCredential = issuerAgent.issueRevocationListCredential(clock.now().localYear)!!
        val latestRevocationList = PupilIdValidator.Callback {
            PupilIdValidator.Callback.RevocationList(
                revocationList = revocationListCredential,
                lastRetrieval = clock.now()
            )
        }
        val vcLibValidator = Validator.newDefaultInstance()
        val validator = PupilIdValidator(
            clock,
            vcLibValidator,
            revocationListHardValidityHours,
            timeLeewaySeconds,
            latestRevocationList
        )

        should("revoked") {
            val credential = validCredential.copy(id = "https://example.com/revocationList/#${credentialIndex}")

            val result = validator.validate(credential)

            result.revocationValidity.shouldBeInstanceOf<RevocationValidity.Revoked>()
        }

        should("not revoked") {
            val credential = validCredential.copy(id = "https://example.com/revocationList/#${credentialIndex + 1}")

            val result = validator.validate(credential)

            result.revocationValidity.shouldBeInstanceOf<RevocationValidity.NotRevoked>()
        }

        context("outdated revocation list") {
            val outdatedRevocationList = PupilIdValidator.Callback {
                PupilIdValidator.Callback.RevocationList(
                    revocationList = revocationListCredential,
                    lastRetrieval = clock.now().minus(revocationListHardValidityHours.hours + 1.hours),
                )
            }
            @Suppress("NAME_SHADOWING") val validator = PupilIdValidator(
                clock,
                vcLibValidator,
                revocationListHardValidityHours,
                timeLeewaySeconds,
                outdatedRevocationList
            )

            should("be marked as outdated") {
                val credential = validCredential.copy(id = "https://example.com/revocationList/#${credentialIndex + 1}")

                val result = validator.validate(credential)

                result.revocationValidity.shouldBeInstanceOf<RevocationValidity.NotRevokedButOldCrl>()
            }

        }
    }

})

class TestClock(localDate: String) : Clock {
    private val now: Instant = localDate.toLocalInstant()
    override fun now() = now
}

fun String.toUnixTimestamp() = this.toLocalDateTime().toInstant(TimeZone.currentSystemDefault()).epochSeconds.toInt()

fun Instant.toUnixTimestamp() = epochSeconds.toInt()

fun String.toLocalInstant() = this.toLocalDateTime().toInstant(TimeZone.currentSystemDefault())

val Instant.localYear get() = this.toLocalDateTime(TimeZone.currentSystemDefault()).year