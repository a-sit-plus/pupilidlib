package at.asitplus.wallet.pupilid

import at.asitplus.wallet.lib.agent.*
import at.asitplus.wallet.lib.msg.IssueCredential
import com.benasher44.uuid.uuid4
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.types.shouldBeInstanceOf

class IssueCredentialProtocolTest : FreeSpec({

    lateinit var issuerCryptoService: CryptoService
    lateinit var holderCryptoService: CryptoService
    lateinit var issuer: Issuer
    lateinit var holder: Holder
    lateinit var issuerProtocol: IssueCredentialProtocol
    lateinit var holderProtocol: IssueCredentialProtocol

    beforeEach {
        issuerCryptoService = DefaultCryptoService()
        holderCryptoService = DefaultCryptoService()
        issuer = IssuerAgent.newDefaultInstance(issuerCryptoService, dataProvider = PupilCredentialDataProvider())
        holder = HolderAgent.newDefaultInstance(holderCryptoService)
        issuerProtocol = IssueCredentialProtocol.newIssuerInstance(
            issuer = issuer,
            serviceEndpoint = "https://example.com/issue?${uuid4()}",
            credentialScheme = at.asitplus.wallet.pupilid.ConstantIndex.PupilId,
        )
        holderProtocol = IssueCredentialProtocol.newHolderInstance(
            holder = holder,
            credentialScheme = at.asitplus.wallet.pupilid.ConstantIndex.PupilId,
        )

    }

    "issueCredentialPupilIdDirect" {

        val requestCredential = holderProtocol.startDirect()
        requestCredential.shouldBeInstanceOf<InternalNextMessage.SendAndWrap>()

        val parsedRequestCredential =
            issuerProtocol.parseMessage(requestCredential.message, holderCryptoService.toJsonWebKey())
        parsedRequestCredential.shouldBeInstanceOf<InternalNextMessage.SendAndWrap>()
        val issueCredential = parsedRequestCredential.message

        val parsedIssueCredential =
            holderProtocol.parseMessage(issueCredential, issuerCryptoService.toJsonWebKey())
        parsedIssueCredential.shouldBeInstanceOf<InternalNextMessage.Finished>()

        val issuedCredential = parsedIssueCredential.lastMessage
        issuedCredential.shouldBeInstanceOf<IssueCredential>()
    }

})
