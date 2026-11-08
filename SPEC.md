# 시크릿노티 — 개발 명세 (v2, 2026-11-07 개정)

> v1 명세를 검토하고 결정한 사항을 반영한 버전. 변경점은 맨 아래 "v1 대비 변경" 참고.

## 1. 한 줄 요약
카카오톡·문자·Microsoft Teams 알림을 OS 레벨에서 받아 저장하고, 화면 가장자리를 살짝 밀면 엣지패널로 최근 메시지를 보여주는 **안드로이드 앱**.
대화방을 직접 열지 않으므로 상대방에게 읽음 표시(카톡의 숫자 1, Teams의 '읽음')가 남지 않는다. NotiSave와 같은 원리.

- 용도: 개인/가족용. Play 스토어에 올리지 않고 직접 설치(sideload)
- 대상 기기: 갤럭시(One UI) 우선, 다른 안드로이드 폰에서도 동작
- iOS는 지원하지 않음 (iOS는 다른 앱의 알림을 읽을 수 없음)

## 2. 기술 스택
- Kotlin, Jetpack Compose, Material 3
- minSdk 26 / compileSdk·targetSdk 37
- AGP 9.5.1(내장 Kotlin), Kotlin 2.4.21, KSP 2.3.13, Gradle 9.8.2, Compose BOM 2026.09.00
- Room 2.8.5, Coroutines + Flow
- DI 없이 수동 주입 (`NotiApp.instance`)
- **INTERNET 권한 없음.** `allowBackup=false` + `dataExtractionRules`로 클라우드 백업·기기 이전도 막는다

## 3. 핵심 구성요소

### 3-1. 알림 수집: `NotificationListenerService`
- 대상 패키지 (설정에서 켜고 끄기)
  - 카카오톡 `com.kakao.talk`
  - 문자: `Telephony.Sms.getDefaultSmsPackage()` + 알려진 SMS 앱(삼성 메시지, Google 메시지)
  - Teams `com.microsoft.teams` (개인 프로필에 설치된 경우만. 업무 프로필 안의 Teams는 읽을 수 없음)
- 파싱 필드 (`Notification.extras`)
  - `EXTRA_TITLE`(보낸 사람), `EXTRA_TEXT`(내용), `EXTRA_BIG_TEXT`(긴 메시지 전체)
  - `EXTRA_SUB_TEXT` / `EXTRA_CONVERSATION_TITLE`(단톡방 이름), `EXTRA_IS_GROUP_CONVERSATION`
  - `EXTRA_MESSAGES`(MessagingStyle): 메시지별 `text`, `time`, `sender`/`sender_person`
  - 보낸 사람이 null인 MessagingStyle 메시지 = 내가 보낸 메시지("나")
- **앱별 파서**: 표준 MessagingStyle 기본 파서 + 카톡/Teams 전용 파서. M1 덤프로 확정
- **방 키(roomKey)**: `shortcutId` → `locusId` → 알림 tag → 방 이름 순으로 안정적인 값을 고른다 (M1 덤프로 확정)
- **방 단위 묶기**: 1:1이든 단톡이든 같은 roomKey의 메시지는 한 대화방으로 모은다. 알림이 여러 개로 나뉘어 와도 마찬가지
- **중복 제거**: (패키지, roomKey, 보낸 사람, 내용, MessagingStyle 메시지 시각). `postTime`은 갱신마다 바뀌므로 메시지 시각으로 쓰지 않는다
- 사진·이모티콘은 알림 텍스트("사진을 보냈습니다")만 저장
- 그룹 요약 알림(`FLAG_GROUP_SUMMARY`), "새 메시지 N개" 요약 텍스트는 무시
- Teams 채널 게시물은 "팀 > 채널"을 한 대화방으로 본다

### 3-2. 저장: Room DB
```
Conversation(id, source, packageName, roomKey, roomTitle, isGroup, lastMessageAt, unreadCount)
Message(id, conversationId, sender, text, postedAt, receivedAt, isDeletedBySender, originalText)
```
- `onNotificationRemoved`가 와도 지우지 않는다
- **삭제 감지(1차 포함)**: 알림이 갱신될 때 이전에 있던 메시지가 "삭제된 메시지입니다" 류로 바뀌거나 목록에서 빠지는 패턴으로 판단 → `isDeletedBySender=true`, 원문은 `originalText`에 보관. 알림이 단순히 사라지는 것(사용자가 지움, PC에서 읽음)은 삭제가 아님
- 보관 기간 설정 (기본 30일, 이후 자동 삭제)
- `unreadCount`는 이 앱 안에서 안 본 개수 (카톡/Teams의 읽음과 무관)
- 단톡방 인원수는 알림에 없으므로 표시하지 않는다

### 3-3. 엣지패널: 오버레이
- 권한 `SYSTEM_ALERT_WINDOW`, `TYPE_APPLICATION_OVERLAY`
- **오버레이를 알림 리스너 서비스 안에서 띄운다.** 리스너는 시스템이 바인딩해 유지하므로 포그라운드 서비스·상시 알림·부팅 자동 시작이 필요 없다 (M1에서 실기기 검증. 안 되면 `specialUse` 포그라운드 서비스로 전환)
- 손잡이: 터치 영역 44dp, 보이는 막대 8dp, 새 메시지 배지
- 제스처: 안쪽으로 40dp 이상 밀면 열림, **탭으로도 열림**(뒤로 가기 제스처와 충돌할 때의 대안). Android 10+ 시스템 제스처 예외 영역 적용(세로 최대 200dp)
- 패널: 폭 316dp, 상하 여백, 안쪽 모서리 28dp, 뒤 화면 반투명. 바깥 탭·뒤로 가기·가장자리 방향 스와이프로 닫힘
- Compose는 `ComposeView` + 직접 만든 `LifecycleOwner`/`SavedStateRegistryOwner`
- 손잡이 위치(좌/우, 세로) 설정. 왼쪽이면 패널 모서리도 좌우 반전
- 키보드 표시 중 숨김은 **가능하면 지원**(다른 앱의 키보드 감지가 확실하지 않음). 대신 "손잡이 잠시 숨기기" 제공
- 패널/상세에서 원래 앱 열기: 알림의 `contentIntent`는 **메모리에만** 보관(알림이 살아 있는 동안). 없으면 해당 앱만 실행

## 4. 화면
시안: https://claude.ai/artifact/GgYzK83RDEurA9pnCwVQiU (복사본: `docs/design/`)

1. 평소 화면(손잡이) + 첫 사용 툴팁 "살짝 밀어서 새 메시지 보기"
2. 엣지패널: "최근 메시지" + "읽음 표시 안 남음" 칩, 필터 칩(전체 / 카톡 / 문자 / Teams — 사용 중인 앱만), 대화방 리스트(최근순 최대 10개), "모든 기록 보기", 설정
3. 대화방 상세: 뒤로가기, 방 이름, 출처("카카오톡" — 인원수 없음), 안내 배너, 말풍선·날짜 구분선, 삭제된 메시지(점선 + "삭제된 메시지 · 원문 보관됨"), 하단 "답장하려면 {앱}을 열어야 해요. 열면 읽음 처리돼요." + [{앱}에서 열기]
4. 메인: 전체 대화방 목록 (검색은 2차)
5. 설정: 대상 앱, 손잡이 위치/크기, 보관 기간, 권한 상태, 배터리 예외 안내
6. 온보딩: 알림 접근(+ "제한된 설정 허용" 안내) → 다른 앱 위에 표시 → 배터리 최적화 제외 → 카톡 "메시지 미리보기" 켜기 안내

디자인 토큰: Noto Sans KR 또는 Pretendard(앱에 포함), 텍스트 #16181D / 보조 #5B6270·#3B3F47, 포인트 #2558E8, 안내 칩 #E8EEFF / #1F4FD1, 배경 #FFFFFF / 상세 #F4F5F7, 구분선 #E6E8EC, 패널 모서리 28dp(안쪽), 말풍선 4/18/18/18dp, 이모지 대신 라인 아이콘.
UI 디자인 구현은 Codex(gpt-6-astra)에 맡긴다.

## 5. 갤럭시·안드로이드 주의사항
- 절전 예외 앱 등록 안내, `requestRebind`
- APK 파일로 설치하면 Android 13+ "제한된 설정"에 막힘 → 앱 정보 → ⋮ → 제한된 설정 허용
- USB 설치 전 갤럭시 "자동 차단(Auto Blocker)" 끄기
- 업데이트 설치를 위해 같은 서명 키 유지 (릴리스 keystore 하나를 만들어 보관, 저장소에는 올리지 않음)
- 카톡: 메시지 미리보기 꺼짐 / 알림 끈 채팅방 / PC 사용 중 모바일 알림 꺼짐 → 저장 불가 (안내)
- Teams: "데스크톱 활동 중 알림 차단" 설정 시 그동안 저장 불가 (안내)
- 알림을 시스템에서 '차단'하지 말고 '무음'으로 두면 방해 없이 수집 가능

## 6. 마일스톤
- **M1 수집·진단**: 리스너, 권한 온보딩, extras 덤프·JSON 내보내기, 손잡이 제스처 시험 ← 완료
- **M2 저장**: 파서, Room 저장, roomKey 묶기, 중복 제거, 삭제 감지 ← 완료
- **M3 앱 UI**: 대화방 목록·상세 ← 완료
- **M4 엣지패널**: 정식 손잡이·패널 ← 완료
- **M5 설정·안정화**: 대상 앱, 손잡이 위치·크기, 보관 기간, 배터리 예외 ← 완료
- **v0.3**: 지원 앱 확장, 앱 잠금, 삭제 메시지 알림, 검색·고정·숨기기·키워드, 사진 미리보기, 암호화 백업, 다크 모드, 위젯, 영어 ← 완료
- **v0.4**: 대화방별 저장 끄기·광고 표시, 엣지패널 밀어서 빼기·모두 지우기·폭 조절, 다른 앱에서 읽으면 정리, 새 메시지 표시(배지·반짝임·말풍선), 카톡 이모티콘·단톡방 표시 ← 완료
- **남은 것**: Teams·문자·다른 메신저 실제 샘플로 파서 확인, Play 스토어 준비

각 작업마다 빌드되는 상태로 커밋·푸시하고, 실기기 확인 항목을 정리한다.
새 화면·화면 변경은 시안을 먼저 보여주고 확인받은 뒤 만든다.
테스터 배포는 GitHub Releases(버전별 APK)로 한다.

## 7. 테스트
- 실기기(갤럭시) USB 설치
- 카톡 1:1·단톡·사진·이모티콘·삭제 / Teams 1:1·그룹·채널 / 문자를 보내고 저장 결과 확인
- 앱을 열지 않은 상태에서 상대 화면의 숫자 1(Teams '읽음')이 유지되는지 확인
- 파서는 덤프한 실제 extras 샘플로 단위 테스트

## v1 대비 변경
- 대상에 Teams 추가, 앱별 파서 구조
- `contentIntent`는 DB가 아니라 메모리에 보관
- `isRemovedFromTray` → `isDeletedBySender` + `originalText`, 삭제 표시는 1차 포함
- 단톡방 인원수 표시 제거
- roomKey 규칙과 방 단위 묶기 명시, `EXTRA_BIG_TEXT`·내 메시지 처리 추가
- 오버레이를 리스너 서비스 안에서 호스팅 (포그라운드 서비스·부팅 수신 불필요할 가능성)
- 탭으로 열기 추가, 키보드 감지는 "가능하면"
- `allowBackup=false`, 제한된 설정·Auto Blocker·서명 키 안내

## v0.3~v0.4에서 정한 것 (실기기 확인 결과 포함)
- **카톡 알림 구조**: MessagingStyle, `shortcutId` = 채팅방 번호(방 키), 메시지마다 `chatLogId`(중복 제거 키). 알림 하나에 최근 몇 개만 담고 오래된 것부터 빠진다
- **삭제 감지**: 같은 알림의 메시지 목록 "중간"에서 빠진 것만 삭제로 본다 (앞쪽에서 빠지는 건 정상). "삭제된 메시지입니다" 류 문구로 바뀌는 경우도 처리
- **카톡 단톡방 이름**: 알림에는 없다(title은 마지막 보낸 사람). `Ranking.getConversationShortcutInfo()`의 이름으로 얻는다. 못 얻으면 기존 방 이름을 보낸 사람 이름으로 덮어쓰지 않는다. 보낸 사람이 2명 이상이면 단톡으로 판정
- **사진·이모티콘**: 카톡은 사진 미리보기 주소(content://)를 알림에 넣고 리스너가 읽을 수 있다. 큰 이모티콘은 MIME 없이 주소만 온다. 미니 이모티콘은 "(이름)" 글자로만 온다
- **다른 앱에서 읽음**: 원래 앱이 알림을 지우거나(REASON_APP_CANCEL) 알림을 눌러 열면(REASON_CLICK) 읽음으로 본다. 사용자가 밀어서 지운 것은 제외
- **배지**: 마지막으로 패널을 연 뒤 온 메시지 중 그 방을 그 뒤에 읽지 않은 것만 센다(`readAt`). "삭제 N"은 앱에서 열어 본 뒤(`seenAt`) 새로 삭제된 것만
- **오버레이 주의**: 다른 앱 위 창에는 Activity가 없어 Dialog를 띄울 수 없다 → 엣지패널 확인창은 패널 안 카드로
- **배포**: R8 릴리스 빌드, 저장소 밖 키로 서명, GitHub Releases. 개발용은 `.dev` 별도 앱

