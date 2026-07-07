# 📅 Smart Scheduling System

A full-featured desktop **class scheduling platform** built in Java, designed to automate one of the most painful problems in academic administration: assigning courses, professors, rooms, and sections into a conflict-free weekly timetable — in seconds instead of days.

![Java](https://img.shields.io/badge/Java-21-orange) ![Swing](https://img.shields.io/badge/UI-Java%20Swing-blue) ![MySQL](https://img.shields.io/badge/Database-MySQL%208-4479A1) ![JDBC](https://img.shields.io/badge/Data%20Access-JDBC-green)

## ✨ Highlights

- **Automated timetable generation** — a constraint-driven scheduling engine that places every section's courses while detecting and avoiding professor, room, and time-slot conflicts.
- **Configurable optimization** — toggle strategies like *Avoid Time Conflicts*, *Balance Professor Load*, *Optimize Room Usage*, and *Respect Lunch Breaks*, plus custom earliest/latest teaching hours.
- **Live generation log** — watch the engine work in real time, with a full report of placements and resolved conflicts.
- **Secure authentication** — login and sign-up system with **SHA-256 password hashing** backed by MySQL.
- **Complete entity management** — full CRUD dashboards for Courses, Professors, Rooms, Sections, and Schedules.
- **At-a-glance dashboard** — stat cards summarizing the whole institution's data the moment you log in.
- **CSV export** — one click sends the generated timetable to a spreadsheet-ready `schedule_export.csv`.
- **Polished custom UI** — a modern green-and-white theme with rounded panels, custom borders, styled dialogs, and card-based navigation, all hand-built on Java Swing.

## 🏗️ Architecture

The project follows a clean separation of concerns:

| Layer | Component | Role |
|---|---|---|
| Presentation | `SchedulingProgram`, `AuthSystem` | Swing UI: dashboards, forms, custom-painted components |
| Business logic | `SchedulingBackend` / `SchedulingBackendImpl` | Interface-driven scheduling engine, conflict detection, CSV export |
| Domain | `ScheduleEntry`, `Course`, `Professor`, `Room`, `Section` | Core data models |
| Persistence | `DBConnection` + JDBC | Singleton MySQL connection with validation and graceful error handling |

Programming the backend against an **interface** (`SchedulingBackend`) keeps the UI fully decoupled from the scheduling logic and database — the engine could be swapped or unit-tested independently.

## 🧠 The Scheduling Engine

The heart of the system: for each section and course, the engine searches candidate day/time/room combinations and validates them against the live schedule —

1. **Professor conflicts** — no instructor is ever double-booked.
2. **Room conflicts** — no two classes share a room at the same time.
3. **Load balancing** — teaching hours are distributed fairly across professors.
4. **Room optimization** — rooms are matched to demand to minimize waste.

It then reports totals of placements and every conflict it had to route around.

## 🛠️ Tech Stack

- **Java 21** with **Swing** (custom-painted rounded components, CardLayout navigation)
- **MySQL 8** with **JDBC** (mysql-connector-j)
- **SHA-256** (java.security) for credential hashing
- **SwingWorker**-style background processing for responsive, cancellable schedule generation

## 🚀 Running It

```bash
# compile
javac -d bin -cp "lib/mysql-connector-j-9.3.0.jar" src/*.java

# launch (login flow)
java -cp "bin;lib/mysql-connector-j-9.3.0.jar" AuthSystem
```

Requires a local MySQL server with a `scheduling_system` database (credentials configured in `DBConnection.java`).

## 📸 Screenshots

<!-- Add screenshots here -->

---

*Built as a portfolio project demonstrating desktop application development, database-backed CRUD design, constraint-based algorithm design, and custom UI engineering in Java.*
