@rem
@rem Gradle startup script for Windows.
@rem CATATAN: gradle-wrapper.jar tidak disertakan (file binary) - Android Studio
@rem akan otomatis membuatkannya saat project ini pertama kali dibuka.
@rem
@if "%DEBUG%"=="" @echo off
setlocal

set DIRNAME=%~dp0
set APP_HOME=%DIRNAME%
set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar

if not exist "%CLASSPATH%" (
    echo ERROR: gradle-wrapper.jar tidak ditemukan.
    echo Buka project ini di Android Studio dan biarkan IDE membuatkan file wrapper yang hilang.
    exit /b 1
)

"%JAVA_HOME%\bin\java.exe" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

endlocal
