# inter-map — Kotlin MapView для Android

### Продолжение osmdroid на Kotlin с офлайн-картами, overlay и кластеризацией маркеров

[![JitPack](https://jitpack.io/v/Khokhlinvladimir/inter-map.svg)](https://jitpack.io/#Khokhlinvladimir/inter-map)
[![CI](https://github.com/Khokhlinvladimir/inter-map/actions/workflows/CI.yml/badge.svg)](https://github.com/Khokhlinvladimir/inter-map/actions/workflows/CI.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-API%2024%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/about/versions/nougat)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

[English version](README.md) · [Скачать демо APK](https://github.com/Khokhlinvladimir/inter-map/releases/latest/download/inter-map-demo-v1.0.0-debug.apk) · [Последний релиз](https://github.com/Khokhlinvladimir/inter-map/releases/latest)

## Описание

**inter-map** — Android `MapView` для онлайн- и офлайн-тайлов, маркеров, фигур, геолокации, WMS, Mapsforge и GeoPackage. Проект основан на финальной архивной версии [osmdroid](https://github.com/osmdroid/osmdroid), синхронизирован с upstream до коммита [`b491ee4`](https://github.com/osmdroid/osmdroid/commit/b491ee4b7) и полностью переведён на Kotlin.

Библиотека сохраняет привычную модель API osmdroid и добавляет встроенную grid-кластеризацию маркеров. Демо-приложение использует Material 3 Views, светлую и тёмную темы, динамические цвета и актуальные Google Material icons — без Compose.

> Это независимое продолжение сообщества, а не официальный релиз `org.osmdroid`.

## Галерея

<table>
  <tr>
    <td align="center"><strong>Каталог примеров</strong><br><img src="docs/screenshots/home.png" alt="Современный каталог примеров" width="260"></td>
    <td align="center"><strong>Стартовая карта</strong><br><img src="docs/screenshots/starter-map.png" alt="Стартовая карта" width="260"></td>
    <td align="center"><strong>Кластеризация</strong><br><img src="docs/screenshots/clustering.png" alt="Кластеризация маркеров" width="260"></td>
  </tr>
</table>

## Основные возможности

1. Kotlin API, совместимый с привычной моделью osmdroid.
2. Онлайн-тайлы, офлайн-архивы, кэш и инструменты упаковки карт.
3. Маркеры, линии, полигоны, геолокация и собственные overlay.
4. Линейная по сложности Web Mercator кластеризация с поддержкой антимеридиана.
5. Интеграции WMS, Mapsforge, GeoPackage и shapefile.
6. Material 3 демо-приложение со 106 запускаемыми примерами.
7. Android API 24+, Day/Night, динамические цвета и доступные элементы управления.

# Инструкция по подключению inter-map

## Шаг 1. Подключите библиотеку

Добавьте JitPack в конец списка репозиториев в `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}
```

Подключите основной модуль:

```kotlin
dependencies {
    implementation("com.github.Khokhlinvladimir.inter-map:osmdroid-android:v1.0.0")
}
```

## Шаг 2. Добавьте и настройте карту

```xml
<org.osmdroid.views.MapView
    android:id="@+id/map"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

```kotlin
val map = findViewById<MapView>(R.id.map)
map.setTileSource(TileSourceFactory.MAPNIK)
map.setMultiTouchControls(true)
map.controller.setZoom(12.0)
map.controller.setCenter(GeoPoint(55.751244, 37.618423))
```

Для онлайн-тайлов добавьте разрешения в manifest приложения:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## Шаг 3. Передавайте lifecycle

```kotlin
override fun onResume() {
    super.onResume()
    map.onResume()
}

override fun onPause() {
    map.onPause()
    super.onPause()
}
```

## Шаг 4. Добавьте маркер

```kotlin
val marker = Marker(map).apply {
    position = GeoPoint(55.751244, 37.618423)
    title = "Москва"
    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
}
map.overlays.add(marker)
map.invalidate()
```

## Шаг 5. Включите кластеризацию

```kotlin
val clusterer = GridMarkerClusterer(map).apply {
    maximumClusterZoom = 17.0
    addAll(markers)
}
map.overlays.add(clusterer)
```

Алгоритм группирует маркеры в экранной сетке с дробным Web Mercator zoom, детерминированным порядком и корректным центроидом около антимеридиана. Нажатие на кластер приближает его содержимое.

## Дополнительные модули

```kotlin
dependencies {
    implementation("com.github.Khokhlinvladimir.inter-map:osmdroid-wms:v1.0.0")
    implementation("com.github.Khokhlinvladimir.inter-map:osmdroid-mapsforge:v1.0.0")
    implementation("com.github.Khokhlinvladimir.inter-map:osmdroid-geopackage:v1.0.0")
    implementation("com.github.Khokhlinvladimir.inter-map:osmdroid-shape:v1.0.0")
}
```

| Модуль | Назначение |
| --- | --- |
| `osmdroid-android` | MapView, тайлы, overlay, офлайн-хранилище и кластеризация |
| `osmdroid-wms` | WMS-клиент |
| `osmdroid-mapsforge` | Рендеринг Mapsforge |
| `osmdroid-geopackage` | Поддержка GeoPackage |
| `osmdroid-shape` | Поддержка shapefile |
| `OpenStreetMapViewer` | Полный каталог демо и регрессий |
| `osmdroid-simple-map` | Минимальный Android-пример |
| `OSMMapTilePackager` | Desktop-инструмент упаковки тайлов |
| `osmdroid-server-jdk` | JVM tile server |

## Сборка из исходников

Требования: JDK 17, Android SDK 37 и Build Tools 36.0.0.

```bash
./gradlew clean build
./gradlew :OpenStreetMapViewer:installDebug
./gradlew publishToMavenLocal
```

## Проверка

Релиз Kotlin-миграции и кластеризации прошёл:

- полную Gradle-сборку и GitHub Actions CI;
- JVM unit-тесты основного и дополнительных модулей;
- **106/106** зарегистрированных sample fragments на эмуляторе Android 16 / API 36;
- **11/11** исторических bug drivers;
- остальные **13** Android instrumentation-тестов.

Внешние tile-серверы, сервисы с API-ключами и локальные источники данных зависят от окружения и не включены в библиотеку.

## Технические характеристики

- Kotlin 2.4.10
- Gradle 9.5.1 и Android Gradle Plugin 9.3.0
- JDK 17
- Минимальная версия Android: API 24
- Compile SDK: 37
- Target SDK: 36
- UI демо: Material 3 Views без Compose

## Статус проекта

Оригинальный репозиторий osmdroid архивирован. Его [wiki](https://github.com/osmdroid/osmdroid/wiki), [upgrade guide](https://github.com/osmdroid/osmdroid/wiki/Upgrade-Guide), история и список участников остаются источником совместимости. Приложение обязано соблюдать правила и атрибуцию выбранного поставщика тайлов.

## Лицензия

Проект распространяется по [Apache License 2.0](LICENSE), как и upstream. История Git сохраняет авторство отдельных вкладов.

## Автор

Kotlin-продолжение поддерживает Владимир Хохлин. Telegram: [@vkhokhlin](https://t.me/vkhokhlin).

## Поддержка

Если вы нашли ошибку или хотите улучшить проект, создайте [GitHub issue](https://github.com/Khokhlinvladimir/inter-map/issues) или отправьте pull request.
