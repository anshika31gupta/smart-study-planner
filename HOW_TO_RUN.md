# 🚀 How to Run StudyOS

## Step 1 — Prerequisites
Make sure you have installed:
- **JDK 17+** → https://adoptium.net/
- **Gradle 8+** → https://gradle.org/install/  
  OR use IntelliJ IDEA which bundles Gradle

## Step 2 — Open in IntelliJ IDEA (Recommended)
1. Open IntelliJ IDEA
2. File → Open → select the `smart-study-planner` folder
3. Wait for Gradle sync to complete
4. Run `Application.kt` → right click → Run

## Step 3 — OR run from Command Line (Windows)
```
cd smart-study-planner
gradlew.bat run
```

## Step 4 — OR run from Command Line (Mac/Linux)
```
cd smart-study-planner
chmod +x gradlew
./gradlew run
```

## Step 5 — Open the App
Once server starts, open browser:
```
http://localhost:8080
```

## ⚠️ Important: gradle-wrapper.jar
If you get a `gradle-wrapper.jar not found` error:
1. Open the project in IntelliJ IDEA — it auto-downloads everything
2. OR run: `gradle wrapper --gradle-version 8.9`

## Troubleshooting
| Error | Fix |
|-------|-----|
| `JAVA_HOME not set` | Install JDK 17 and set JAVA_HOME |
| `Port 8080 in use` | Change port in `application.conf` |
| `Unresolved reference` | Run `gradlew clean build` |
