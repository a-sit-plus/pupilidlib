package at.asitplus.wallet.pupilid

import at.asitplus.KmmResult
import at.asitplus.wallet.lib.agent.Issuer
import at.asitplus.wallet.lib.agent.IssuerCredentialDataProvider
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.minutes

class PupilCredentialDataProvider(
    private val clock: Clock = Clock.System,
) : IssuerCredentialDataProvider {

    private val defaultLifetime = 1.minutes

    override fun getClaim(subjectId: String, attributeName: String) =
        KmmResult.failure<IssuerCredentialDataProvider.CredentialToBeIssued>(UnsupportedOperationException("no data"))

    override fun getCredential(subjectId: String, attributeType: String) = when (attributeType) {
        at.asitplus.wallet.pupilid.PupilIdConstants.Scheme.vcType -> KmmResult.success(
            IssuerCredentialDataProvider.CredentialToBeIssued(
                subject = dummyCredential(subjectId),
                expiration = clock.now() + defaultLifetime,
                attributeType = attributeType,
                attachments = dummyAttachments()
            )
        )

        else -> KmmResult.failure(UnsupportedOperationException("empty"))
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
            at.asitplus.wallet.pupilid.PupilIdConstants.Scheme.attachmentNamePicture, "image/webp", byteArrayOf(32)
        ),
        Issuer.Attachment(
            at.asitplus.wallet.pupilid.PupilIdConstants.Scheme.attachmentNameScaledPicture, "image/webp", byteArrayOf(16)
        )
    )
}