# AutoShot

홈 화면 아이콘을 탭하면 미리보기 없이 사진 1장을 자동 촬영하는 Android 앱. 대상 기기: Galaxy S23 Ultra.

- 사양서: [docs/SPEC.md](docs/SPEC.md)
- 진행 단계: **M0** (프로젝트 생성, 투명 Activity 실행 확인)

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
