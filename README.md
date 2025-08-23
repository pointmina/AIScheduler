# AI Scheduler

Android app that converts to-do lists into time-blocked schedules using AI.

## Features

- Generate schedules from task lists using Groq API
- Save and load schedules with Room database
- Edit task times with automatic conflict resolution
- Track progress with completion checkboxes

## Tech Stack

| Category              | Technology                               |
|--------------------|-----------------------------------------------|
| Language        | Kotlin                   |
| Frontend      | Jetpack Compose, Material 3, StateFlow       |
| Architecture   | Clean Architecture, MVVM, Repository Pattern |
| DI | Hilt + KSP                                    |
| Database       | Room (SQLite)                                |
| Networking     | Retrofit + OkHttp + Gson                     |
| AI             | Groq API (Llama 3)                           |


## Architecture

```
┌─────────────────┐
│   Presentation  │ ← Jetpack Compose + ViewModel
├─────────────────┤
│     Domain      │ ← Use Cases + Entities + Repository Interfaces  
├─────────────────┤
│      Data       │ ← Repository Impl + API + Database
└─────────────────┘
```

## Setup

1. Clone repository
2. Add Groq API key to `local.properties`:
```properties
GROQ_API_KEY=your_api_key_here
```
3. Build and run
