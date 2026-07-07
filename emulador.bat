@echo off
echo ============================================
echo   OptoApp - Emulador + Instalar
echo ============================================

set SDK=C:\Users\usuario\AppData\Local\Android\Sdk
set PATH=%SDK%\emulator;%SDK%\platform-tools;%PATH%

echo.
echo [1/2] Lanzando emulador Pixel_9_Pro_XL...
start /min "" "%SDK%\emulator\emulator.exe" -avd Pixel_9_Pro_XL
echo        Esperando 40s a que arranque...
adb wait-for-device

echo.
echo [2/2] Instalando OptoApp...
call gradlew installDebug

if %errorlevel% equ 0 (
    echo ============================================
    echo   LISTO - App en el emulador
    echo ============================================
) else (
    echo ERROR: No se pudo instalar
)
pause
