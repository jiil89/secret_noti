# 시크릿노티 (secret_noti)

카카오톡·문자·Teams 알림을 폰 안에 저장하고, 화면 가장자리 엣지패널로 보여주는 개인용 안드로이드 앱.
대화방을 열지 않으므로 상대에게 읽음 표시가 남지 않는다. 자세한 내용은 [SPEC.md](SPEC.md).

## 빌드

필요: JDK 21, Android SDK (platform 37, build-tools 37). `local.properties`에 `sdk.dir`을 적는다.

```
./gradlew assembleRelease testDebugUnitTest
```

- 실사용: `app/build/outputs/apk/release/app-release.apk` (R8 최적화, 릴리스 키 서명)
- 개발용: `assembleDebug` → `com.jiil.secretnoti.dev` "시크릿노티 dev"로 따로 설치됨

릴리스 서명: `local.properties`에 `secretnoti.signing=<서명 정보 파일 경로>`를 적는다.
그 파일에는 `storeFile`, `storePassword`, `keyAlias`, `keyPassword`가 들어 있다. 키와 비밀번호는 저장소에 올리지 않는다.
**키를 잃어버리면 기존 앱 위에 업데이트를 설치할 수 없으니 따로 백업해 둔다.**

## 설치 (갤럭시)

1. 설정 → 휴대전화 정보 → 소프트웨어 정보 → 빌드번호 7번 탭 → 개발자 옵션 → USB 디버깅 켜기
2. 설정 → 보안 및 개인정보 보호 → **자동 차단(Auto Blocker) 끄기**
3. `adb install -r app/build/outputs/apk/release/app-release.apk`

## 진행 상황

- [x] M1 수집·진단: 알림 리스너, 권한 화면, extras 덤프·JSON 내보내기, 손잡이 제스처 시험
- [x] M2 저장: 파서, Room, 방 단위 묶기, 중복 제거 (삭제 감지는 실제 샘플로 확인 필요)
- [x] M3 앱 UI: 대화방 목록·상세, 출처 앱 아이콘
- [x] M4 엣지패널: 탭으로 열기, 대화방 바로 열기
- [x] M5 설정: 대상 앱, 손잡이 위치·크기, 보관 기간
- [ ] 남은 것: 단톡·삭제·Teams·문자 샘플로 파서 보강, 사진 미리보기(2차)
