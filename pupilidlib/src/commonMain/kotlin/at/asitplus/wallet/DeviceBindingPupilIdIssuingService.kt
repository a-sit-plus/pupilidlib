package at.asitplus.wallet

import at.asitplus.wallet.lib.agent.Agent
import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.IssueCredentialMessenger
import at.asitplus.wallet.lib.agent.NextMessage
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.ConstantIndex
import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*

/**
 * @param engine can be passed to enable mocks in unit tests, to use the default engine, pass null
 */
class DeviceBindingPupilIdIssuingService(
    private val extAuthNonce: String,
    private val serverAddress: String,
    private val deviceAdapter: DeviceAdapter,
    private val asn1Service: Asn1Service,
    private val agentCryptoService: CryptoService,
    private val subjectCredentialStore: SubjectCredentialStore,
    engine: HttpClientEngine? = null
) {

    private val client: HttpClient = engine?.let {
        HttpClient(it) {
            install(ContentNegotiation) {
                json()
            }
        }
    } ?: HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }
    private val deviceBindingService = DeviceBindingService(
        extAuthNonce = extAuthNonce,
        serverAddress = serverAddress,
        deviceAdapter = deviceAdapter,
        asn1Service = asn1Service,
        engine = engine
    )

    suspend fun createDeviceBindingAndIssueCredentials(): ServiceResult {
        try {
            val deviceBindingResponse = deviceBindingService.getDeviceBindingResponse()
            val xAuthToken = deviceBindingResponse.headers[HeaderNames.X_AUTH_TOKEN].toString()
            val bindingConfirmBody = deviceBindingResponse.body<BindingConfirmResponse>()
            if (!bindingConfirmBody.success) {
                return ServiceResult.ErrorFromNetwork("No success for binding/confirm message")
            }

            val agent = Agent(cryptoService = agentCryptoService, subjectCredentialStore = subjectCredentialStore)
            val messenger = IssueCredentialMessenger(agent = agent, credentialScheme = ConstantIndex.PupilId)

            val message = messenger.startDirect()
            if (message !is NextMessage.Send)
                throw IllegalArgumentException("No issuing message from native code")
                    .also { Napier.w("Message from messenger: $message") }

            val initialPost = client.post("$serverAddress/pupilid/issue") {
                contentType(ContentType.Text.Plain)
                headers {
                    append(HeaderNames.X_AUTH_TOKEN, xAuthToken)
                }
                setBody(message.message)
            }
            val response = initialPost.body<String>()

            return ServiceResult.Success(response)
        } catch (e: ResponseException) {
            return ServiceResult.ErrorFromNetwork(e.message ?: "Error")
                .also { Napier.w("Error from network", throwable = e) }
        } catch (e: Throwable) {
            return ServiceResult.ErrorFromNativeCode(e)
                .also { Napier.w("Error from native code", throwable = e) }
        }
    }

}