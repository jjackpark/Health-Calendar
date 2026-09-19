package com.jjackpark.healthcalendar

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class SyncHttpException(val status: Int, message: String) : Exception(message)
object CalendarApi {
 suspend fun post(path: String, body: JSONObject, token: String? = null): JSONObject = withContext(Dispatchers.IO) {
  require(path in setOf("/api/device/pair", "/api/device/sync", "/api/device/revoke"))
  val connection = URL(BuildConfig.SITE_URL + path).openConnection() as HttpsURLConnection
  try {
   connection.requestMethod = "POST"
   connection.instanceFollowRedirects = false
   connection.connectTimeout = 15000
   connection.readTimeout = 25000
   connection.doOutput = true
   connection.setRequestProperty("Content-Type", "application/json")
   connection.setRequestProperty("Accept", "application/json")
   if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
   connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
   val code = connection.responseCode
   if (code in 300..399) throw SyncHttpException(code, "연동 서버의 접속 설정이 아직 완료되지 않았습니다. 웹 관리자에게 확인해주세요.")
   val stream = if (code in 200..299) connection.inputStream else connection.errorStream
   val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
   val json = try { JSONObject(text) } catch (_: Exception) { throw SyncHttpException(code, "연동 서버에 접속할 수 없습니다. 사이트 공개 접속 설정을 확인해주세요.") }
   if (code !in 200..299) throw SyncHttpException(code, json.optString("error", "동기화에 실패했습니다. 잠시 후 다시 시도해주세요."))
   json
  } finally { connection.disconnect() }
 }
}
