# TailscaleSwitch

Минималистичное Android-приложение: две кнопки для включения/выключения Tailscale на роутере Keenetic через SSH.

## Что делает

| Кнопка    | SSH-команда      |
|-----------|-----------------|
| VPN_ON    | `tailscale up`  |
| VPN_OFF   | `tailscale down`|

Подключается к `root@192.168.1.1`, пароль `keenetic`.  
При успехе кнопка подсвечивается (зелёный / красный).

---

## Как собрать (Android Studio — проще всего)

1. **Установи Android Studio** (https://developer.android.com/studio)
2. **File → Open** → выбери папку `TailscaleSwitch`
3. Подожди пока Gradle синхронизируется (~1-2 мин, скачает зависимости)
4. **Build → Build Bundle(s) / APK(s) → Build APK(s)**
5. APK появится в `app/build/outputs/apk/debug/app-debug.apk`

---

## Как собрать через командную строку

Нужны: JDK 17+, и всё — Android SDK скачается сам через wrapper.

```bash
# macOS / Linux
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

Перекинь на телефон (через ADB или просто файлом), разреши установку из неизвестных источников.

---

## Как установить через ADB (если телефон подключён к компу)

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Изменить параметры подключения

Всё в одном месте — `MainActivity.kt`:

```kotlin
private val SSH_HOST = "192.168.1.1"
private val SSH_PORT = 22
private val SSH_USER = "root"
private val SSH_PASS = "keenetic"
```

---

## Зависимости

- `com.github.mwiede:jsch:0.2.17` — современный форк JSch, работает на Android без танцев с бубном
- Material Components, AndroidX
