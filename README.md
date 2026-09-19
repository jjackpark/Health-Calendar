# 헬스 캘린더

모바일 웹 캘린더 + 삼성헬스 걸음 자동 연동 Android 앱.

## 웹 기능
- 오늘 / 캘린더 / 통계 / 설정, 320px 이상의 모바일 화면 지원.
- ChatGPT 로그인 계정별 D1 저장소 분리. 사용자 ID는 서버에서만 결정.
- 걸음·운동·수면·음주·식단 기록 저장/수정/삭제.
- 일별 걸음 그래프, 7일/월간 통계, 목표 달성일, 평균 수면, 운동 합계, 음주 일수.
- 미기록은 평균에서 제외하며 명시적인 0과 구분.
- 휴대폰 연결 코드 발급, 최근 동기화 상태, 연결 해제.

## 삼성헬스 자동 연동
`android/`에 Health Connect를 사용하는 네이티브 Android 앱이 있습니다.

1. 웹 설정 → 삼성헬스 연결에서 APK를 다운로드해 설치합니다.
2. 웹에서 10분짜리 일회성 연결 코드를 발급하고 앱에 붙여넣습니다.
3. 삼성헬스 → 설정 → 헬스 커넥트에서 걸음 쓰기를 허용합니다.
4. 연동 앱에서 걸음 읽기와, 지원되는 경우 백그라운드 읽기를 허용합니다.
5. 지금 동기화로 처음 확인합니다. 이후 약 1시간 간격으로 동기화를 요청합니다.

Android 9 이상에서 Health Connect를 사용할 수 있습니다. Android 13 이하에서는 Health Connect 별도 설치가 필요할 수 있습니다. 백그라운드 읽기 기능과 권한을 지원하지 않으면 앱을 열 때 동기화합니다. OS 절전 정책, 네트워크, 삼성헬스 반영 시점에 따라 자동 동기화가 지연될 수 있습니다.

삼성헬스 출처(`com.sec.android.app.shealth`)만 필터링하고 날짜별 aggregate를 사용합니다. 최근 30일의 날짜별 합계를 전송합니다. 재전송은 더하지 않고 갱신하며, 오래된 스냅샷은 무시합니다. 원본 삭제(null)는 연동 기록만 제거하며 직접 입력한 기록은 유지됩니다. 직접 입력한 걸음이 있으면 그 날짜는 수동 값이 우선합니다. 연동 값으로 바꾸려면 수동 걸음 기록을 삭제한 뒤 재동기화합니다.

## 접속 설정 — 출시 전 필요한 단계
최초 배포는 소유자 전용입니다. 독립 Android 앱의 `/api/device/pair`, `/api/device/sync`, `/api/device/revoke` 호출은 웹 브라우저의 로그인 쿠키가 없으므로 사이트 입구가 공개 접속이어야 합니다. 공개 범위 변경은 사용자 승인을 받은 뒤 Sites 접근 정책에서 수행해야 합니다. **아직 공개 접속으로 바꾸지 않았다면 휴대폰 동기화는 서버 입구에서 차단됩니다.**

사이트 입구를 공개해도 `/api/records`, `/api/preferences`, `/api/device`는 로그인한 사용자 본인의 데이터만 처리합니다. Android 토큰은 일별 걸음 전송/연결 해제 전용이며 기록 조회 권한이 없습니다. 서버는 토큰/코드의 SHA-256 해시만 저장하고, Android는 Keystore AES-GCM으로 토큰을 암호화하며 앱 백업을 허용하지 않습니다. 연결 해제 시 서버 토큰이 삭제됩니다.

## APK
- `public/downloads/health-calendar-sync.apk`: 서명된 1.0.0 설치본 (디버그 모드 비활성).
- `.sha256` 파일로 설치본 해시를 확인할 수 있습니다.
- 서명 키와 암호는 저장소에 포함하지 않습니다. 후속 업데이트는 같은 개인 키를 사용해야 합니다.
- Google Play 배포는 별도입니다. 실제 삼성헬스/휴대폰 데이터 읽기는 사용자의 설치·권한 허용 후 확인해야 합니다.

## 개발 및 검사
웹: Node 22.13 이상, `npm run install:ci`, `npm run dev`, `npm run build`.

DB: `db/schema.ts`, `drizzle/`. 스키마 변경 후 `npm run db:generate`. 로컬 빌드 후 아직 적용되지 않은 SQL을 순서대로 적용합니다.

```
node --import ./scripts/sites-env.mjs ./node_modules/wrangler/bin/wrangler.js d1 execute DB --local --config dist/server/wrangler.json --persist-to .wrangler/state --file drizzle/0001_nice_lizard.sql
```

Android: JDK 17+, Gradle wrapper 9.4.1, AGP 9.2.1, SDK 36.1, Build Tools 36.0.0. `android/local.properties`에 SDK 위치를 설정한 후 `./gradlew assembleDebug lintDebug`.

서명 APK: `CALENDAR_STORE_FILE`, `CALENDAR_STORE_PASSWORD` 환경 변수에 개인 PKCS12 키를 설정하고 `./gradlew assembleRelease`. 키 별칭은 `health-calendar`입니다. 비밀값을 커밋하지 마세요.

검사:
- `node node_modules/typescript/bin/tsc --noEmit`
- `node --experimental-strip-types --test tests/health.test.mts`
- dev 서버에서 `node tests/api-smoke.mjs`
- dev 서버에서 `node tests/device-sync-smoke.mjs` (테스트 사용자, 최근 29/28일 전이 비어 있을 때만 실행; 완료 후 정리)

검증된 범위: APK 컴파일/서명 및 Android lint, 웹 타입/빌드, 기록 CRUD, 일회성 연결, 토큰 권한 제한, 중복 방지, 오래된 스냅샷 차단, 수동 기록 보존, 연결 해제, 모바일 웹 연결 UI.

미검증 범위: 실제 갤럭시의 삼성헬스 읽기/권한 화면, OS 백그라운드 예약 실행, 휴대폰→운영 서버 전송. 최종 기기 테스트가 필요합니다.
