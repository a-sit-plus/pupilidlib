package at.asitplus.wallet.pupilid

import at.asitplus.wallet.lib.agent.*
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
        verifier = VerifierAgent.newDefaultInstance(verifierCryptoService.identifier)
        holderProtocol = PresentProofProtocol.newHolderInstance(
            holder = holder,
            serviceEndpoint = "https://example.com",
            credentialScheme = ConstantIndex.PupilId
        )
        verifierProtocol = PresentProofProtocol.newVerifierInstance(
            verifier = verifier,
            serviceEndpoint = "https://example.com",
            credentialScheme = ConstantIndex.PupilId
        )

    }

    "presentProofPupilIdDirect" {

        holder.storeCredentials(
            IssuerAgent.newDefaultInstance(
                DefaultCryptoService(),
                dataProvider = PupilCredentialDataProvider(),
            ).issueCredentialWithTypes(
                holderCryptoService.identifier,
                listOf(ConstantIndex.PupilId.vcType),
            ).toStoreCredentialInput()
        )

        val requestPresentation = verifierProtocol.startDirect()
        requestPresentation.shouldBeInstanceOf<InternalNextMessage.SendAndWrap>()

        val parsedRequestPresentation =
            holderProtocol.parseMessage(requestPresentation.message, verifierCryptoService.toJsonWebKey())
        parsedRequestPresentation.shouldBeInstanceOf<InternalNextMessage.SendAndWrap>()
        val presentation = parsedRequestPresentation.message

        val parsedPresentation =
            verifierProtocol.parseMessage(presentation, holderCryptoService.toJsonWebKey())
        parsedPresentation.shouldBeInstanceOf<InternalNextMessage.Finished>()

        val receivedPresentation = parsedPresentation.lastMessage
        receivedPresentation.shouldBeInstanceOf<Presentation>()
    }

})
