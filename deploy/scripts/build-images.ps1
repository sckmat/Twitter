# UMS
Push-Location ..\..\backend\ums
if (Test-Path .\gradlew.bat) {
    .\gradlew.bat clean bootJar
} else {
    ./gradlew clean bootJar
}
Pop-Location

Copy-Item ..\..\backend\ums\build\libs\ums-1.3.jar app.jar -Force
docker build -t ums:1.3 -f ..\docker\ums.Dockerfile .
Remove-Item app.jar -Force

# Twitter
Push-Location ..\..\backend\twitter
if (Test-Path .\gradlew.bat) {
    .\gradlew.bat clean bootJar
} else {
    ./gradlew clean bootJar
}
Pop-Location

Copy-Item ..\..\backend\twitter\build\libs\twitter-1.3.jar app.jar -Force
docker build -t twitter:1.3 -f ..\docker\twitter.Dockerfile .
Remove-Item app.jar -Force

# UI
docker build --no-cache -f ..\docker\ui.Dockerfile -t ui-login:1.3 ..\..