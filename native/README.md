# SendIT 모바일 공유 수신

웹 PWA의 `share_target`은 SendIT 웹앱을 실행합니다. 원래 앱 화면 위에서 컬렉션을 선택하려면 이 디렉터리의 네이티브 앱을 설치해야 합니다. 앱을 한 번 열어 로그인하면 공유 창에서도 로그인 상태를 사용합니다.

## 동작

- Android: `ACTION_SEND` → 투명한 `ShareActivity` → `/share-target?native=android` → 컬렉션 확정 → 완료 버튼으로 Activity 종료, 원래 앱 복귀.
- iOS: 시스템 공유 메뉴 → `SendITShare` 확장 → `/share-target?native=ios` → 컬렉션 확정 → `completeRequest`로 확장 닫기. 본체 앱을 실행하지 않습니다.
- 공통: 공유 URL은 먼저 분석 대기로 등록됩니다. 컬렉션을 확정하기 전에는 장소 자동 저장이 수행되지 않습니다. 분석 완료 전 선택해도 완료 후 같은 컬렉션으로 저장됩니다. 로그인하지 않았거나 네트워크 요청이 실패하면 완료라고 표시하지 않습니다.
- 공유 페이지의 컬렉션 기본값은 자동 분석 결과이며, 수동 선택 이후에는 추천이 갱신되어도 사용자 선택을 덮어쓰지 않습니다.
- Android 세션은 Android Keystore AES-GCM으로 암호화하고, iOS 세션은 본체와 확장이 공유하는 Keychain access group에 보관합니다. 메시지 브리지는 지정한 HTTPS origin의 메인 프레임에만 허용합니다. 세션을 URL 쿼리로 전달하지 않습니다.

## Android 빌드

필요: JDK 17 이상, Android SDK 35, Gradle 8.11.1 이상(8.x).

```sh
cd native/android
gradle :app:assembleDebug -PsenditOrigin=https://YOUR-SENDIT-DOMAIN
```

Windows의 프로젝트 경로에 한글이 포함되어 Gradle 경로 검사에 걸리면 빌드 명령에 `-Pandroid.overridePathCheck=true`를 추가합니다.

`senditOrigin`은 실제 배포된 프런트엔드 HTTPS origin으로 지정합니다. 기본값 `https://sendit.example.com`은 빌드 확인용이며 접속 가능한 서비스가 아닙니다. 프런트엔드 API 주소도 기기에서 접근 가능한 HTTPS 주소여야 합니다. APK: `app/build/outputs/apk/debug/app-debug.apk`. 스토어 배포에는 별도 release signing이 필요합니다. 최신 Android System WebView가 필요합니다.

## iOS 빌드

필요: macOS, Xcode, XcodeGen, Apple 개발자 서명 팀.

```sh
cd native/ios
xcodegen generate
open SendIT.xcodeproj
```

1. 두 타깃의 Signing Team을 같은 팀으로 설정합니다.
2. `SENDIT_ORIGIN`을 실제 HTTPS 프런트엔드 주소로 설정합니다.
3. bundle identifier를 소유한 식별자로 변경하는 경우 `SENDIT_KEYCHAIN_GROUP`도 두 타깃에서 동일하게 변경합니다. Keychain Sharing entitlement가 해당 서명 프로필에 포함되어야 합니다.
4. 본체 앱을 기기에 설치하고 로그인한 다음 Safari 등에서 링크 공유 → SendIT을 선택합니다.

`project.yml`은 본체와 확장, 공유 Keychain 설정을 함께 생성합니다. Windows에서는 Xcode 빌드·코드 서명·iOS 실기기 공유 동작을 검증할 수 없습니다.

## 실기기 확인

- 원래 앱에서 공유 → SendIT 본체로 전환되지 않고 선택창 표시 → 기본 추천/기존 컬렉션/새 컬렉션 각각 저장 → 완료 → 원래 앱 복귀.
- 분석 완료 전·후 확정, 동일 링크 재공유, 여러 장소가 담긴 게시물, 저장 중 네트워크 끊김, 로그인 만료, 컬렉션 추가 실패 확인.
- 컬렉션 확정 없이 닫으면 새 장소가 저장되지 않아야 합니다.
- 앱 본체에서 로그아웃한 후 공유 창에서도 로그인이 필요해야 합니다.
- 키보드 표시 시 저장 버튼 접근, VoiceOver/TalkBack, 안전 영역, Android 뒤로가기 확인.

## 참고

- [Android 공유 수신](https://developer.android.com/training/basics/intents/filters)
- [Android origin 제한 메시지 브리지](https://developer.android.com/reference/androidx/webkit/WebViewCompat)
- [Apple Share Extension](https://developer.apple.com/library/archive/documentation/General/Conceptual/ExtensibilityPG/Share.html)
- [PWA share_target](https://developer.mozilla.org/en-US/docs/Web/Progressive_web_apps/Manifest/Reference/share_target)
