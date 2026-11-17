package at.asitplus.wallet.pupilid

import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.DefaultCryptoService
import at.asitplus.wallet.lib.agent.Holder
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.IssueCredentialMessenger
import at.asitplus.wallet.lib.agent.IssueCredentialProtocolResult
import at.asitplus.wallet.lib.agent.Issuer
import at.asitplus.wallet.lib.agent.IssuerAgent
import at.asitplus.wallet.lib.agent.MessageWrapper
import at.asitplus.wallet.lib.agent.NextMessage
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.testballoon.matrix.matrixSuite
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.aroundEachTest
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

val IssueCredentialMessengerTest by matrixSuite {

    lateinit var issuerCryptoService: CryptoService
    lateinit var holderCryptoService: CryptoService
    lateinit var issuer: Issuer
    lateinit var holder: Holder
    lateinit var issuerServiceEndpoint: String
    lateinit var issuerMessenger: IssueCredentialMessenger
    lateinit var holderMessenger: IssueCredentialMessenger


    PupilIdInitializer


    fun initHolderMessenger() = IssueCredentialMessenger.newHolderInstance(
        holder = holder,
        keyId = holderCryptoService.keyId,
        messageWrapper = MessageWrapper(holderCryptoService),
    )

    fun initIssuerMessenger(scheme: ConstantIndex.CredentialScheme) =
        IssueCredentialMessenger.newIssuerInstance(
            issuer = issuer,
            keyId = issuerCryptoService.keyId,
            messageWrapper = MessageWrapper(issuerCryptoService),
            serviceEndpoint = issuerServiceEndpoint,
            credentialScheme = scheme,
        )

    suspend fun runProtocolFlow(): IssueCredentialProtocolResult {
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

    fun assertPupilIdVc(issuedCredentials: IssueCredentialProtocolResult) {
        issuedCredentials.accepted shouldHaveSize 1
        issuedCredentials.accepted.map { it.vc.credentialSubject }
            .forEach { it.shouldBeInstanceOf<PupilIdCredential>() }
        issuedCredentials.rejected.shouldBeEmpty()
    }

    testSuite("tests", testConfig = TestConfig.aroundEachTest { action ->
        issuerCryptoService = DefaultCryptoService()
        holderCryptoService = DefaultCryptoService()
        issuer = IssuerAgent.newDefaultInstance(issuerCryptoService, dataProvider = PupilCredentialDataProvider())
        holder = HolderAgent.newDefaultInstance(holderCryptoService)
        issuerServiceEndpoint = "https://example.com/issue?${uuid4()}"
        holderMessenger = initHolderMessenger()
        action()
    }) {
        "issueCredentialPupilId" {
            issuerMessenger = initIssuerMessenger(at.asitplus.wallet.pupilid.PupilIdConstants.Scheme)

            val issuedCredential = runProtocolFlow()

            assertPupilIdVc(issuedCredential)
        }
    }
}