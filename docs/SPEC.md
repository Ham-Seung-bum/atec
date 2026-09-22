# AutoShot 개발사양서 (v0.1)

> 바탕화면 아이콘을 누르면 별도 조작 없이 사진 한 장을 자동으로 찍어 저장하는 Android 앱

| 항목 | 내용 |
|---|---|
| 문서 버전 | v0.1 (초안) |
| 작성일 | 2026-09-22 |
| 대상 기기 | Samsung Galaxy S23 Ultra (SM-S918N, 국내판 기준) |
| 상태 | 사용자 확인 대기 (§12 미결 사항) |

---

## 1. 목표와 범위

### 1.1 목표
사용자가 홈 화면(런처)의 **AutoShot 아이콘을 탭하면**, 셔터 버튼을 누르지 않아도 **후면 메인 카메라로 사진 1장이 자동 촬영되어 갤러리에 저장**되고 앱은 스스로 종료된다.

### 1.2 범위 (v1.0)
- 포함: 런처 아이콘 실행 → 자동 촬영 → 저장 → 종료, 최초 실행 시 권한 요청, 촬영 피드백(셔터음·진동·토스트)
- 제외: 동영상, 연속 촬영, 필터/편집, 클라우드 업로드, 백그라운드/화면 꺼짐 상태 촬영, 위젯·Quick Settings 타일(§11 확장 후보)

---

## 2. 대상 환경

| 항목 | 값 | 비고 |
|---|---|---|
| 기기 | Galaxy S23 Ultra | Snapdragon 8 Gen 2, 후면 메인 200MP |
| OS | One UI 7 이상 (Android 15+) | 실제 기기 OS 버전은 개발 착수 시 `설정 > 휴대전화 정보`에서 확인 |
| minSdk | 29 (Android 10) | Scoped Storage 기준으로 저장 로직 단순화. 단일 기기 대상이라 하위 호환 불필요 |
| targetSdk / compileSdk | 착수 시점 최신 안정 버전 | Google Play 요구사항과 무관하게 최신 권장 |

---

## 3. 핵심 사용자 시나리오

```
[홈 화면] 아이콘 탭
   └─▶ [앱 실행] 카메라 권한 확인
          ├─ 권한 없음 ─▶ 권한 요청 다이얼로그 ─▶ 거부 시 안내 후 종료
          └─ 권한 있음
               └─▶ 카메라 열기 + 미리보기 표시
                     └─▶ AE/AF 안정화 대기 (최대 1.0초)
                           └─▶ 자동 촬영 ─▶ 셔터음/진동
                                 └─▶ MediaStore 저장 ─▶ "저장됨" 토스트
                                       └─▶ 앱 종료 (홈 화면 복귀)
```

---

## 4. 기능 요구사항

| ID | 요구사항 | 우선순위 |
|---|---|---|
| F-01 | 런처 아이콘 탭 시 추가 입력 없이 자동으로 1장 촬영 | 필수 |
| F-02 | 최초 실행 시 `CAMERA` 권한 요청. 거부 시 사유 안내, "다시 묻지 않음" 상태면 앱 설정 화면 이동 버튼 제공 | 필수 |
| F-03 | 기본 카메라: 후면 메인(광각) 렌즈 | 필수 |
| F-04 | 촬영 전 AE(노출)/AF(초점) 수렴 대기. 타임아웃(1.0초) 초과 시 그대로 촬영 | 필수 |
| F-05 | 저장 위치: `DCIM/AutoShot/`, 파일명 `AUTOSHOT_yyyyMMdd_HHmmss_SSS.jpg` | 필수 |
| F-06 | 저장 후 갤러리 앱에서 즉시 보이도록 MediaStore 등록 | 필수 |
| F-07 | 촬영 피드백: 셔터음(`MediaActionSound`) + 짧은 진동 + 토스트 | 필수 |
| F-08 | 저장 완료 후 앱 자동 종료(`finishAndRemoveTask()`), 최근 앱 목록에 남기지 않음 | 필수 |
| F-09 | 촬영 중 아이콘 재탭 등 중복 실행 시 1회만 촬영 (`singleTask` + 촬영 중 플래그) | 필수 |
| F-10 | 오류(카메라 점유 중, 저장 공간 부족 등) 시 토스트로 원인 표시 후 종료 | 필수 |
| F-11 | EXIF 방향(회전) 정보 올바르게 기록 | 필수 |
| F-12 | 위치 정보 EXIF 기록 (옵션, 기본 OFF) | 선택 |

---

## 5. 비기능 요구사항

| ID | 항목 | 목표치 | 측정 방법 |
|---|---|---|---|
| N-01 | 아이콘 탭 → 셔터 (콜드 스타트) | ≤ 1.5초 | `adb logcat` 타임스탬프, 10회 평균 |
| N-02 | 아이콘 탭 → 셔터 (웜 스타트) | ≤ 1.0초 | 동일 |
| N-03 | 촬영 성공률 | 100% (정상 조건 50회 연속) | 수동 반복 테스트 |
| N-04 | 결과물 품질 | 흔들림·초점 이탈 없는 사진 | 육안 확인, 실내/실외/저조도 각 10장 |
| N-05 | APK 크기 | ≤ 10MB | 빌드 산출물 확인 |
| N-06 | 개인정보 | 네트워크 권한 없음, 외부 전송 없음 | Manifest 검사 |

> N-01/N-02 수치는 목표치이며 실측 전이다. S23 Ultra에서 CameraX 초기화 + 3A 수렴 시간을 1차 프로토타입에서 측정한 뒤 조정한다.

---

## 6. 기술 스택

| 영역 | 선택 | 선택 이유 |
|---|---|---|
| 언어 | Kotlin | Android 공식 권장 언어 |
| 카메라 | **CameraX** (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`) | Camera2 대비 코드량이 적고, 기기별 호환성 문제(Samsung 포함)를 라이브러리가 흡수. 자동 촬영 1장이라는 단순 요구에 충분 |
| UI | Android View (`PreviewView` 1개) | 화면이 사실상 미리보기 하나뿐이라 Compose 도입 이득이 적음 |
| 저장 | `MediaStore` API | API 29+에서 저장소 권한 없이 `DCIM/` 저장 가능 |
| 빌드 | Gradle (Kotlin DSL) + Android Studio 최신 안정판 | |
| 테스트 | JUnit4, AndroidX Test, Espresso(최소), 실기기 수동 테스트 | |

라이브러리 버전은 착수 시점에 최신 안정 버전을 확인해 `gradle/libs.versions.toml`에 고정한다.

**Camera2 직접 사용을 택하지 않은 이유:** 1장 자동 촬영에는 CameraX의 `ImageCapture`로 충분하고, Camera2는 세션·3A 상태 머신을 직접 관리해야 해서 코드와 버그 표면이 3~5배 늘어난다. 속도가 N-01을 만족하지 못할 때만 Camera2 전환을 검토한다.

---

## 7. 아키텍처

단일 Activity 구조. 별도 ViewModel/DI 없이 시작하고, 기능이 늘면 분리한다.

```
app/
 └─ src/main/java/.../autoshot/
     ├─ CaptureActivity.kt      // 진입점. 권한 확인 → 카메라 바인딩 → 촬영 → 종료
     ├─ CameraController.kt     // CameraX 바인딩, 3A 대기, takePicture 래핑
     ├─ PhotoSaver.kt           // MediaStore 저장 옵션/파일명 생성
     └─ Feedback.kt             // 셔터음, 진동, 토스트
```

### 7.1 촬영 흐름 상세
1. `CaptureActivity.onCreate()` — 레이아웃에 `PreviewView`만 배치, 화면 켜짐 유지
2. 권한 확인 → 없으면 `ActivityResultContracts.RequestPermission`
3. `ProcessCameraProvider`로 `Preview` + `ImageCapture` 바인딩
   - `ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY` 사용 (N-01 우선)
   - 결과 화질이 부족하면 `MAXIMIZE_QUALITY`로 전환 후 지연 재측정
4. 미리보기 스트림 시작 후 중앙 영역 `FocusMeteringAction`(AF+AE) 실행 → 완료 콜백 또는 1.0초 타임아웃
5. `takePicture(OutputFileOptions(MediaStore), ...)` 호출
6. 성공 콜백 → 피드백 → `finishAndRemoveTask()`

### 7.2 Manifest 핵심 설정
```xml
<uses-feature android:name="android.hardware.camera.any" android:required="true" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.VIBRATE" />

<activity
    android:name=".CaptureActivity"
    android:exported="true"
    android:launchMode="singleTask"
    android:excludeFromRecents="true"
    android:screenOrientation="portrait"
    android:theme="@style/Theme.AutoShot.Fullscreen">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```
- `INTERNET` 권한은 넣지 않는다 (N-06).
- 세로 고정은 v1.0 기본값. 가로 촬영이 필요하면 §12에서 결정.

---

## 8. 플랫폼 제약 및 설계 결정

| 제약 | 내용 | 대응 |
|---|---|---|
| 화면 없는 촬영 불가 | Android 11+에서는 백그라운드 앱의 카메라 접근이 차단되고, 포그라운드 서비스도 백그라운드에서 카메라용으로 새로 시작할 수 없다 | 아이콘 탭으로 Activity가 포그라운드에 올라온 상태에서만 촬영. 화면에 미리보기가 잠깐(약 1초) 보이는 것은 **정상 동작**으로 정의 |
| 카메라 사용 표시 | 카메라 사용 중 상태 표시줄에 초록 점(개인정보 표시)이 뜬다 | OS 기능이라 숨길 수 없음. 정상 동작 |
| 셔터음 | 국내 판매 휴대폰은 촬영음이 나도록 하는 것이 관행/표준이다. Camera2/CameraX는 셔터음을 자동으로 내지 않는다 | `MediaActionSound.SHUTTER_CLICK`으로 항상 재생, 무음 옵션 제공하지 않음 |
| 미리보기 없이 촬영 | `ImageCapture`만 바인딩해도 촬영은 가능하나 AE/AF 수렴이 불안정해 어두운/흐린 사진이 나올 수 있다 | `Preview`를 함께 바인딩해 3A를 안정화 |
| 200MP 센서 | 서드파티 앱은 보통 픽셀 비닝된 12MP급 출력을 받는다 (추정, 실기기에서 `ImageCapture` 해상도 목록으로 확인 필요) | v1.0은 기본 해상도 사용. 고해상도 필요 시 별도 검토 |
| 카메라 점유 | 삼성 카메라 앱 등 다른 앱이 카메라 사용 중이면 열기 실패 | F-10 오류 처리 |

---

## 9. 테스트 계획

### 9.1 실기기 준비 (Galaxy S23 Ultra)
1. `설정 > 휴대전화 정보 > 소프트웨어 정보 > 빌드번호` 7회 탭 → 개발자 옵션 활성화
2. `설정 > 개발자 옵션 > USB 디버깅` ON
3. PC와 USB-C 연결 → 기기에서 "USB 디버깅 허용" 승인 (또는 Android Studio의 **무선 디버깅** 페어링)
4. `adb devices`로 연결 확인 → Android Studio에서 Run 또는 `./gradlew installDebug`

### 9.2 자동 테스트
| 대상 | 방식 | 내용 |
|---|---|---|
| `PhotoSaver` | JVM 단위 테스트 | 파일명 형식, 저장 경로, ContentValues 구성 |
| `CaptureActivity` | Instrumented (실기기) | 권한 부여 상태로 실행 → MediaStore에 새 이미지 1건 증가 확인 → Activity 종료 확인 |

### 9.3 수동 테스트 케이스
| TC | 시나리오 | 기대 결과 |
|---|---|---|
| TC-01 | 최초 설치 후 아이콘 탭 | 권한 요청 → 허용 → 자동 촬영 → 저장 → 종료 |
| TC-02 | 권한 거부 | 안내 메시지 표시 후 종료, 촬영 없음 |
| TC-03 | "다시 묻지 않음" 거부 후 재실행 | 앱 설정 이동 안내 |
| TC-04 | 권한 허용 상태에서 아이콘 탭 10회 | 매회 1장씩, 총 10장 저장 |
| TC-05 | 아이콘 빠르게 2회 연속 탭 | 1장만 저장 |
| TC-06 | 삼성 카메라 앱 실행 중 분할 화면에서 AutoShot 실행 | 오류 토스트 후 종료, 크래시 없음 |
| TC-07 | 저조도(실내 소등) | 노출이 확보된 사진 저장 |
| TC-08 | 갤러리 앱 확인 | `AutoShot` 앨범에 사진 표시, 방향 정상 |
| TC-09 | 기기를 가로로 들고 촬영 | EXIF 회전 정보로 올바른 방향 표시 |
| TC-10 | 콜드/웜 스타트 지연 측정 | N-01, N-02 충족 |
| TC-11 | 저장 공간 부족 상태 | 오류 토스트, 크래시 없음 |
| TC-12 | 무음/진동 모드 | 셔터음 재생 여부 확인 (국내 정책 확인용) |

---

## 10. 개발 일정 (안)

| 단계 | 내용 | 산출물 | 예상 기간 |
|---|---|---|---|
| M0 | 프로젝트 생성, Gradle/버전 카탈로그, 빈 Activity 실기기 실행 | 설치 가능한 빈 APK | 0.5일 |
| M1 | 권한 처리 + CameraX 미리보기 | 미리보기 표시 | 1일 |
| M2 | 자동 촬영 + MediaStore 저장 + 자동 종료 | **핵심 기능 동작 APK** | 1일 |
| M3 | 3A 대기, 피드백, 중복 실행 방지, 오류 처리 | F-01~F-11 충족 | 1일 |
| M4 | 테스트 작성 및 실기기 수동 테스트, 지연 튜닝 | 테스트 결과표 | 1일 |
| M5 | 아이콘/이름 정리, 릴리스 서명 APK | release APK | 0.5일 |

---

## 11. 확장 후보 (v1.0 이후)
- 앱 아이콘 길게 누르기 메뉴(App Shortcuts): "전면 카메라로 찍기", "3초 타이머"
- 홈 화면 위젯 / Quick Settings 타일에서 즉시 촬영
- 연속 촬영(N장), 타이머
- 촬영 직후 썸네일 1초 표시 후 종료

---

## 12. 미결 사항 (확인 필요)

아래는 기본값을 정해 두었으며, 다르게 원하면 변경한다.

| # | 질문 | 기본값 |
|---|---|---|
| Q1 | 촬영 후 동작: 바로 종료 vs 결과 사진 잠깐 표시 | 바로 종료 |
| Q2 | 카메라: 후면 vs 전면 | 후면 메인 |
| Q3 | 미리보기가 약 1초 보이는 것 허용 여부 (완전히 화면 없이 촬영은 OS 제약상 불가) | 허용 |
| Q4 | 플래시 | 자동(AUTO) |
| Q5 | 화면 방향 | 세로 고정 |
| Q6 | 배포 방식: 개인 사용(APK 직접 설치) vs Play 스토어 | 개인 사용 |
| Q7 | 앱 이름 | AutoShot |
