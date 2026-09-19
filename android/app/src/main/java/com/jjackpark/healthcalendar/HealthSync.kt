package com.jjackpark.healthcalendar

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object HealthSync {
 val readPermission = HealthPermission.getReadPermission(StepsRecord::class)
 const val backgroundPermission = "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"
 private val mutex = Mutex()
 fun available(context: Context) = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
 fun backgroundAvailable(client: HealthConnectClient) = client.features.getFeatureStatus(HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND) == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
 suspend fun sync(context: Context, inBackground: Boolean): String = mutex.withLock {
  val vault = TokenVault(context)
  val token = vault.token() ?: throw IllegalStateException("웹에서 연결 코드를 발급받아 휴대폰을 연결해주세요.")
  check(available(context)) { "헬스 커넥트를 설치하거나 업데이트해주세요." }
  val client = HealthConnectClient.getOrCreate(context)
  val permissions = client.permissionController.getGrantedPermissions()
  check(readPermission in permissions) { "걸음 수 읽기 권한이 필요합니다. 권한 허용 버튼을 눌러주세요." }
  val canBackground = backgroundAvailable(client) && backgroundPermission in permissions
  if (inBackground) check(canBackground) { "백그라운드 읽기 권한이 없습니다. 앱을 열면 동기화됩니다." }
  val observed = Instant.now()
  val zone = ZoneId.systemDefault()
  val today = LocalDate.now(zone)
  val days = JSONArray()
  var todaySteps: Long? = null
  var recordedDays = 0
  for (ago in 29 downTo 0) {
   val date = today.minusDays(ago.toLong())
   val start = date.atStartOfDay(zone).toInstant()
   val next = date.plusDays(1).atStartOfDay(zone).toInstant()
   val end = if (next.isAfter(observed)) observed else next
   if (!end.isAfter(start)) continue
   val aggregate = client.aggregate(AggregateRequest(metrics = setOf(StepsRecord.COUNT_TOTAL), timeRangeFilter = TimeRangeFilter.between(start, end), dataOriginFilter = setOf(DataOrigin("com.sec.android.app.shealth"))))
   val count = aggregate[StepsRecord.COUNT_TOTAL]
   if (count != null) recordedDays++
   if (ago == 0) todaySteps = count
   days.put(JSONObject().put("date", date.toString()).put("steps", count ?: JSONObject.NULL))
  }
  val body = JSONObject().put("observedAt", observed.toEpochMilli()).put("timezone", zone.id).put("background", canBackground).put("days", days)
  try { CalendarApi.post("/api/device/sync", body, token) } catch (e: SyncHttpException) {
   if (e.status == 401) { vault.clear(); SyncWorker.cancel(context) }
   throw e
  }
  vault.synced()
  val result = if (recordedDays == 0) "삼성헬스 걸음 기록이 없습니다. 삼성헬스 설정 → 헬스 커넥트에서 걸음 쓰기를 허용한 뒤 삼성헬스를 열어 동기화해주세요." else "최근 30일 중 ${recordedDays}일을 동기화했습니다. 오늘: ${todaySteps?.let { "%,d보".format(it) } ?: "기록 없음"}"
  vault.status(result)
  result
 }
}
