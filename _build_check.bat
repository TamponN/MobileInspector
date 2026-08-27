@echo off
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
cd /d C:\Users\user\StudioProjects\MobileInspector
call gradlew.bat :app:compileDebugKotlin --console=plain
echo BUILD_EXIT_CODE=%ERRORLEVEL%
