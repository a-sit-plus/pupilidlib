package at.asitplus.wallet.pupilid

import at.asitplus.wallet.utils.HeaderNames
import io.github.aakira.napier.Napier
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SelectedSchool(
    val id: String,
)

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

    suspend fun getEcoRequestRetrieve(xAuthToken: String): HttpResponse {
        val url = URLBuilder(serverAddress).appendPathSegments("pupilid", "schools", "retrieve").buildString()
        val response = client.get(url) {
            headers {
                append(HeaderNames.X_AUTH_TOKEN, xAuthToken)
            }
            expectSuccess = true
        }
        return response
    }

    suspend fun postEcoRequestSelect(xAuthToken: String, schoolId: String): HttpResponse {
        val url = URLBuilder(serverAddress).appendPathSegments("pupilid", "schools", "select").buildString()
        val selectedSchool = SelectedSchool(schoolId)
        Napier.d("Set body to: ${Json.encodeToString(selectedSchool)}")

        val response = client.post(url) {
            headers {
                append(HeaderNames.X_AUTH_TOKEN, xAuthToken)
            }
            contentType(ContentType.Application.Json)
            setBody(selectedSchool)
            expectSuccess = true
        }
        return response
    }

    suspend fun getEcoRequestCheck(xAuthToken: String): HttpResponse {
        val url = URLBuilder(serverAddress).appendPathSegments("pupilid", "schools", "check").buildString()
        val response = client.get(url) {
            headers {
                append(HeaderNames.X_AUTH_TOKEN, xAuthToken)
            }
            expectSuccess = true
        }
        return response
    }
}