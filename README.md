<<<<<<< HEAD
=======
# StudyOS
>>>>>>> f076f5872b6ebb8ac8e2e34e294c721a9fe65657
# 📚 StudyOS — Smart Study Planner

A fully functional **AI-powered study planner** built with **Kotlin + Ktor** backend and a modern web frontend.

---

## 🏗️ Architecture

```
smart-study-planner/
├── src/main/kotlin/com/studyplanner/
│   ├── Application.kt              ← Entry point
│   ├── models/Models.kt            ← All data classes
│   ├── services/
│   │   ├── StudyPlannerService.kt  ← AI logic (subject analysis, scheduling)
│   │   └── DataStore.kt            ← In-memory data store
│   ├── routes/ApiRoutes.kt         ← REST API routes
│   └── plugins/Plugins.kt          ← Ktor plugin configuration
├── src/main/resources/
│   ├── application.conf            ← Ktor config
│   └── static/
│       └── index.html              ← Full web frontend
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 🚀 Running the Project

### Prerequisites
- JDK 17+
- Gradle 8+

### Steps

```bash
# 1. Navigate to project
cd smart-study-planner

# 2. Run the server
./gradlew run

# 3. Open browser
open http://localhost:8080
```

The web app opens automatically at `http://localhost:8080`.

---

## 🌐 REST API Reference

### Health
```
GET  /api/health
```

### Profile
```
POST /api/profile          → Save student profile
GET  /api/profile/{id}     → Get profile
GET  /api/profile/demo     → Get demo profile
```

### Study Plan
```
POST /api/plan/generate    → Generate AI study plan
GET  /api/plan/{studentId} → Get existing plan
POST /api/plan/demo        → Generate demo plan
```

**Request body for `/api/plan/generate`:**
```json
{
  "profile": {
    "id": "",
    "name": "Arjun Kumar",
    "subjects": [
      {
        "id": "sub1",
        "name": "Mathematics",
        "difficulty": 4,
        "hoursPerWeek": 8.0,
        "currentScore": 62,
        "examDate": "2025-06-15"
      }
    ],
    "studyHoursPerDay": 6.0,
    "preferredStudyTime": "morning",
    "learningStyle": "visual",
    "examGoal": "distinction"
  },
  "openaiApiKey": "sk-..." 
}
```

### Sessions
```
GET   /api/sessions/{studentId}              → Get all sessions
PATCH /api/sessions/update/{studentId}       → Mark session complete
```

### Progress
```
GET /api/progress/{studentId}  → Get progress report
```

### Analyze subjects only
```
POST /api/analyze  → Returns subject insights without full plan
```

---

## 🤖 AI Features

### Without OpenAI API Key (built-in ML logic)
- **Weakness score** calculation per subject (based on current score, difficulty, exam date, hours allocated)
- **Smart scheduling** — high priority subjects get more slots and longer sessions
- **Learning style–based** technique recommendations
- **Spaced repetition** scheduling across the week
- **Goal-aware** tips (distinction/merit/pass strategies)

### With OpenAI API Key
- AI-enhanced personalized daily tips via GPT-3.5-turbo
- Paste your `sk-...` key in the Setup page

---

## 📱 Web Frontend Features

| Feature | Description |
|---|---|
| **Setup** | Add subjects, set difficulty, current score, exam dates |
| **Dashboard** | AI motivational message, stats, today's focus, tips |
| **Weekly Schedule** | Day-by-day sessions with ✓ completion tracking |
| **AI Insights** | Subject-by-subject weakness scores, topic suggestions |
| **Progress** | Completion rate, strong/weak subjects, study streak |
| **Offline Mode** | Works without server (client-side plan generation) |

---

## 🔧 Configuration

Edit `src/main/resources/application.conf`:
```hocon
ktor {
  deployment {
    port = 8080      # Change port here
  }
}
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Backend | Kotlin 2.1 + Ktor 3.1 (Netty engine) |
| Serialization | kotlinx.serialization |
| HTTP Client | Ktor CIO (for OpenAI calls) |
| Frontend | Vanilla HTML/CSS/JS (no framework) |
| Fonts | Syne + DM Mono + Instrument Sans |
| Storage | In-memory (ConcurrentHashMap) |

### Extending with a real database
Replace `DataStore.kt` with exposed or kmongo:
```kotlin
// Example with Exposed
implementation("org.jetbrains.exposed:exposed-core:0.44.1")
implementation("org.jetbrains.exposed:exposed-jdbc:0.44.1")
implementation("com.h2database:h2:2.1.214")
```

---

## 📈 Weakness Score Algorithm

```
score = (100 - currentScore) × 0.4        // Low score → high weakness
      + difficulty × 8                     // Higher difficulty = more attention
      + (hoursPerWeek < avg ? 15 : 0)      // Under-studied = more weakness
      + examDateBonus (7d=25, 14d=15, 30d=8)  // Upcoming exam = urgent
```
Score 0–100 → Low: revision, Medium: study, High: urgent focus

---

## 🤝 Contributing / Extending Ideas
- [ ] Firebase / PostgreSQL integration
- [ ] Gamification (XP, badges, leaderboard)
- [ ] WhatsApp/Telegram bot for daily reminders
- [ ] Analytics dashboard with charts
- [ ] Multi-user authentication (JWT)
- [ ] Android app consuming this API (Jetpack Compose)
<<<<<<< HEAD
=======

>>>>>>> f076f5872b6ebb8ac8e2e34e294c721a9fe65657
