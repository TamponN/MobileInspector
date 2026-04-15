# Мобильный инспектор (MobileInspector)

Android-приложение для контроллеров-обходчиков энергосбытовых организаций. Позволяет снимать показания приборов учёта, создавать акты проверки и допуска, синхронизировать данные с сервером 1С:Предприятие.

## Стек технологий

| Компонент | Технология |
|---|---|
| Язык | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Архитектура | Clean Architecture, MVVM |
| DI | Dagger Hilt |
| БД | Room (SQLite) |
| Сеть | Retrofit + OkHttp |
| Сериализация | kotlinx.serialization |
| Хранение настроек | Jetpack DataStore |
| OCR | Google ML Kit Text Recognition |
| Камера | CameraX |
| Карты | Google Maps Compose |
| Навигация | Jetpack Navigation Compose |

## Требования

- **Android Studio** Ladybug (2024.2) или новее
- **JDK 17**
- **Android SDK 35** (compileSdk)
- **minSdk 26** (Android 8.0+)
- **Google Maps API ключ** (для экрана карты)

## Развёртывание

### 1. Клонирование

```bash
git clone <url-репозитория>
cd MobileInspector
```

### 2. Настройка local.properties

Создайте файл `local.properties` в корне проекта:

```properties
sdk.dir=C\:\\Users\\<ваш_пользователь>\\AppData\\Local\\Android\\Sdk
```

### 3. Google Maps API ключ

В файле `app/src/main/res/values/strings.xml` замените значение:

```xml
<string name="google_maps_key">YOUR_GOOGLE_MAPS_API_KEY_HERE</string>
```

на ваш ключ из [Google Cloud Console](https://console.cloud.google.com/apis/credentials) (включите Maps SDK for Android).

### 4. Сборка

```bash
# Linux / macOS
./gradlew assembleDebug

# Windows
.\gradlew.bat assembleDebug
```

APK будет в `app/build/outputs/apk/debug/app-debug.apk`.

### 5. Установка на устройство

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Настройка сервера 1С

Приложение работает с HTTP-сервисом 1С:Предприятие, опубликованным по адресу:

```
{схема}://{адрес}/{база}/hs/api/WorkTasks
```

- **GET** — загрузка маршрутных листов (Header: `UUID`, `Authorization: Basic`)
- **POST** — отправка результатов обхода (показания, акты, статусы)

При первом запуске приложения укажите:
- Адрес сервера (IP:порт)
- Имя базы данных
- UUID устройства (должен быть зарегистрирован в 1С)
- Логин и пароль пользователя 1С

## Структура проекта

```
app/src/main/java/com/bestplus/mobileinspector/
├── data/           # Слой данных
│   ├── local/      #   Room БД, DAO, Entity, Converters
│   ├── remote/     #   Retrofit API, DTO
│   └── repository/ #   Реализации репозиториев
├── di/             # Dagger Hilt модули
├── domain/         # Доменный слой
│   ├── model/      #   Модели данных (RouteSheet, Subscriber, etc.)
│   └── repository/ #   Интерфейсы репозиториев
└── ui/             # Presentation слой (Compose)
    ├── camera/     #   Экран камеры + OCR
    ├── inspection/ #   Экран обследования абонента
    ├── login/      #   Экран авторизации
    ├── map/        #   Экран карты Google Maps
    ├── routes/     #   Список маршрутных листов
    ├── settings/   #   Настройки приложения
    ├── subscribers/#   Список абонентов маршрута
    └── theme/      #   Material 3 тема
```
