package at.asitplus.wallet.pupilid

import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.DefaultCryptoService
import at.asitplus.wallet.lib.agent.Holder
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.InternalNextMessage
import at.asitplus.wallet.lib.agent.IssueCredentialProtocol
import at.asitplus.wallet.lib.agent.Issuer
import at.asitplus.wallet.lib.agent.IssuerAgent
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.msg.IssueCredential
import at.asitplus.testballoon.matrix.matrixSuite
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.aroundEachTest
import io.kotest.matchers.types.shouldBeInstanceOf

val IssueCredentialProtocolTest by matrixSuite {

    lateinit var issuerCryptoService: CryptoService
    lateinit var holderCryptoService: CryptoService
    lateinit var issuer: Issuer
    lateinit var holder: Holder
    lateinit var issuerProtocol: IssueCredentialProtocol
    lateinit var holderProtocol: IssueCredentialProtocol


    testSuite("tests", testConfig = TestConfig.aroundEachTest { action ->
        issuerCryptoService = DefaultCryptoService()
        holderCryptoService = DefaultCryptoService()
        issuer = IssuerAgent.newDefaultInstance(issuerCryptoService, dataProvider = PupilCredentialDataProvider())
        holder = HolderAgent.newDefaultInstance(holderCryptoService)
        issuerProtocol = IssueCredentialProtocol.newIssuerInstance(
            issuer = issuer,
            keyId = issuerCryptoService.keyId,
            serviceEndpoint = "https://example.com/issue?${uuid4()}",
            credentialScheme = ConstantIndex.Generic,
        )
        holderProtocol = IssueCredentialProtocol.newHolderInstance(
            holder = holder,
            keyId = holderCryptoService.keyId,
            credentialScheme = ConstantIndex.Generic,
        )
        action()
    }) {
        "issueCredentialPupilIdDirect" {
            issuerProtocol = IssueCredentialProtocol.newIssuerInstance(
                issuer = issuer,
                keyId = issuerCryptoService.keyId,
                serviceEndpoint = "https://example.com/issue?${uuid4()}",
                credentialScheme = at.asitplus.wallet.pupilid.PupilIdConstants.Scheme,
            )
            holderProtocol = IssueCredentialProtocol.newHolderInstance(
                holder = holder,
                keyId = holderCryptoService.keyId,
                credentialScheme = at.asitplus.wallet.pupilid.PupilIdConstants.Scheme,
            )

            val requestCredential = holderProtocol.startDirect()
            requestCredential.shouldBeInstanceOf<InternalNextMessage.SendAndWrap>()

            val parsedRequestCredential =
                issuerProtocol.parseMessage(requestCredential.message, holderCryptoService.keyId)
            parsedRequestCredential.shouldBeInstanceOf<InternalNextMessage.SendAndWrap>()
            val issueCredential = parsedRequestCredential.message

            val parsedIssueCredential =
                holderProtocol.parseMessage(issueCredential, issuerCryptoService.keyId)
            parsedIssueCredential.shouldBeInstanceOf<InternalNextMessage.Finished>()

            val issuedCredential = parsedIssueCredential.lastMessage
            issuedCredential.shouldBeInstanceOf<IssueCredential>()
        }
    }
}
