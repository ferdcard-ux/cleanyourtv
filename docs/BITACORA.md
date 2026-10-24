# Bitácora de Desarrollo — CleanYourTV (fork ferdcard-ux) en Caixun Sgamer

> **Propósito:** Registro cronológico de todas las implementaciones, correcciones y decisiones técnicas del desarrollo.
>
> **Alcance:** la bitácora es el registro íntegro, **incluidos los intentos fallidos, las ideas descartadas y lo que quedó sin verificar**. El proyecto no tiene archivo de notas de release propio; el resumen público del proyecto sigue siendo el [README](../README.md).

Fork: `ferdcard-ux/cleanyourtv` (upstream `Gathin23/cleanyourtv`). Rama de trabajo: `launcher-on-tv-theme-settings`, creada desde `main` en `f2a336a`. A la fecha de la última entrada **no hay ningún commit** del trabajo descrito aquí: por instrucción del propietario, solo se hace commit cuando él lo ordena.

Dispositivo de todas las entradas:

| Campo | Valor |
|---|---|
| Marca / fabricante | Caixun / Expressluck |
| Modelo / device / product | Sgamer / `cottongreen` / `Caixun_4K_RATV3` |
| Sistema | Google TV, Android 14 (SDK 34), firmware `UKRC.260302.020` (`Smarttech_4K_RATV3-user`) |
| SoC / plataforma | Realtek RTD2885N / `rtd6748` |
| RAM | ~1,7 GB (`MemTotal 1739624 kB`) |
| Red | Wi-Fi `KIRA9142-5GHz`, IP `192.168.1.13` |

---

## 1. Diagnóstico de compatibilidad del TV con CleanYourTV

Tipo: registro de verificación. Fecha: 2026-10-24.

### Qué se comprobaba
Si el fork (debloat + launcher `com.gathin.tvhome`) se puede aplicar a este TV, que no tenía lista propia en `examples/`.

### Métodos
- `adb devices -l` sobre la conexión de depuración inalámbrica (`adb-5C857EF4F339-rfpAQ4._adb-tls-connect._tcp`).
- `getprop` (marca, fabricante, plataforma, versión, fingerprint), `pm list features`, `/proc/meminfo`.
- `cmd package resolve-activity` para la categoría HOME y para `com.android.tv.action.VIEW_INPUTS`.
- `pm list packages` (todos, `-d` desactivados, `-3` instalados por el usuario) y `dumpsys package` de los candidatos a desactivar (ruta y flags).
- Lectura del repo: README, PROMPT.md, `launcher/app/build.gradle`, listas de `examples/` con la misma plataforma o el mismo fabricante.

### Resultado
**Compatible** (verificado por lectura del dispositivo y del código, sin instalar nada en ese momento):
- Google TV 14 con `android.software.leanback_only`; el launcher declara `minSdk 26` y `targetSdk 34`.
- Misma plataforma `rtd6748` que `examples/kalley-r10g/` y `examples/prismplus-q65-ultra-r10g/`; mismo fabricante (Expressluck) que `examples/aipro2k/`. La de Kalley R10G, también de Latinoamérica, se tomó como base.
- Home de fábrica: `com.google.android.apps.tv.launcherx/.home.HomeActivity`.
- Selector de entradas: `com.google.android.tv.inputplayer/.switcher.SelectInputActivity`.
- 180 paquetes. Ya venían desactivados de fábrica `com.google.android.play.games` y `com.google.android.youtube.tvmusic`.
- El TV no es un TCL chino que rechace launchers ni un Fire TV.

### Qué no se pudo comprobar
- Que el launcher se convirtiera en el Home: solo se confirmó al instalar (ver entrada 4, donde el primer intento falló).
- Si el botón Input del control (`KEYCODE_TV_INPUT`) queda sin manejar al quitar el Home de fábrica. Se dedujo por analogía con Kalley R10G y por eso se instaló con `-InputKeyService`.

### Cambios residuales en el entorno
- Ninguno: todo fue lectura.

---

## 2. Lista de debloat `examples/caixun-4k-ratv3/debloat.txt`

Tipo: funcionalidad + decisiones técnicas. Fecha: 2026-10-24.

### Implementación
Archivo nuevo [`examples/caixun-4k-ratv3/debloat.txt`](../examples/caixun-4k-ratv3/debloat.txt), en el formato de las demás listas (cabecera con modelo, comandos Apply/Undo, launcher de fábrica, qué conservar). Paquetes que desactiva en su versión final:

| Paquete | Motivo |
|---|---|
| `tv.anoki.acr.anokiacroptin` | ACR: reconocimiento del contenido en pantalla (`/system/priv-app/AnokiAcrOptin`) |
| `com.realtek.retailmode` | Modo demo de tienda |
| `com.google.android.partnersetup` | Configuración de socio de Google |
| `com.google.android.feedback` | Envío de reportes a Google |
| `com.seraphic.hbbtv.app` | Superposiciones HbbTV de la TV abierta |
| `com.google.android.tungsten.setupwraith` | Asistente de configuración: reclama el Home al quitar `launcherx` (ver entrada 4) |

### Decisiones
1. **Apps instaladas por el usuario, comentadas:** Prime Video, iQIYI, Viki y TCL Browser aparecen en `pm list packages -3`, es decir, no son preinstaladas. Se dejan como líneas comentadas.
2. **Opcionales que se conservan:** `com.google.android.apps.tv.dreamx` (salvapantallas de Google TV) y `com.rtk.mediabrowser` (reproductor USB).
3. **Karaoke conservado por decisión del propietario.** La primera versión de la lista desactivaba `com.realtek.kara` y `com.realtek.karaoke.userguide`; se pasaron a comentarios.
4. **`launcherx` fuera de la lista:** `launcher/build-and-install.ps1` lo desactiva solo después de comprobar que el launcher nuevo arranca, y lo reactiva si el Home no queda en él. Ponerlo en la lista lo desactivaría antes de tener un Home de reemplazo.
5. **Conservar** (anotado en la lista): `com.realtek.dtv`, `com.realtek.tv`, `com.realtek.tv.atv`, `com.google.android.tv.inputplayer` (HDMI y entradas), `com.realsil.ota` (actualizaciones del control), `com.google.android.katniss` (voz), `com.google.android.apps.mediashell` (Chromecast), `com.google.android.tv.remote.service` (control desde el teléfono), `com.realtek.keyintercept.epl` y `com.realtek.globalkeyhandler` (botones del control; **deducido por el nombre, no probado**), y los menús de fábrica `com.expressluck.tvfactory` y `com.realtek.tv110factorytools`.
6. **Validación de nombres:** todos pasan la misma expresión regular que aplica `tools/disable.ps1` antes de enviar nada al TV.

### Alcance del cambio
- Toca: solo `examples/caixun-4k-ratv3/`.
- Fuera de alcance: las tablas de `README.md` y `examples/README.md`. Por eso `tests/test_examples.py::test_examples_table_matches_folders` falla mientras la carpeta exista sin su fila en `examples/README.md`. Hay que añadir esa fila en el mismo commit que la lista.

### Resultado verificado
Verificado en hardware: aplicada en el TV (entrada 4). `pm list packages -d` muestra los 6 paquetes desactivados más los 2 que venían así de fábrica.

### No verificado / atajos de prueba
- Que `com.realtek.keyintercept.epl` y `com.realtek.globalkeyhandler` manejen los botones del control: deducido, no probado.
- La lista no se ha usado durante varios días; la cabecera lo indica.

### Corrige
- Corrige a la primera versión de la lista: desactivaba el karaoke y `launcherx` y dejaba `setupwraith` comentado. Las tres cosas cambiaron por los motivos 3 y 4 y por la entrada 4.

---

## 3. Opciones de personalización en «Ajustes del lanzador»

Tipo: funcionalidad. Fecha: 2026-10-24.

### Problema
Antes de este cambio, la pantalla de ajustes del launcher (mantener OK sobre **Ajustes** → **Ajustes del lanzador**) solo cambiaba las columnas, las animaciones y los efectos avanzados. Todo lo demás (tema, color de fondo, fondo de pantalla, cielo, estadísticas, biblioteca) solo se cambiaba enviando un `config.json` desde el PC con `tools/theme.ps1`.

### Implementación
1. [`Config.java`](../launcher/app/src/main/java/com/gathin/tvhome/Config.java):
   - Nuevas opciones que se guardan en el TV (preferencias `launcher_settings`) y tienen prioridad sobre el tema: `background_color`, `sky_intensity`, `show_stats`, `app_library` y `wallpaper_mode` (vacío = del tema, `none` o `custom`). Es la misma regla que ya seguían `columns`, `animations` y `advanced_effects`.
   - Las ocho claves están en `ON_TV_KEYS`. «Seguir el tema», enviar un tema o restablecerlo las borra todas juntas.
   - La imagen propia vive en los archivos privados (`files/custom_wallpaper.jpg`), así que enviar un tema no la sobrescribe.
2. [`BuiltInThemes.java`](../launcher/app/src/main/java/com/gathin/tvhome/BuiltInThemes.java) (nuevo):
   - Aplica desde el TV los temas Original, Aurora y Seis columnas.
   - Escribe los mismos archivos que `tools/theme.ps1` (`config.json`, `wallpaper.jpg` y un `theme_revision` nuevo), pasando por un archivo temporal.
   - «Original» borra el tema, como `theme.ps1 -Reset`.
   - Guarda `applied_theme` para marcar el tema elegido. Si después llega un tema desde el PC, esa marca se borra.
3. [`build.gradle`](../launcher/app/build.gradle): la tarea `copyBuiltInThemes` copia `themes/aurora` y `themes/six-columns` a `build/generated/builtInThemes/themes` y los añade como assets antes de `preBuild`, sin duplicar archivos en el repo.
   - `themes/default` no se empaqueta porque su `dock_apps` es de otro TV.
   - `dussehra` no se empaqueta porque el tema ya caducó.
4. [`LauncherSettingsActivity.java`](../launcher/app/src/main/java/com/gathin/tvhome/LauncherSettingsActivity.java) y [`activity_launcher_settings.xml`](../launcher/app/src/main/res/layout/activity_launcher_settings.xml) añaden:
   - Tema: 3 opciones.
   - Color de fondo: 8 muestras oscuras, dibujadas como el fondo habitual de cada opción más un recuadro del color.
   - Fondo de pantalla: Del tema, Ninguno o Tu imagen….
   - Cielo según la hora: 0, 30, 60 o 100.
   - Activar o desactivar la línea de CPU, memoria y almacenamiento, y la biblioteca de aplicaciones.
   - **Reordenar apps**, que cierra la pantalla y entra en el modo de reordenar.
5. **Tu imagen…**:
   - Abre `ACTION_GET_CONTENT image/*`, porque en este TV `ACTION_OPEN_DOCUMENT` resuelve a `com.android.tv.frameworkpackagestubs/.Stubs$DocumentsStub`, que no hace nada.
   - Decodifica la imagen en un hilo aparte usando los límites de `WallpaperSize` y la reescala al tamaño de la pantalla.
   - La guarda como JPEG de calidad 85.
6. [`MainActivity.java`](../launcher/app/src/main/java/com/gathin/tvhome/MainActivity.java): `onActivityResult` lee `EXTRA_REORDER`. El aviso sobrevive a `recreate()` mediante `onSaveInstanceState`, y `onResume` arranca el modo de reordenar después de que las apps hayan cargado y recuperado el foco.
7. Estilos `SettingsLabel` y `SettingsHint`. 19 textos nuevos en `values`, `values-es` y `values-iw`: el hebreo es obligatorio por `tests/test_hebrew_resources.py`. Los otros 13 idiomas muestran el inglés.

### Alcance del cambio
- Toca: solo el launcher (Java, recursos, gradle).
- Fuera de alcance:
  - Los scripts de `tools/`, que siguen igual y son compatibles: un tema enviado desde el PC sigue borrando los ajustes hechos en el TV.
  - Un editor de dock en el propio TV: se reutilizan el modo de reordenar y la biblioteca de aplicaciones, que ya existían.
  - No se subió la versión: `versionCode 4` y `versionName 2.3` quedan como estaban.

### Diagnóstico de un fallo de build
- **Primer `assembleRelease`:** falló en `mergeReleaseResources` con `Invalid unicode escape sequence in string` en `wallpaper_hint` y `wallpaper_failed`.
- **Causa:** los apóstrofos de «TV's» y «Couldn't» quedaron sin escapar al insertar los textos con un script.
- **Corrección:** se escaparon como `\'`.
- **Efecto secundario corregido:** un `unix2dos` aplicado por error pasó tres `strings.xml` a CRLF; se devolvieron a LF con `dos2unix`. `git diff --stat` confirmó que solo cambian las líneas añadidas.

### Resultado verificado
- **Verificado por código:** `gradlew assembleRelease testReleaseUnitTest` termina con BUILD SUCCESSFUL y pasan los tests JVM. El APK pesa 478 162 bytes e incluye `assets/themes/aurora/{config.json,wallpaper.jpg}` y `assets/themes/six-columns/config.json`.
- **Verificado en hardware:** capturas con `screencap` de la pantalla de ajustes en español, en tres tramos. Se ven todas las secciones nuevas, con la selección inicial correcta (Del tema, cielo No, 4 columnas, Seguir el tema). Se recorre con la flecha abajo. `logcat -b crash` no muestra fallos.
- **Validado por el propietario:** dijo que «todo muestra funcionar bien».

### No verificado / atajos de prueba
- La pantalla de ajustes se abrió con `am start -a com.gathin.tvhome.action.LAUNCHER_SETTINGS`, no manteniendo OK sobre el botón. La vía real con el control queda **pendiente de validar a mano**.
- **Sin comprobar en hardware:**
  - Aplicar Aurora o Seis columnas.
  - Elegir un color o un nivel de cielo.
  - Activar las estadísticas o la biblioteca.
  - Entrar en Reordenar desde los ajustes, con y sin otros cambios en la misma visita.
  - «Seguir el tema».
- **Tu imagen… no se ha probado:** no se sabe si el selector de fotos de este TV muestra imágenes de una memoria USB.
- Las traducciones al hebreo no las ha revisado un hablante nativo.
- **Problema anterior, no corregido:** «Choose default home» y su texto de ayuda salen en inglés con el TV en español, porque `home_settings` y `home_settings_help` nunca se tradujeron en `values-es`.

### Cambios residuales en el entorno
- `launcher/local.properties` creado con `sdk.dir` (está en `.gitignore`).
- `launcher/app/build/` con la salida de la compilación.

### Versión publicada
- Ninguna. Los cambios están **sin commit** en la rama `launcher-on-tv-theme-settings`.

---

## 4. Aplicación en el TV: respaldo, debloat e instalación del launcher

Tipo: registro de verificación con un intento fallido. Fecha: 2026-10-24, de 06:45 a 07:05 (UTC-5).

### Contexto
El propietario aprobó aplicar la lista (con el karaoke conservado) y cambiar el Home de Google por el launcher del repo con sus cambios nuevos. Antes de empezar desconectó del ADB un teléfono Xiaomi (`23028RA60L`, `tapas`) para que solo quedara el TV.

### Pasos y resultados
1. **Respaldo** por la conexión TLS, antes de tocar nada, en `C:\Users\migue\.cleanyourtv\backup-caixun-cottongreen-20261024\`:
   - `packages-all.txt` (`pm list packages -f -U`), `packages-disabled.txt` y `packages-thirdparty.txt`.
   - `settings-{secure,global,system}.txt`.
   - `home.txt` y `getprop.txt`.
   - `wifi-owners.txt`: las líneas `cname=` de `dumpsys wifi`, que dicen qué app creó cada red.
2. **Puerto 5555:** `adb tcpip 5555`, porque `tools/tv.py` solo habla por 5555.
3. **`tools\venv` reparado:** no tenía pip. Se corrigió con `ensurepip` y `pip install -r tools\requirements.txt`. Después, `tv.py --probe` respondió con el modelo y la versión.
4. **`tools\disable.ps1`:**
   - Escribió el registro de recuperación `C:\Users\migue\.cleanyourtv\recovery-192.168.1.13-a9ca7743.json` (06:48:41), que guarda el estado previo de los paquetes, el Home y las escalas de animación del TV (`0.0 / 0.0 / 0.5`, ya puestas así antes de este trabajo).
   - Desactivó 5 paquetes. `setupwraith` todavía estaba comentado en la lista.
5. **`launcher\build-and-install.ps1 -InputKeyService`, primer intento: FALLÓ.**
   - Lo que funcionó: el APK subió con el md5 correcto, `pm install` dio Success y `am start -W` devolvió `Status: ok` (arranque en frío, 3352 ms). Después se desactivó `launcherx` y `set-home-activity` dio Success.
   - El fallo: la comprobación final vio que el Home resolvía a `com.google.android.tungsten.setupwraith/.RecoveryActivity`.
   - El script reactivó `launcherx` y lo volvió a poner como Home. Se confirmó con `resolve-activity`: el TV quedó con el Home de Google y el launcher nuevo instalado pero sin usar.
6. **Diagnóstico de las redes Wi-Fi:**
   - Riesgo conocido: Android borra las redes guardadas que pertenecen a `setupwraith` cuando este se desactiva.
   - Se pasó `dumpsys wifi` al PC y se relacionó cada `ID: N SSID:` con la línea `cname=` que le sigue.
   - Resultado: `setupwraith` solo poseía la red antigua `"Hanny "`. La red en uso, `KIRA9142-5GHz` (Net ID 7, `mostRecentlyConnected=true`), pertenece a `android.uid.system:1000`.
   - El propietario aprobó desactivar `setupwraith` sabiendo que se borraría `"Hanny "`.
7. **Segunda pasada de `disable.ps1`:**
   - Se activó `setupwraith` en la lista y se ejecutó con `FORCE_SETUPWRAITH=1`. Hacía falta porque el script no distingue entre la red en uso y una red antigua: se niega en cuanto ve `cname=...setupwraith` en cualquier red.
   - El registro de recuperación conservó el original y solo añadió `setupwraith`.
8. **Segundo intento de instalación: CORRECTO.**
   - `am start -W` dio `Status: ok` (4879 ms).
   - Se ejecutaron `pm grant ... ACCESS_FINE_LOCATION`, `cmd package compile -m speed -f` y `pm disable-user launcherx`.
   - Mensaje final del script: `Home now resolves to com.gathin.tvhome/.MainActivity`.
   - `InputKeyService` quedó añadido a `enabled_accessibility_services` con `accessibility_enabled 1`.

### Verificación en hardware
- Wi-Fi: después de desactivar `setupwraith`, `mWifiInfo` seguía mostrando `KIRA9142-5GHz` con IP 192.168.1.13.
- Tecla Home (`input keyevent KEYCODE_HOME`): `topResumedActivity=com.gathin.tvhome/.MainActivity`.
- Captura del Home:
  - Saludo «Buenos días» y reloj 7:05 a. m.
  - Botones Entradas, `KIRA9142-…` y Ajustes.
  - Dock con Netflix, Prime Video, YouTube y la app de Ajustes, con el brillo rojo de Netflix.
- `logcat -d -b crash` sin entradas.
- `pm list packages -d`: 8 paquetes, los 6 de la lista y los 2 de fábrica.

### No verificado / atajos de prueba
- Las teclas se enviaron con `input keyevent`, no con el control.
- **Sin comprobar con el control:**
  - Botón Input y cambio a HDMI.
  - Voz y «Hey Google».
  - Chromecast.
  - Control desde el teléfono.
  - Botones de acceso directo del control.
  - Abrir las apps del dock.
- No se comprobó que el Wi-Fi se mantenga después de reiniciar el TV. En Kalley R10G sí se mantuvo; aquí no se ha visto.

### Cambios residuales en el entorno
| Cambio | Dónde | Cómo revertirlo |
|---|---|---|
| 6 paquetes desactivados (lista) | TV, usuario 0 | `tools\restore.ps1 -TvHost 192.168.1.13 examples\caixun-4k-ratv3\debloat.txt com.google.android.apps.tv.launcherx` (reproduce el registro de recuperación) |
| `launcherx` desactivado; Home = `com.gathin.tvhome` | TV | El mismo `restore.ps1`. También se puede elegir el Home en **Ajustes del lanzador → Choose default home**, después de reactivar `launcherx` |
| Red guardada `"Hanny "` borrada | TV | **No se recupera** con `restore`: hay que volver a unirse a la red a mano si hace falta |
| `com.gathin.tvhome` instalado, compilado en modo speed, con `ACCESS_FINE_LOCATION` | TV | `adb uninstall com.gathin.tvhome`, después de restaurar el Home de Google |
| `InputKeyService` en `enabled_accessibility_services` | TV, `settings secure` | Quitarlo de la lista. El valor anterior está en `settings-secure.txt` del respaldo |
| **Puerto ADB 5555 abierto sin cifrar** | TV | **Pendiente:** reiniciar el TV lo cierra. Después, comprobar que `nc -vz 192.168.1.13 5555` falla |
| Respaldo y registro de recuperación | `C:\Users\migue\.cleanyourtv\` | Guardar: el registro es la única copia que necesita `restore` |
| `pip` y dependencias en `tools\venv` | PC (en `.gitignore`) | No hace falta revertirlo |

---

## 5. Condiciones de proyecto Android — instalación y verificación

### 5.1 Condición de entorno — conexión ADB que usan los scripts del repo

#### Síntoma
`tools/tv.py` y los scripts que dependen de él (`disable.ps1`, `build-and-install.ps1`, `theme.ps1`) se conectan con `adb_shell` a `TV_HOST:5555`. Este TV solo ofrecía **depuración inalámbrica** con TLS (`_adb-tls-connect._tcp`, puerto variable), que esa biblioteca no usa.

#### Medición por variable
| Vía | Puerto 5555 | Resultado |
|---|---|---|
| `adb` del SDK por TLS (mDNS) | cerrado | Funciona (`adb shell` y `exec-out`) |
| `tools/tv.py` con `tools\venv` sin pip | cerrado | `ModuleNotFoundError: No module named 'adb_shell'` |
| `tools/tv.py` con `tools\venv` reparado | abierto con `adb tcpip 5555` | **Funciona** (`--probe` responde) |

#### Receta
```bash
adb tcpip 5555
tools\venv\Scripts\python.exe -m ensurepip --upgrade
tools\venv\Scripts\python.exe -m pip install -r tools\requirements.txt
tools\venv\Scripts\python.exe -I tools\tv.py --probe
```
La variable `TV_HOST` debe valer `192.168.1.13` antes de ejecutar `tv.py`.

#### Alcance y límites
- Medido en este TV (Android 14, `UKRC.260302.020`) y en este PC (Windows 11, Python 3.11 de `C:\CraftRoot`, que creó el venv sin pip).
- El puerto 5555 queda abierto **hasta reiniciar el TV** y va **sin cifrar**. Hay que cerrarlo al terminar.
- **Con más de un dispositivo conectado**, `adb shell` falla con `more than one device/emulator`. Hay que usar `adb -s <serial>` o desconectar el otro dispositivo.

### 5.2 Condición de entorno — trampas de herramientas medidas en esta sesión

#### Qué se comprobaba
Que las salidas usadas como evidencia fueran fiables.

#### Métodos
| Vía | Para qué sirve | Trampa medida |
|---|---|---|
| PowerShell `adb exec-out screencap -p > archivo.png` | Capturas de pantalla | **Corrompe el PNG**: PowerShell 5.1 reescribe la salida binaria como texto. Usar Bash (`adb exec-out screencap -p > f.png`) y comprobar con `file` que dice `PNG image data, 1920 x 1080` |
| `build-and-install.ps1` dentro de una tubería con `Select-Object -Last N` | Instalar | Pareció colgado durante 10 minutos: el script había terminado, pero el daemon de Gradle heredó la salida y la mantenía abierta. Se arregla con `gradlew --stop`, o desde el principio con `GRADLE_OPTS=-Dorg.gradle.daemon=false` |
| `dumpsys wifi \| grep cname=` | Saber qué app posee cada red | **Una línea `cname=...setupwraith` no dice qué red es.** Hay que emparejar cada `ID: N SSID:` con la línea `cname=` que le sigue. La primera `cname=` del volcado no pertenece a ninguna red guardada, y la red en uso aparece como `* ID: 7`, con asterisco |
| `dumpsys ... \| grep -m1` / `head` | Leer un fragmento | El TV imprime `Failed to write while dumping service ...: Broken pipe`. Es inofensivo: viene de cortar la salida |
| `python -m unittest discover -s tests` | Tests del repo | 7 fallan en Windows **también en `main` sin cambios** (scripts `.sh` de restauración y registro de recuperación). Se comprobó en un worktree aparte de `main`. No los causa este trabajo |

#### Reglas de verificación
1. **Verificar la premisa:** antes de desactivar `setupwraith`, se comprobó qué red poseía y que la red en uso no era suya.
2. **Una salida vacía no es evidencia:** una tubería sin salida no significaba que la instalación siguiera en marcha. Se contrastó con `pm list packages com.gathin`, con el Home resuelto y con la lista de procesos.
3. **Triangular:** el Home se confirmó con `resolve-activity`, `topResumedActivity` y una captura de pantalla.

#### Qué no se pudo comprobar
- Todo lo que necesita el control físico (ver entradas 3 y 4).

#### Cambios residuales en el entorno
- Capturas de trabajo en el directorio temporal de la sesión. No se versionan.

---

## 6. Volumen mantenido que no se repite y botón Input sin efecto

Tipo: bug / diagnóstico. Fecha: 2026-10-24, de 07:27 a 07:46 (UTC-5), después de que el propietario reiniciara el TV (eso cerró el puerto 5555).

### Problema
Con el control «Smart GTV-R Remote» (Bluetooth), mantener pulsado Volumen + o − cambia el volumen un solo paso en lugar de repetir hasta soltar.

### Causa Raíz
1. **`InputKeyService` filtra las teclas.** Se activó en la entrada 4 con `-InputKeyService` y declara `flagRequestFilterKeyEvents`. Mientras un servicio de accesibilidad filtra teclas, Android no genera la repetición de una tecla mantenida.
2. **El servicio no aportaba nada en este TV.** Solo consume `KEYCODE_TV`, y el botón Input de este control envía `KEYCODE_TV_INPUT`. Se había activado por analogía con Kalley R10G, sin medirlo aquí. **Fue un error de método.**

### Diagnóstico
- **Inyectar eventos:** `sendevent` en `/dev/input/event5` da `Permission denied` aunque el shell está en el grupo `input` (lo bloquea SELinux). Se usó la herramienta `hid` del sistema para registrar un control HID virtual de tipo consumer control, que recorre el mismo camino de entrada que el control real.
- **Medición A/B** (mantener Volumen −, informe `0xEA`, 2,5 s), leyendo el volumen con `cmd media_session volume --stream 3 --get`:

  | Prueba | `enabled_accessibility_services` | Volumen |
  |---|---|---|
  | A | `com.gathin.tvhome/.InputKeyService` | 22 → **21** (un paso) |
  | B | vacío | 21 → **0** (repite) |

- **Con el control físico** (servicio desactivado), según el registro de `dumpsys audio`:
  - 07:38:01,8–02,3: `ADJUST_RAISE` cada ~70 ms, índice de 22 a 30.
  - 07:38:02,7–03,5: `ADJUST_LOWER`, de 30 a 21.
- **Qué envía el botón Input** (`getevent -lt` sobre todos los dispositivos): `EV_MSC MSC_SCAN 000c01bb` seguido de `KEY_UNKNOWN`. Realtek lo traduce: `KCR-KeyConverter: usageCode 0xc01bb --> keyCode 178` (`KEYCODE_TV_INPUT`).
- **Qué hace la app de TV de Realtek con esa tecla.** Se desensambló `RtkLiveTv.apk` con `dexdump` y `aapt2` de build-tools 37.0.0:
  - `TvApplication.handleTvInputKey()` arma un broadcast `com.google.global_button.ACTION_LAUNCH_APP` hacia el componente fijo `config_globalButtonLaunchAppReceiver`, que vale `com.google.android.apps.tv.launcherx/...globalbutton.GlobalButtonLaunch_Receiver`.
  - Ese broadcast lleva dentro `VIEW_INPUTS` para `com.google.android.tv.inputplayer`.
  - Con `launcherx` desactivado, el registro muestra `handleTvInputKey() cannot handle intent` y no se abre nada.
- **Prueba con `launcherx` reactivado** (`pm enable`, aprobada por el propietario):
  - `KEYCODE_TV_INPUT` abre `inputplayer/.switcher.SelectInputActivity`. El registro muestra `GlobalButtonLaunch ... inputplayer` y `START ... VIEW_INPUTS`.
  - Pero el Home pasa de inmediato a `launcherx/.home.HomeActivity`, aunque se ejecutó `set-home-activity` y el rol HOME es de `com.gathin.tvhome`. La resolución elige por prioridad (launcherx 2, nuestro launcher 0).
  - `pm disable launcherx/.home.HomeActivity` falla con `SecurityException: Shell cannot change component state`.

### Fix aplicado
- **Entorno:** `InputKeyService` se dejó desactivado (`enabled_accessibility_services` vacío).
- **`launcherx`:** volvió a desactivarse, como pidió el propietario si reclamaba el Home. `set-home-activity` volvió a `com.gathin.tvhome/.MainActivity`.
- **Documentación:** se anotó el hallazgo en la cabecera de `examples/caixun-4k-ratv3/debloat.txt`.
- **Código:** sin cambios.

### Alcance del cambio
- Toca: ajustes del TV y comentarios de la lista.
- Fuera de alcance: modificar `InputKeyService`. Manejar `KEYCODE_TV_INPUT` con ese servicio volvería a romper la repetición de teclas.

### Verificación en hardware
- Volumen mantenido con el control físico: se repite (registro de `dumpsys audio` arriba).
- Después de revertir: el Home resuelve a `com.gathin.tvhome/.MainActivity`, que es la actividad en primer plano. La captura muestra el tema Aurora y el orden del dock que eligió el propietario, sin cambios.

### No verificado / atajos de prueba
- La prueba A/B se hizo con un control HID virtual y la prueba con `launcherx` con `input keyevent`. La del volumen se confirmó luego con el control real; la de `launcherx` no.
- La repetición de las flechas mantenidas con el servicio activo no se midió.

### Cambios residuales en el entorno
- `InputKeyService` desactivado. Para volver a activarlo: `settings put secure enabled_accessibility_services com.gathin.tvhome/.InputKeyService`. No se recomienda.
- Archivos de prueba en `/data/local/tmp/` (`hold.sh`, `hold.json`, `input.json`): ya borrados. Los demás archivos de esa carpeta (`uidump*.xml`, `plugin3.apk`) ya estaban antes de este trabajo.
- El botón Input no hace nada. Para cambiar de entrada se usa el botón **Entradas** del Home.

### Corrige
- Corrige a la entrada 4 («Instalar launcher nuevo con `-InputKeyService`»): en este TV ese servicio no maneja el botón Input y rompe la repetición de teclas mantenidas, así que **no** debe activarse.
- Corrige a la entrada 1, «Qué no se pudo comprobar»: el botón Input envía `KEYCODE_TV_INPUT`, que depende del receptor de `launcherx`.

---

## Historial de Versiones

| Fecha | Versión | Cambios |
|---|---|---|
| 2026-10-24 | 2.3 (versionCode 4, sin cambio) | Sin commit, en la rama `launcher-on-tv-theme-settings`: opciones de tema, fondo, cielo, estadísticas, biblioteca y reordenar en Ajustes del lanzador; temas Aurora y Seis columnas incluidos en el APK; lista `examples/caixun-4k-ratv3/debloat.txt`. Instalado y verificado en el Caixun Sgamer. |
