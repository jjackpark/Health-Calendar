package com.jjackpark.healthcalendar
import android.app.Activity
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
class PrivacyActivity : Activity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val padding = (24 * resources.displayMetrics.density).toInt()
  val text = TextView(this).apply { textSize = 17f; setPadding(padding, padding*2, padding, padding)
   text = "헬스 캘린더 · 건강 데이터 안내\n\n이 앱은 사용자가 허용한 삼성헬스의 일별 걸음 수를 헬스 커넥트에서 읽습니다. 최근 30일의 날짜별 걸음 수, 시간대, 동기화 시각, 휴대폰 이름 및 백그라운드 권한 상태를 연결된 헬스 캘린더 계정으로 전송합니다.\n\n전송 대상: ${BuildConfig.SITE_URL}\n\n연결 코드를 발급한 계정에만 저장하며 광고나 판매에 사용하지 않습니다. 심박수, 위치, 연락처는 읽지 않습니다. 삼성헬스에 데이터를 쓰거나 수정하지 않습니다.\n\n걸음 읽기와 백그라운드 읽기는 선택한 뒤 언제든 헬스 커넥트 설정에서 철회할 수 있습니다. 권한을 철회하면 자동 동기화가 중지되며 웹의 직접 기록 기능은 계속 사용할 수 있습니다.\n\n휴대폰 연결 정보는 Android Keystore로 암호화해 저장하며 앱 백업에 포함하지 않습니다. 서버에는 연결 토큰의 해시만 저장합니다.\n\n연결 해제는 앱 또는 웹 설정에서 할 수 있습니다. 이미 저장된 기록은 자동 삭제되지 않으며 웹 캘린더에서 해당 날짜의 걸음 기록을 삭제할 수 있습니다. 원본을 헬스 커넥트에서 지우면 다음 동기화 때 최근 30일의 해당 연동 기록도 제거합니다. 웹에서 직접 입력한 기록은 보존합니다.\n\n백그라운드 기능을 지원하지 않는 휴대폰은 앱을 열 때 동기화합니다. 동기화 시간은 OS 절전 정책과 삼성헬스의 데이터 반영 시점에 따라 달라질 수 있습니다.\n\n사용 문의: 웹앱을 제공한 운영자에게 문의해주세요."
  }
  setContentView(ScrollView(this).apply { addView(text) })
 }
}
