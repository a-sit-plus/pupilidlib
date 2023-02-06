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
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.types.shouldBeInstanceOf

class PresentProofProtocolTest : FreeSpec({

    lateinit var holderCryptoService: CryptoService
    lateinit var verifierCryptoService: CryptoService
    lateinit var holder: Holder
    lateinit var verifier: Verifier
    lateinit var holderProtocol: PresentProofProtocol
    lateinit var verifierProtocol: PresentProofProtocol

    beforeEach {
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
    }

    "presentProofPupilIdDirect" {
        holderProtocol = PresentProofProtocol.newHolderInstance(
            holder,
            holderCryptoService.keyId,
            "https://example.com",
            ConstantIndex.PupilId
        )
        verifierProtocol = PresentProofProtocol.newVerifierInstance(
            verifier,
            verifierCryptoService.keyId,
            "https://example.com",
            ConstantIndex.PupilId
        )

        holder.storeCredentials(
            IssuerAgent.newDefaultInstance(
                DefaultCryptoService(),
                dataProvider = Foo(),
            ).issueCredential(
                holderCryptoService.keyId,
                ConstantIndex.PupilId.vcType
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

})
