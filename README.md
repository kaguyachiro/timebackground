<div align="center">
  
# 🕒 시간배경 (Sky Character Live Wallpaper)

**시간의 흐름에 따라 하늘색이 변하는 나만의 캐릭터 라이브 배경화면**

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack_Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)

</div>

<br>

##  주요 기능

* **실시간 하늘 그라데이션**  
  현재 시간에 맞춰 배경색이 새벽 ➔ 일출 ➔ 낮 ➔ 일몰 ➔ 밤으로 아주 부드럽게 전환됩니다.
* **커스텀 팔레트**  
  낮과 밤, 일출과 일몰의 하늘색을 내가 원하는 예쁜 파스텔톤으로 직접 세팅할 수 있습니다.
* **캐릭터 커스터마이징**  
  최애 캐릭터, 반려동물, 혹은 투명 배경(PNG) 이미지를 넣어 나만의 폰 꾸미기가 가능합니다.
* **24시간 미리보기**  
  [▶ 24H] 버튼을 눌러 하루 동안 하늘이 어떻게 변하는지 8초 만에 빠르게 미리 감상할 수 있습니다.

<br>

##  다운로드 및 설치 방법

이 앱은 구글 플레이 스토어가 아닌 **GitHub를 통해 직접 무료로 배포**됩니다.

1. 우측의 **[Releases]** 탭으로 이동하거나 [여기](https://github.com/kaguyachiro/timebackground/releases/latest/download/app-release.apk)를 클릭합니다.
2. 최신 버전의 `app-release.apk` 파일을 스마트폰으로 다운로드합니다.
3. 다운로드한 APK 파일을 실행하여 설치합니다. *(※ 출처를 알 수 없는 앱 설치 허용 필요)*
※google play 프로텍트가 떴을 경우
세부정보 더보기 -> 무시하고 설치하기 진행

<br>

##  기술 스택

* **Language**: Kotlin
* **UI**: Jetpack Compose
* **Image Loading**: Coil
* **Live Wallpaper**: Android `WallpaperService` & `Canvas` API
* **Architecture**: SharedPreferences를 활용한 Service - Activity 실시간 상태 동기화

<br>

---
<div align="center">
  <i>Make your home screen alive!</i>
</div>
