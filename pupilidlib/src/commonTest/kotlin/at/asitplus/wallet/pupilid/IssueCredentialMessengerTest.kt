package at.asitplus.wallet.pupilid

import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.DefaultCryptoService
import at.asitplus.wallet.lib.agent.Holder
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.Issuer
import at.asitplus.wallet.lib.agent.IssuerAgent
import at.asitplus.wallet.lib.aries.IssueCredentialMessenger
import at.asitplus.wallet.lib.aries.IssueCredentialProtocolResult
import at.asitplus.wallet.lib.aries.MessageWrapper
import at.asitplus.wallet.lib.aries.NextMessage
import at.asitplus.wallet.lib.data.ConstantIndex
import com.benasher44.uuid.uuid4
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class IssueCredentialMessengerTest : FreeSpec() {

    lateinit var issuerCryptoService: CryptoService
    lateinit var holderCryptoService: CryptoService
    lateinit var issuer: Issuer
    lateinit var holder: Holder
    lateinit var issuerServiceEndpoint: String
    lateinit var issuerMessenger: IssueCredentialMessenger
    lateinit var holderMessenger: IssueCredentialMessenger

    init {
        Initializer
        beforeEach {
            issuerCryptoService = DefaultCryptoService()
            holderCryptoService = DefaultCryptoService()
            issuer = IssuerAgent.newDefaultInstance(issuerCryptoService, dataProvider = PupilCredentialDataProvider())
            holder = HolderAgent.newDefaultInstance(holderCryptoService)
            issuerServiceEndpoint = "https://example.com/issue?${uuid4()}"
            holderMessenger = initHolderMessenger()
        }

        "issueCredentialPupilId" {
            issuerMessenger = initIssuerMessenger(at.asitplus.wallet.pupilid.ConstantIndex.PupilId)

            val issuedCredential = runProtocolFlow()

            assertPupilIdVc(issuedCredential)
        }
    }

    private fun initHolderMessenger() = IssueCredentialMessenger.newHolderInstance(
        holder = holder,
        messageWrapper = MessageWrapper(holderCryptoService),
        credentialScheme = at.asitplus.wallet.pupilid.ConstantIndex.PupilId,
    )

    private fun initIssuerMessenger(scheme: ConstantIndex.CredentialScheme) =
        IssueCredentialMessenger.newIssuerInstance(
            issuer = issuer,
            messageWrapper = MessageWrapper(issuerCryptoService),
            serviceEndpoint = issuerServiceEndpoint,
            credentialScheme = scheme,
        )

    private suspend fun runProtocolFlow(): IssueCredentialProtocolResult {
        val oobInvitation = issuerMessenger.startCreatingInvitation()
        oobInvitation.shouldBeInstanceOf<NextMessage.Send>()
        val invitationMessage = oobInvitation.message

        val parsedInvitation = holderMessenger.parseMessage(invitationMessage)
        parsedInvitation.shouldBeInstanceOf<NextMessage.Send>()
        parsedInvitation.endpoint shouldBe issuerServiceEndpoint
        val requestCredential = parsedInvitation.message

        val parsedRequestCredential = issuerMessenger.parseMessage(requestCredential)
        parsedRequestCredential.shouldBeInstanceOf<NextMessage.Send>()
        val issueCredential = parsedRequestCredential.message

        val parsedIssueCredential = holderMessenger.parseMessage(issueCredential)
        parsedIssueCredential.shouldBeInstanceOf<NextMessage.Result<IssueCredentialProtocolResult>>()

        val issuedCredential = parsedIssueCredential.result
        issuedCredential.shouldBeInstanceOf<IssueCredentialProtocolResult>()
        return issuedCredential
    }

    private fun assertPupilIdVc(issuedCredentials: IssueCredentialProtocolResult) {
        issuedCredentials.acceptedVcJwt shouldHaveSize 1
        issuedCredentials.acceptedVcJwt.map { it.vc.credentialSubject }
            .forEach { it.shouldBeInstanceOf<PupilIdCredential>() }
        issuedCredentials.rejected.shouldBeEmpty()
    }
}