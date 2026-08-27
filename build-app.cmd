@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"

set "GRADLE_VERSION=9.5.0"
set "TOOLS=%CD%\.tools"
set "GRADLE_HOME=%TOOLS%\gradle-%GRADLE_VERSION%"
set "GRADLE_ZIP=%TOOLS%\gradle-%GRADLE_VERSION%-bin.zip"

 echo ==========================================================
 echo   APPLOCK - LOCAL ANDROID BUILD
 echo ==========================================================
 echo.

rem ---- Java ---------------------------------------------------
if not defined JAVA_HOME (
    if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jbr"
)
if not defined JAVA_HOME (
    echo ERROR: JAVA_HOME is not set and Android Studio JBR was not found.
    echo Install Android Studio or JDK 17+, then run this file again.
    exit /b 1
)
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERROR: JAVA_HOME does not contain java.exe: %JAVA_HOME%
    exit /b 1
)
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Java:
"%JAVA_HOME%\bin\java.exe" -version
if errorlevel 1 exit /b 1

rem ---- Android SDK --------------------------------------------
if not defined ANDROID_SDK_ROOT (
    if exist "%LOCALAPPDATA%\Android\Sdk" set "ANDROID_SDK_ROOT=%LOCALAPPDATA%\Android\Sdk"
)
if not defined ANDROID_HOME set "ANDROID_HOME=%ANDROID_SDK_ROOT%"

if not defined ANDROID_SDK_ROOT (
    echo.
    echo ERROR: Android SDK not found.
    echo Install Android Studio and Android SDK Platform 36.
    exit /b 1
)

echo.
echo Android SDK: %ANDROID_SDK_ROOT%

set "SDKMANAGER=%ANDROID_SDK_ROOT%\cmdline-tools\latest\bin\sdkmanager.bat"
if exist "%SDKMANAGER%" (
    echo Checking Android SDK packages...
    echo Accepting Android SDK licenses...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "1..30 | ForEach-Object { 'y' } | & '%SDKMANAGER%' --licenses" >nul 2>nul
    call "%SDKMANAGER%" "platforms;android-36" "build-tools;36.0.0" "platform-tools"
    if errorlevel 1 (
        echo WARNING: sdkmanager could not install/update one or more packages.
        echo The build will continue in case they are already installed.
    )
) else (
    echo NOTE: sdkmanager.bat was not found under cmdline-tools\latest.
    echo The build will continue using the SDK packages already installed.
)

rem ---- Gradle -------------------------------------------------
if not exist "%TOOLS%" mkdir "%TOOLS%"
if not exist "%GRADLE_HOME%\bin\gradle.bat" (
    echo.
    echo Downloading Gradle %GRADLE_VERSION%...
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
      "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -UseBasicParsing 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%GRADLE_ZIP%'"
    if errorlevel 1 (
        echo ERROR: Failed to download Gradle.
        exit /b 1
    )
    echo Extracting Gradle...
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
      "Expand-Archive -LiteralPath '%GRADLE_ZIP%' -DestinationPath '%TOOLS%' -Force"
    if errorlevel 1 (
        echo ERROR: Failed to extract Gradle.
        exit /b 1
    )
)

rem ---- Build --------------------------------------------------
echo.
echo Building debug APK...
call "%GRADLE_HOME%\bin\gradle.bat" --no-daemon clean assembleDebug
if errorlevel 1 (
    echo.
    echo BUILD FAILED.
    exit /b 1
)

set "APK=%CD%\app\build\outputs\apk\debug\app-debug.apk"
echo.
echo ==========================================================
echo BUILD SUCCESSFUL
if exist "%APK%" (
    echo APK: %APK%
    explorer /select,"%APK%" >nul 2>nul
) else (
    echo APK output folder: %CD%\app\build\outputs\apk\debug
)
echo ==========================================================
exit /b 0
