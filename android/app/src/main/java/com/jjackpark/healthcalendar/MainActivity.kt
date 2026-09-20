package com.jjackpark.healthcalendar

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
 private lateinit var vault: TokenVault
 private lateinit var content: LinearLayout
 private lateinit var codeInput: EditText
 private lateinit var status: TextView
 private lateinit var connection: TextView
 private lateinit var background: TextView
 private lateinit var lastSync: TextView
 private lateinit var interval: Spinner
 private val actionButtons = mutableListOf<Button>()
 private var running = false
 private val permissions = registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
  if (HealthSync.readPermission in granted) { runTask { configureSchedule(); HealthSync.sync(this@MainActivity, false) } }
  else { status.text = "권한은 언제든 다시 허용할 수 있어요. 웹에서 직접 기록하는 기능은 계속 사용할 수 있습니다." }
 }
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  vault = TokenVault(this)
  window.decorView.setOnApplyWindowInsetsListener { view, insets ->
   view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom); insets
  }
  val scroll = ScrollView(this).apply { setBackgroundColor(Color.parseColor("#F5F7F8")); isFillViewport = true }
  content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(24), dp(24), dp(30)) }
  scroll.addView(content); setContentView(scroll)
  text("헬스 캘린더", 16, "#176650", true)
  text("삼성헬스와 연결하기", 26, "#172B28", true, 14)
  text("한 번 연결하면, 걸음은 자동으로.", 16, "#657773", false, 8)
  connection = text("연결 상태 확인 중", 17, "#176650", true, 26)
  status = text("", 15, "#496353", false, 8)
  lastSync = text("", 14, "#657773", false, 8)
  text("1. 웹에서 연결 코드 받기", 18, "#172B28", true, 28)
  text("웹 캘린더 → 설정 → 삼성헬스 연결에서 코드를 발급받아 아래에 붙여넣으세요.", 15, "#657773", false, 8)
  button("웹 캘린더 열기") { openUrl(BuildConfig.SITE_URL) }
  codeInput = EditText(this).apply {
   hint = "연결 코드 붙여넣기"; textSize = 17f; setSingleLine(true)
   inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
   setAutofillHints(null); importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
   background = rounded("#FFFFFF"); setPadding(dp(15), dp(12), dp(15), dp(12))
  }
  content.addView(codeInput, LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(14) })
  button("이 휴대폰 연결", true) {
   val code = codeInput.text.toString().replace(Regex("[\\s-]"), "").lowercase()
   if (!code.matches(Regex("[a-f0-9]{24}"))) { codeInput.error = "웹에서 받은 24자리 코드를 붙여넣어주세요."; return@button }
   runTask {
    val result = CalendarApi.post("/api/device/pair", JSONObject().put("code", code).put("name", "${Build.MANUFACTURER} ${Build.MODEL}".take(80)))
    vault.save(result.getString("token")); codeInput.text.clear()
    "휴대폰이 연결됐습니다. 아래에서 걸음 수 읽기 권한을 허용해주세요."
   }
  }
  text("2. 걸음 수 접근 허용", 18, "#172B28", true, 28)
  text("삼성헬스의 걸음 수만 읽어 내 웹 캘린더로 전송합니다. 삼성헬스 설정에서도 헬스 커넥트의 걸음 쓰기를 허용해주세요.", 15, "#657773", false, 8)
  button("걸음 수 권한 허용") { requestHealthPermissions(false) }
  button("삼성헬스 열기") {
   val intent = packageManager.getLaunchIntentForPackage("com.sec.android.app.shealth")
   if (intent != null) startActivity(intent) else openUrl("https://play.google.com/store/apps/details?id=com.sec.android.app.shealth")
  }
  text("3. 자동 동기화", 18, "#172B28", true, 28)
  background = text("", 15, "#657773", false, 8)
  button("백그라운드 접근 허용") { requestHealthPermissions(true) }
  text("동기화 간격", 15, "#496353", true, 16)
  val intervalLabels = listOf("15분마다 · 배터리 사용 증가", "1시간마다 · 권장", "3시간마다", "6시간마다", "수동으로만")
  val intervalValues = intArrayOf(15, 60, 180, 360, 0)
  interval = Spinner(this).apply {
   adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, intervalLabels)
   setSelection(intervalValues.indexOf(vault.syncMinutes()).coerceAtLeast(0), false)
   onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
    private var initialized = false
    override fun onNothingSelected(parent: AdapterView<*>?) = Unit
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
     if (!initialized) { initialized = true; return }
     val value = intervalValues[position]; vault.syncMinutes(value)
     lifecycleScope.launch { configureSchedule(); refresh() }
     status.text = if (value == 0) "자동 동기화를 껐습니다. ‘지금 동기화’로 가져올 수 있어요." else "자동 동기화 간격을 ${intervalLabels[position].substringBefore('·').trim()}로 변경했습니다."
    }
   }
  }
  content.addView(interval, LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(8) })
  button("지금 동기화", true) { runTask { configureSchedule(); HealthSync.sync(this@MainActivity, false) } }
  text("15분은 Android가 허용하는 가장 짧은 안정적 주기이며 배터리를 더 사용합니다. 절전 모드와 삼성헬스 반영 시점에 따라 늦어질 수 있습니다. 웹에서 직접 입력한 걸음은 유지됩니다.", 14, "#657773", false, 14)
  button("헬스 커넥트 설정") {
   try { startActivity(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)) }
   catch (_: Exception) { openUrl("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata") }
  }
  button("연결 해제") { AlertDialog.Builder(this).setTitle("휴대폰 연결을 해제할까요?").setMessage("이 휴대폰의 자동 동기화가 중지됩니다. 웹에 저장된 기록은 유지됩니다.").setNegativeButton("취소", null).setPositiveButton("연결 해제") { _, _ ->
   runTask {
    val token = vault.token(); var remote = true
    if (token != null) try { CalendarApi.post("/api/device/revoke", JSONObject(), token) } catch (_: Exception) { remote = false }
    SyncWorker.cancel(this@MainActivity); vault.clear()
    if (remote) "연결을 해제했습니다." else "휴대폰 동기화를 중지했습니다. 웹 설정에서도 연결을 해제해주세요."
   }
  }.show() }
  button("개인정보 안내") { startActivity(Intent(this, PrivacyActivity::class.java)) }
  refresh()
 }
 override fun onStart() { super.onStart(); if (::vault.isInitialized && vault.token() != null && HealthSync.available(this)) runTask { configureSchedule(); HealthSync.sync(this@MainActivity, false) } }
 private fun requestHealthPermissions(includeBackground: Boolean) {
  if (vault.token() == null) { status.text = "먼저 연결 코드로 휴대폰을 연결해주세요."; return }
  if (!HealthSync.available(this)) { openUrl("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata"); return }
  val requested = mutableSetOf(HealthSync.readPermission)
  if (includeBackground) {
   if (!HealthSync.backgroundAvailable(HealthConnectClient.getOrCreate(this))) { status.text = "이 휴대폰은 백그라운드 읽기를 지원하지 않습니다. 앱을 열 때마다 자동 동기화합니다."; return }
   requested.add(HealthSync.backgroundPermission)
  }
  permissions.launch(requested)
 }
 private suspend fun configureSchedule() {
  val client = HealthConnectClient.getOrCreate(this)
  val granted = client.permissionController.getGrantedPermissions()
  if (vault.token() != null && HealthSync.backgroundAvailable(client) && granted.containsAll(setOf(HealthSync.readPermission, HealthSync.backgroundPermission))) SyncWorker.schedule(this, vault.syncMinutes()) else SyncWorker.cancel(this)
 }
 private fun runTask(block: suspend () -> String) {
  if (running) return
  running = true; actionButtons.forEach { it.isEnabled = false }; status.text = "처리 중…"
  lifecycleScope.launch {
   try { val message = block(); vault.status(message); status.text = message }
   catch (e: CancellationException) { throw e }
   catch (e: Exception) { val message = e.message ?: "연결하지 못했습니다. 잠시 후 다시 시도해주세요."; vault.status(message); status.text = message }
   finally { running = false; actionButtons.forEach { it.isEnabled = true }; refresh() }
  }
 }
 private fun refresh() {
  connection.text = if (vault.token() != null) "내 휴대폰 연결됨" else "아직 연결되지 않았어요"
  status.text = vault.status()
  lastSync.text = if (vault.lastSync() > 0) "마지막 동기화 · " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(vault.lastSync())) else "동기화 기록 없음"
  if (!HealthSync.available(this)) { background.text = "헬스 커넥트 설치 또는 업데이트가 필요합니다."; return }
  lifecycleScope.launch {
   try { val client = HealthConnectClient.getOrCreate(this@MainActivity); val granted = client.permissionController.getGrantedPermissions()
    background.text = if (!HealthSync.backgroundAvailable(client)) "이 휴대폰은 앱을 열 때 자동으로 동기화됩니다." else if (HealthSync.backgroundPermission in granted) { val minutes=vault.syncMinutes(); if(minutes==0) "백그라운드 권한이 허용되어 있으며 자동 동기화는 꺼져 있습니다." else "백그라운드 동기화 권한이 허용되었습니다. 현재 간격: ${if(minutes<60) "${minutes}분" else "${minutes/60}시간"}" } else "앱을 닫아도 동기화하려면 백그라운드 접근을 허용해주세요."
   } catch (_: Exception) { background.text = "헬스 커넥트 권한을 확인해주세요." }
  }
 }
 private fun openUrl(url: String) { try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Exception) { status.text = "웹 브라우저를 열 수 없습니다." } }
 private fun text(value: String, size: Int, color: String, bold: Boolean, margin: Int = 0): TextView = TextView(this).apply {
  text = value; textSize = size.toFloat(); setTextColor(Color.parseColor(color)); if (bold) setTypeface(typeface, Typeface.BOLD); setLineSpacing(dp(3).toFloat(), 1f)
  content.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(margin) })
 }
 private fun button(label: String, primary: Boolean = false, action: () -> Unit) {
  val button = Button(this).apply { text = label; textSize = 16f; isAllCaps = false; minHeight = dp(52); setTextColor(Color.parseColor(if (primary) "#FFFFFF" else "#176650")); background = rounded(if (primary) "#176650" else "#E7F0EB"); setOnClickListener { action() } }
  actionButtons.add(button); content.addView(button, LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) })
 }
 private fun rounded(color: String) = GradientDrawable().apply { setColor(Color.parseColor(color)); cornerRadius = dp(15).toFloat() }
 private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
