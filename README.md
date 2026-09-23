# AutoShot

홈 화면 아이콘을 탭하면 미리보기 없이 사진 1장을 자동 촬영하는 Android 앱. 대상 기기: Galaxy S23 Ultra.

- 사양서: [docs/SPEC.md](docs/SPEC.md)
- 진행 단계: **M5 완료** (release 빌드 정리, 버섯 아이콘). versionName 1.0.0
- 앱 표시 이름: **버섯** / 플래시: 강제 OFF / 셔터음: 기본 무음(실기기 확인됨)

## 빌드

요구사항: JDK 17+, Android SDK (API 36). Android Studio 최신 안정판으로 폴더를 열면 된다.

```bash
./gradlew assembleDebug     # app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug      # USB 디버깅으로 연결된 기기에 설치
```

GitHub Actions(`Android CI`)가 push마다 debug APK를 빌드해 `autoshot-debug-apk` 아티팩트로 올린다.

## 실기기(S23 Ultra) 설치

1. `설정 > 휴대전화 정보 > 소프트웨어 정보 > 빌드번호` 7회 탭 → 개발자 옵션 활성화
2. `설정 > 개발자 옵션 > USB 디버깅` ON, PC 연결 후 "USB 디버깅 허용"
3. `adb install -r app-debug.apk` 또는 `./gradlew installDebug`

### "앱이 설치되지 않음"이 뜰 때

1. 기존 AutoShot을 먼저 삭제하고 다시 설치한다. 서명이 다른 이전 APK가 남아 있으면 업데이트가 거부된다.
   (`9a2ba9c` 이전 빌드는 CI마다 debug 키가 달랐다. 이후 빌드는 `app/debug.keystore`로 서명이 고정되어 삭제 없이 업데이트된다.)
2. 다운로드한 `autoshot-debug-apk.zip`의 압축을 풀고 안의 `app-debug.apk`를 설치한다.
3. `설정 > 보안 및 개인정보 보호 > 자동 차단`이 켜져 있으면 외부 APK 설치가 막힌다. 설치하는 동안 끈다.
4. 그래도 안 되면 PC에서 `adb install -r app-debug.apk`를 실행해 `INSTALL_FAILED_…` 오류 코드를 확인한다.

## M0 확인 항목

- [ ] 홈 화면에 AutoShot 아이콘이 생긴다
- [ ] 아이콘 탭 시 앱 화면(흰 화면·스플래시)이 보이지 않고 "AutoShot 실행됨 (M0)" 토스트만 뜬다
- [ ] 최근 앱 목록에 AutoShot이 남지 않는다
- [ ] `adb logcat -s AutoShot`에 `launched` 로그가 찍힌다

## M1 확인 항목

아이콘 탭 → (최초 1회 권한 요청) → 약 0.5~1초 뒤 `카메라 준비 완료 · 초점 성공 · 812ms · 4000x3000` 형식의 토스트 → 종료. M1은 아직 사진을 저장하지 않는다.

- [ ] 최초 실행: 홈 화면 위에 시스템 카메라 권한 다이얼로그만 뜬다 (TC-15)
- [ ] 거부 1회: "카메라 권한이 없어…" 토스트 후 종료 (TC-02)
- [ ] 거부 2회("다시 묻지 않음"): "설정 열기" 다이얼로그 → 앱 권한 화면으로 이동 (TC-03)
- [ ] 허용 후: 초록 점(카메라 사용 표시)이 잠깐 뜨고, 미리보기 없이 준비 토스트가 뜬다
- [ ] 삼성 카메라 앱을 분할 화면으로 켠 상태: "카메라를 열 수 없습니다" 토스트, 5초 안에 종료 (TC-06)

측정값 수집 (N-01, 해상도 확인):

```bash
adb logcat -s AutoShot
# launched sinceProcessStart=…ms
# camera ready sinceLaunch=…ms bind=…ms metering=…ms focus=true resolution=4000x3000
```

콜드 스타트는 `adb shell am force-stop com.atec.autoshot` 후 아이콘 탭으로 측정한다.

## M2 확인 항목

아이콘 탭 → 셔터음·진동 → `사진 저장됨 · 초점 성공 · 1234ms` 토스트 → 종료.

- [ ] 갤러리 앱에 `AutoShot` 앨범이 생기고 사진이 저장된다 (TC-08)
- [ ] 파일명이 `AUTOSHOT_yyyyMMdd_HHmmss_SSS.jpg` 형식이다
- [ ] 아이콘 10회 탭 → 10장 저장 (TC-04)
- [ ] 토스트의 초점 결과가 "초점 성공"인지 확인 (M1의 "초점 대기 시간 초과" 수정 확인)
- [ ] 저조도(실내 소등)에서 노출이 확보된다 (TC-07)

```bash
adb logcat -s AutoShot
# camera ready sinceLaunch=…ms open=…ms metering=…ms focus=FOCUSED resolution=…
# shutter sinceLaunch=…ms      ← N-01/N-02 측정값 (아이콘 탭 → 셔터)
# saved sinceLaunch=…ms uri=content://media/…
```

## M3 확인 항목

- [ ] 셔터음: 기본값(무음)에서 아이콘 탭 시 우리 앱 소리가 나지 않는다. 그래도 소리가 나면 기기 펌웨어 강제이며(순정 카메라도 무음이 안 되는지 비교), 앱에서 끌 수 없다 (TC-12)
- [ ] 기기를 가로로 들고 촬영 → 갤러리에서 사진이 올바른 방향으로 보인다 (TC-09)
- [ ] 아이콘을 빠르게 2회 연속 탭 → 1장만 저장된다 (TC-05)
- [ ] 저장 공간이 부족한 상태 → "사진을 저장하지 못했습니다" 토스트, 크래시 없음 (TC-11)

> 셔터음을 켜려면 `AppSettings.PLAY_SHUTTER_SOUND = true`로 바꿔 다시 빌드한다.

## M4 확인 항목

- [ ] 홈 화면 아이콘 이름이 **버섯**으로 보인다
- [ ] 촬영 완료 토스트에 "사진" 문구가 없다 (`저장됨 · …`)
- [ ] 어두운 곳에서도 플래시가 켜지지 않는다
- [ ] N-01/N-02 지연 측정: `adb logcat -s AutoShot`의 `shutter sinceLaunch` 10회 평균 (콜드/웜)

### 지연 측정 스크립트

```bash
adb logcat -c
for i in $(seq 1 10); do
  adb shell am force-stop com.atec.autoshot     # 콜드 스타트. 웜은 이 줄 제거
  adb shell monkey -p com.atec.autoshot -c android.intent.category.LAUNCHER 1 >/dev/null
  sleep 3
done
adb logcat -d -s AutoShot | grep "shutter sinceLaunch"
```

계측 테스트(`assembleDebugAndroidTest`로 컴파일 검증, 실행은 기기 필요):

```bash
./gradlew connectedDebugAndroidTest    # 기기/에뮬레이터 연결 시
```

## M5 (릴리스)

- 앱 아이콘: 버섯 모양(빨간 갓 + 흰 점 + 크림색 대) 적응형 아이콘
- `assembleRelease`로 서명된 release APK 생성. 개인 사용이라 공용 debug 키로 서명(Play 스토어 배포 시 전용 키로 교체)
- release는 R8 축소를 끔: CameraX 콜백이 런타임에 잘못 제거될 위험 회피, 앱이 작아 크기 영향 없음
- CI가 `autoshot-release-apk` 아티팩트로 release APK 업로드

release APK는 debug와 동일하게 설치한다. 두 빌드의 서명 키가 같아 서로 덮어써 설치된다.

```bash
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk
```
