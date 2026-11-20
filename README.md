# SlugSpeed

Android приложение, което мери средната скорост на слъг или сачма от въздушна пушка само по звука.

Телефонът записва изстрела и удара в мишената. От времето между двата звука, разстоянието и температурата на въздуха приложението смята скоростта в м/с, fps и енергията в джаули.

## Възможности

- Засича автоматично началото на изстрела и на удара с точност до 0.02 ms (48 kHz)
- Скорост в **м/с** и **J** една до друга; докосване сменя J ↔ **fps**
- Вълна и увеличение около удара с маркери, които се местят с пръст
- Серия: средна скорост, **SD**, **ES**, мин, макс, средна енергия; споделяне като текст
- Индикатор за нивото на звука с маркер за прага на изстрела
- Всичко на един екран, настройките и помощта са в долни панели
- Български и английски, сменят се с флагчето

## Инсталация

Свали **SlugSpeed-1.0.apk** от [Releases](https://github.com/smouky577/slugspeed/releases/tag/v1.0) и го инсталирай на телефона. Ако Android попита, разреши инсталация от този файл.

## Как се мери

1. Измери с ролетка разстоянието от дулото до мишената. 1% грешка в него дава ~1% грешка в скоростта.
2. Сложи телефона до себе си, на 0.3–1 м встрани от дулото, с микрофона към мишената.
3. В **Настройки** въведи разстоянието и температурата – от нея зависи скоростта на звука.
4. Въведи теглото на слъга в грамове (1 grain = 0.0648 гр), за да виждаш енергията в джаули.
5. Мишената трябва да звучи рязко при удар: метална плочка, гонг, консерва.
6. Натисни **Старт** и стреляй. След всеки изстрел приложението е готово за следващия.
7. Зеленият маркер е изстрелът, червеният – ударът. Ако е сбъркан, плъзни го по графиката.

Най-точно е при 15–40 м. При ехо от стени промени **Чувствителност за удара** в Настройки.

## Формула

Δt = D/v + (Dтм − Dтд)/c, откъдето v = D / (Δt − (Dтм − Dтд)/c)

D е дуло → мишена, Dтм е телефон → мишена, Dтд е телефон → дуло, а c = 331.3·√(1 + T/273.15) е скоростта на звука.

## Компилация

Проектът е за `compileSdk 36`, `minSdk 24` и Gradle 9.4. Отвори папката в Android Studio или изпълни `gradlew.bat assembleDebug`.

От командния ред Gradle ползва `JAVA_HOME` (JDK 17 или по-нов). Ако Java е само JBR на Android Studio, задай `JAVA_HOME` към него или добави `org.gradle.java.home` в потребителския файл `%USERPROFILE%\.gradle\gradle.properties` (Windows) или `~/.gradle/gradle.properties` (Linux и macOS).

---

# SlugSpeed (English)

Android app that measures the average speed of an air-rifle slug or pellet from sound alone.

The phone records the shot and the impact on the target. From the time between the two sounds, the distance and the air temperature, the app works out the speed in m/s, fps and the energy in joules.

## Features

- Detects the shot onset and the impact onset automatically, to within 0.02 ms (48 kHz)
- Speed in **m/s** and **J** side by side; tap to switch J ↔ **fps**
- Waveform and impact zoom with markers you drag with a finger
- Series: average speed, **SD**, **ES**, min, max, average energy; share as text
- Sound level meter with a mark for the shot trigger level
- Everything on one screen, settings and help in bottom sheets
- Bulgarian and English, switched with the flag

## Install

Download **SlugSpeed-1.0.apk** from [Releases](https://github.com/smouky577/slugspeed/releases/tag/v1.0) and install it on the phone. Allow installation from this file if Android asks.

## How to measure

1. Measure the muzzle-to-target distance with a tape. A 1% error in it gives ~1% speed error.
2. Put the phone next to you, 0.3–1 m to the side of the muzzle, mic towards the target.
3. In **Settings** enter the distance and temperature – the speed of sound depends on it.
4. Enter the slug weight in grams (1 grain = 0.0648 g) to see the energy in joules.
5. The target must sound sharp on impact: steel plate, gong, can.
6. Tap **Start** and shoot. After each shot the app is ready for the next.
7. Green marker = shot, red = impact. If one is wrong, drag it on the chart.

Most accurate at 15–40 m. With echoes from walls change **Impact sensitivity** in Settings.

## Formula

Δt = D/v + (Dpt − Dpm)/c, so v = D / (Δt − (Dpt − Dpm)/c)

D is muzzle → target, Dpt is phone → target, Dpm is phone → muzzle, and c = 331.3·√(1 + T/273.15) is the speed of sound.

## Build

The project uses `compileSdk 36`, `minSdk 24` and Gradle 9.4. Open the folder in Android Studio or run `gradlew.bat assembleDebug`.

From the command line Gradle uses `JAVA_HOME` (JDK 17 or newer). If the only Java is Android Studio's JBR, point `JAVA_HOME` at it, or add `org.gradle.java.home` to the user file `%USERPROFILE%\.gradle\gradle.properties` (Windows) or `~/.gradle/gradle.properties` (Linux and macOS).
