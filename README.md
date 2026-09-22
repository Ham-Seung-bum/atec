# AutoShot

홈 화면 아이콘을 탭하면 미리보기 없이 사진 1장을 자동 촬영하는 Android 앱. 대상 기기: Galaxy S23 Ultra.

- 사양서: [docs/SPEC.md](docs/SPEC.md)
- 진행 단계: **M1** (카메라 권한, 미리보기 없이 카메라 열기, AE/AF 대기). 촬영·저장은 M2

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
