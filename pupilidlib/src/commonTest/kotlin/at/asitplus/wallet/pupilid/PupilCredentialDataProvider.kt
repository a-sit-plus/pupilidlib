package at.asitplus.wallet.pupilid

import at.asitplus.KmmResult
import at.asitplus.crypto.datatypes.CryptoPublicKey
import at.asitplus.crypto.datatypes.jws.toJsonWebKey
import at.asitplus.wallet.lib.agent.CredentialToBeIssued
import at.asitplus.wallet.lib.agent.Issuer
import at.asitplus.wallet.lib.agent.IssuerCredentialDataProvider
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.minutes

class PupilCredentialDataProvider(
    private val clock: Clock = Clock.System,
) : IssuerCredentialDataProvider {

    private val defaultLifetime = 1.minutes
    override fun getCredential(
        subjectPublicKey: CryptoPublicKey,
        credentialScheme: at.asitplus.wallet.lib.data.ConstantIndex.CredentialScheme,
        representation: at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation,
        claimNames: Collection<String>?
    ): KmmResult<List<CredentialToBeIssued>> {
        if (credentialScheme == ConstantIndex.PupilId && representation == at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation.PLAIN_JWT) {
            return KmmResult.success(
                listOf(
                    CredentialToBeIssued.VcJwt(
                        subject = dummyCredential(subjectPublicKey.keyId),
                        expiration = clock.now() + defaultLifetime,
                        attachments = dummyAttachments()
                    )
                )
            )
        }
        return KmmResult.failure(IllegalArgumentException("Unsupported credential scheme or representaion"))
    }

    private fun dummyCredential(subjectId: String) = PupilIdCredential(
        id = subjectId,
        firstName = "Susanne",
        lastName = "Meier",
        dateOfBirth = "2000-01-01",
        schoolName = "HTL",
        schoolCity = "Wien",
        schoolZip = "1010",
        schoolStreet = "Musterstraße 10",
        schoolId = "1234",
        pupilCity = "Wien",
        pupilZip = "1000",
        cardId = "5678",
        validUntil = "2033-12-31",
        pictureHash = byteArrayOf(8),
        scaledPictureHash = byteArrayOf(8),
    )

    private fun dummyAttachments() = listOf(
        Issuer.Attachment(
            ConstantIndex.PupilId.attachmentNamePicture, "image/webp", byteArrayOf(32)
        ),
        Issuer.Attachment(
            ConstantIndex.PupilId.attachmentNameScaledPicture, "image/webp", byteArrayOf(16)
        )
    )
}