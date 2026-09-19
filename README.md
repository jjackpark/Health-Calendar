# 헬스 캘린더

휴대폰 중심의 개인 건강 기록 웹앱. 첫 화면은 오늘의 걸음과 기록, 하단 탭은 오늘 / 캘린더 / 통계 / 설정으로 나뉩니다.

## 구현된 기능
- 로그인 사용자별 D1 저장소 분리. 사용자 ID는 서버에서 인증 헤더로만 결정합니다.
- 걸음, 운동 시간, 수면, 음주 여부/종류/양/도수, 식단 메모 저장·수정·삭제.
- 날짜별 캘린더, 7일/월간 걸음 그래프, 목표 달성일, 평균 수면, 운동 합계, 음주 일수.
- 미기록과 명시적 0을 구분. 평균은 기록일만 사용합니다.
- 사용자별 걸음 목표, 홈 화면 추가용 웹 앱 매니페스트.
- 320px/390px 모바일 레이아웃 및 모달 입력.

## 현재 범위
삼성헬스 자동 동기화는 아직 구현되지 않았습니다. 화면에서 연결 전/준비 중을 표시하며 직접 입력만 저장합니다. 실제 건강 데이터를 가져왔다고 표시하지 않습니다.

갤럭시 핏 → 삼성헬스 → Health Connect → Android 연동 앱 → 사용자별 서버 저장 구조가 필요합니다. 후속 작업에는 Android Health Connect 읽기 권한, 하루 단위 aggregate(중복 걸음 방지), 사용자 연결 인증, 재전송 시 덮어쓰기, 권한 철회, 백그라운드 실행 제약 처리 및 실제 기기 검증이 포함됩니다. 현재 private Sites 배포는 외부 Android 동기화 엔드포인트 용도로 사용할 수 없습니다. 공개 앱 배포/인증 경로를 먼저 확정해야 합니다.

현재 호스팅 로그인은 ChatGPT 계정 기반이며 첫 배포는 소유자 전용입니다. 사용자별 데이터 분리는 구현되어 있지만, 여러 사람에게 개방하는 사이트 접근 설정은 별도입니다.

## 개발
Node 22.13 이상. `npm run install:ci`, `npm run dev`, `npm run build`.

DB 선언은 `.openai/hosting.json`, 스키마는 `db/schema.ts`, 마이그레이션은 `drizzle/`에 있습니다. `npm run db:generate`로 변경을 생성합니다. 로컬 실행 전에 빌드 후 다음을 한 번 적용합니다.

```
node --import ./scripts/sites-env.mjs ./node_modules/wrangler/bin/wrangler.js d1 execute DB --local --config dist/server/wrangler.json --persist-to .wrangler/state --file drizzle/0000_mighty_colossus.sql
```

로컬에서 `/signin-with-chatgpt?return_to=/`는 스타터의 테스트 사용자로 로그인합니다. 이 모의 인증은 운영 빌드에 포함되지 않습니다.

## 검증
- `node node_modules/typescript/bin/tsc --noEmit`
- `node --experimental-strip-types --test tests/health.test.mts`
- dev 서버 실행 후 `node tests/api-smoke.mjs` (로컬 2001-01-02 테스트 기록 생성/정리)
- UI: 390px 입력·저장·통계, 320px 가로 넘침, WebMCP 유효/잘못된 날짜 검증.

브라우저 건강 정보나 세션 응답을 service worker에 캐싱하지 않습니다. 오프라인 저장은 지원하지 않습니다.
