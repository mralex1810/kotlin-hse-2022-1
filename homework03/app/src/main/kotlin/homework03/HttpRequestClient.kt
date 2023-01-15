package homework03

import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpClient
import com.sun.jdi.request.InvalidRequestStateException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object HttpRequestClient {

    private val DEFAULT_HEADERS = Http.Headers(
        "Accept" to "application/json", "Accept-Encoding" to "deflate"
    )
    private val CLIENT = HttpClient()
    suspend fun processRequest(url: String): String = withContext(Dispatchers.IO) {
        val ret = CLIENT.request(
            Http.Method.GET, url, DEFAULT_HEADERS, null, HttpClient.RequestConfig(followRedirects = false)
        )
        if (ret.status != 200) {
            throw InvalidRequestStateException("Request on $url had status ${ret.status}")
        }
        return@withContext ret.readAllString()
    }
}