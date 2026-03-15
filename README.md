# MaterialMusicFlyout (Kotlin Multiplatform)

Proyecto Kotlin Multiplatform con 3 modulos:

- `shared`: UI Compose, animaciones, estado de reproduccion y barra sinusoidal.
- `desktopApp`: entrada Windows (Compose Desktop), ventana `900x420` centrada.
- `androidApp`: host Android minimo para compilar multiplataforma.

## Caracteristicas implementadas

- Tarjeta musical estilo glass con blur, overlay oscuro y bordes redondeados.
- Integracion con sesion multimedia global de Windows (WinRT GSMTC) para:
  - titulo, artista, duracion, posicion, portada
  - acciones `play/pause`, `next`, `previous`
- Fallback visual a `cover_preview` si no hay portada activa.

## Requisitos para Desktop en Windows

- JDK 17
- .NET SDK 8 (para compilar `desktopApp/winrt-helper`)
- NuGet con acceso a `Microsoft.Windows.SDK.NET` (en este entorno se usa `10.0.18362.6-preview`)

## Ejecutar en Windows (cmd.exe)

```bat
gradlew.bat :desktopApp:run
```

> El task `run` publica automaticamente el helper WinRT antes de iniciar la app.

## Publicar helper WinRT manualmente

```bat
gradlew.bat :desktopApp:publishWinRtHelper
```

## Compilar Android placeholder

```bat
gradlew.bat :androidApp:assembleDebug
```

## Empaquetar EXE de Desktop

```bat
gradlew.bat :desktopApp:packageDistributionForCurrentOS
```

## Cambiar portada/paleta

Edita `paletteSeed` en `shared/src/commonMain/kotlin/com/myg/materialmusicflyout/shared/model/MusicState.kt`.
