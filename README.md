# Silo — Community Food Rescue Network

A Java 21 Swing desktop app that records surplus food donations, **ranks them with a hand-written Merge Sort**, schedules volunteer pickups through a **queue**, tracks distribution to recipients, and produces reports and CSV exports. A **stack** powers "Undo last action".

## Run it

Needs **JDK 21+** (check with `javac -version`).

| OS | Command (from this folder) |
|---|---|
| Windows | `build-and-run.bat` |
| Linux / macOS | `./build-and-run.sh` |
| Maven | `mvn package` then `java -jar target/silo-1.0.0.jar` *(see "Not verified" below)* |

Data is saved as text files in the `data/` folder (created on first run). Delete that folder to start fresh. On an empty system the Dashboard offers **Load sample data** (clearly sample records, created through the normal rules).

**Eclipse:** File → Import → Existing Maven Projects → choose this folder. Run `com.silo.ui.SiloApp`.
**VS Code:** open the `Silo-main` folder (the folder containing `pom.xml`) with the Java Extension Pack. Wait for Maven dependencies to resolve, then open `src/main/java/com/silo/ui/SiloApp.java` and click Run. You can also use `build-and-run.bat` (Windows) or `./build-and-run.sh` (macOS/Linux) with Maven installed.

## Screens
Dashboard · Donations · Pickups · Volunteers · Recipients (with distribution history) · Reports (summary, near best-before, distributions, CSV export).

## Rules worth knowing
- Priority order: higher perishability → earlier best-before → shorter distance → lower ID.
- A donation moves **Available → Scheduled → Picked up → Delivered** (or **Cancelled** while available/scheduled). Pickups drive those changes; you can't skip steps.
- A volunteer can't have two scheduled pickups less than 60 minutes apart, can't be deactivated while holding active pickups, and pickups can't be after the best-before date or in the past.
- Distribution can't exceed the quantity left; a donation becomes Delivered when fully distributed, or when you click **Confirm delivered** after at least one distribution.
- Totals are reported **per unit** (kg and pieces are never added together).
- Undo covers: adding a donation / volunteer / recipient, and scheduling a pickup (only while nothing has progressed from them). Undo history resets when the app closes.

## Tests
`mvn test` or `./run-tests.sh` — 27 JUnit 5 tests covering Merge Sort (empty, single, stability, priority rules, random data vs. a reference sort), queue, stack, search, validation, scheduling conflicts, distribution limits, undo, reports, CSV, persistence across restarts.

## Known limitations (honest list)
- **Local by default:** the main application still uses local text files and local account storage. Supabase helper classes exist, but the main UI/service is not yet wired to Supabase for its donation, pickup, volunteer, and recipient data. The `SiloService` / `FileStore` split is the place to swap in a database later.
- No status-history table (the Dashboard shows a recent-activity log instead).
- Not verified: the Maven build (`pom.xml`) — the build environment had no access to Maven Central. The `javac` scripts and the 27 tests were run and pass. Eclipse's Maven import will download JUnit automatically.
- UI was rendered and checked on Linux (virtual display). Windows should look the same apart from using the Segoe UI font.
