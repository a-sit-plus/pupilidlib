package at.asitplus.wallet.pupilid

import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.DefaultCryptoService
import at.asitplus.wallet.lib.agent.Holder
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.InternalNextMessage
import at.asitplus.wallet.lib.agent.IssuerAgent
import at.asitplus.wallet.lib.agent.PresentProofProtocol
import at.asitplus.wallet.lib.agent.Verifier
import at.asitplus.wallet.lib.agent.VerifierAgent
import at.asitplus.wallet.lib.msg.Presentation
import at.asitplus.testballoon.matrix.matrixSuite
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.aroundEachTest
import io.kotest.matchers.types.shouldBeInstanceOf

val PresentProofProtocolTest by matrixSuite {

    lateinit var holderCryptoService: CryptoService
    lateinit var verifierCryptoService: CryptoService
    lateinit var holder: Holder
    lateinit var verifier: Verifier
    lateinit var holderProtocol: PresentProofProtocol
    lateinit var verifierProtocol: PresentProofProtocol


    testSuite("tests", testConfig = TestConfig.aroundEachTest { action ->
        holderCryptoService = DefaultCryptoService()
        verifierCryptoService = DefaultCryptoService()
        holder = HolderAgent.newDefaultInstance(holderCryptoService)
        verifier = VerifierAgent.newDefaultInstance(verifierCryptoService.keyId)
        holderProtocol = PresentProofProtocol.newHolderInstance(
            holder,
            holderCryptoService.keyId,
            "https://example.com/"
        )
        verifierProtocol =
            PresentProofProtocol.newVerifierInstance(verifier, verifierCryptoService.keyId)
        action()
    }) {
        "presentProofPupilIdDirect" {
            holderProtocol = PresentProofProtocol.newHolderInstance(
                holder,
                holderCryptoService.keyId,
                "https://example.com",
                PupilIdConstants.Scheme
            )
            verifierProtocol = PresentProofProtocol.newVerifierInstance(
                verifier,
                verifierCryptoService.keyId,
                "https://example.com",
                PupilIdConstants.Scheme
            )

            holder.storeCredentials(
                IssuerAgent.newDefaultInstance(
                    DefaultCryptoService(),
                    dataProvider = PupilCredentialDataProvider(),
                ).issueCredential(
                    holderCryptoService.keyId,
                    PupilIdConstants.Scheme.vcType
                ).toStoreCredentialInput()
            )

            val requestPresentation = verifierProtocol.startDirect()
            requestPresentation.shouldBeInstanceOf<InternalNextMessage.SendAndWrap>()

            val parsedRequestPresentation = holderProtocol.parseMessage(
                requestPresentation.message,
                verifierCryptoService.keyId
            )
            parsedRequestPresentation.shouldBeInstanceOf<InternalNextMessage.SendAndWrap>()
            val presentation = parsedRequestPresentation.message

            val parsedPresentation =
                verifierProtocol.parseMessage(presentation, holderCryptoService.keyId)
            parsedPresentation.shouldBeInstanceOf<InternalNextMessage.Finished>()

            val receivedPresentation = parsedPresentation.lastMessage
            receivedPresentation.shouldBeInstanceOf<Presentation>()
        }
    }
}
