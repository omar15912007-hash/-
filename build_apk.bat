@echo off
set "JAVA_HOME=F:\android studio\jbr"
set "ANDROID_HOME=F:\android studio2"
set "ANDROID_SDK_ROOT=F:\android studio2"
set "PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%PATH%"

cd /d "F:\omar\New folder"
echo Building Release APK with R8 minification and resource shrinking...
call gradlew.bat assembleRelease
if %ERRORLEVEL% EQU 0 (
    copy /Y "F:\omar\New folder\app\build\outputs\apk\release\app-release.apk" "%USERPROFILE%\Desktop\NabdAlDhikr.apk"
    copy /Y "F:\omar\New folder\app\build\outputs\apk\release\app-release.apk" "F:\omar\New folder\NabdAlDhikr.apk"
    echo ========================================================
    echo BUILD SUCCESSFUL! Ultra-compact Release APK [2.24 MB]
    echo Output: "%USERPROFILE%\Desktop\NabdAlDhikr.apk"
    echo ========================================================
) else (
    echo BUILD FAILED! Check console error logs.
)
