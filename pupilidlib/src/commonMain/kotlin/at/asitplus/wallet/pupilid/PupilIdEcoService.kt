package at.asitplus.wallet.pupilid

import at.asitplus.wallet.utils.HeaderNames
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments

class PupilIdEcoService(
    /**
     * Base address of the server to contact, i.e. `https://wallet.a-sit.at/`
     */
    private val serverAddress: String,
    /**
     * Use this to configure an HTTP client once
     */
    httpClientBuilder: HttpClientBuilder,
    ) {

    private val client = httpClientBuilder.client


    suspend fun getEcoRequest(xAuthToken: String): String {
        val url = URLBuilder(serverAddress).appendPathSegments("pupilid", "schools", "retrieve").buildString()
        val response = client.get(url) {
            headers {
                append(HeaderNames.X_AUTH_TOKEN, xAuthToken)
            }
            expectSuccess = true
        }
        return response.body<String>()
    }
}