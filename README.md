# ClassManager Android

A coaching-class manager for Android: student roster with batches, batch-wise
daily attendance, test marks with per-student progress, and fee tracking with
a defaulter list. Fully offline-first — everything lives in a local Room
database on the phone, so it works in a classroom with no internet at all.

This is the Android port of the ClassManager web app (2026-10-20), built for
coaching classes and tuition centres that want the same register on a phone.

## Features

- **Dashboard** — active students, fees collected, total dues, today's
  attendance percentage, and the five largest defaulters for follow-up.
- **Students** — roster with search (name / phone / batch), add and edit,
  hide instead of delete (history is never lost), and a detail screen with
  attendance %, average test score, fee dues, and payment history.
- **Attendance** — pick a batch and a date, then tap Present / Absent per
  student. Every tap saves immediately; one record per (student, date), so
  re-marking a day just replaces the old mark. Back-to-back days are allowed.
- **Tests** — create tests with max marks, then enter per-student marks with
  one Save button per row, so a half-filled sheet never loses work. Scores
  above the test's max are refused, not silently clamped.
- **Fees** — dues per student = whole calendar months from admission through
  the current month x the batch's monthly fee plan, minus payments, clamped
  at zero. Students in a batch with no fee plan show dues as "unknown"
  instead of a fake zero. Largest dues first.
- **Fee plans** — one monthly fee per batch, editable any time. Editing a
  plan changes future dues only; payments are append-only and never
  rewritten, so past receipts stay honest.
- **Receipts** — every payment gets a sequential receipt number (CM-0001,
  CM-0002, …) and a one-page PDF receipt you can share from the student's
  detail screen.
- **Offline-first** — Room database, no account, no server, no internet
  needed. Data stays on the device.

## How to open and run in Android Studio

This project was **not** compiled or tested on a device here (this machine
has no Android SDK). To run it:

1. Open Android Studio and choose **Open**, then select this folder.
2. Let Gradle sync (first sync downloads the Android Gradle Plugin 8.2.2,
   Kotlin 1.9.22, Room 2.6.1, and Material 1.11.0).
3. Run on an emulator or a connected phone (min SDK 24, target SDK 34).
4. The app starts on the Dashboard. Add a fee plan for a batch (Fees tab >
   Fee plans) before expecting dues — without a plan, dues show as unknown.

## Direct APK download (once the workflow is added)

After the GitHub Actions workflow below is added to this repo, every push to
`main` builds a debug APK and attaches it to a rolling `apk-latest` release:

`https://github.com/entity-coder/2026-10-29/releases/download/apk-latest/ClassManager-v1.0.apk`

Do not treat that link as working until the `apk-latest` release actually
exists — it only appears after the workflow's first successful build.
Android will show an "unknown developer" prompt for a debug APK; that is
normal outside the Play Store.

## Project structure

- `app/src/main/java/com/classmanager/app/data/` — Room entities
  (`Student`, `FeePlan`, `Payment`, `AttendanceRecord`, `Test`, `Mark`),
  `ClassDao`, `ClassDatabase`, and the pure maths in `FeeMath.kt`
  (months-between, dues, attendance %, average score, receipt numbers).
- `app/src/main/java/com/classmanager/app/ui/` — `MainActivity` with
  bottom navigation, one fragment per tab (Dashboard / Students /
  Attendance / Tests / Fees), detail activities for students and tests,
  `FeePlansActivity`, and `ReceiptPdf` (one-page receipt via PdfDocument).
- `app/src/main/res/` — layouts, strings, Material 3 theme, bottom-nav
  menu, and a vector-only adaptive launcher icon (no PNG assets).

## Interview notes

**What is this project?**
ClassManager Android is an offline coaching-class register: it tracks who
the students are, whether they showed up, how they scored, and what fees
they still owe — the four things a tuition teacher checks every day. It is
an Android port of an earlier web app with the same fee rules, so the
numbers match between the two versions.

**Why Room + Flow instead of plain SQLite or SharedPreferences?**
The data is relational (students have payments, attendance rows, and
marks), so a real database with foreign keys and cascade deletes is the
right shape; SharedPreferences cannot express that. Room's Flow queries
push updates to the UI automatically, which is what makes "tap Present and
the summary updates" work with no manual refresh code.

**Why compute dues at read time instead of storing a balance?**
Dues = months charged x plan − paid. If the balance were stored, every plan
edit or backdated payment would need a recalculation pass that could drift
out of sync. Computing it when the screen renders means a plan edit is
instantly reflected, and payments stay append-only, so the money trail is
auditable. The trade-off is a little more work per render, which is
irrelevant at classroom scale (tens to low hundreds of students).

**Why hide students instead of deleting them?**
Deleting a student would orphan or cascade-delete their payments,
attendance, and marks. Hiding keeps the history intact while removing them
from the roster. A true delete is offered only when the student has no
history at all, and it is guarded by a confirmation dialog.

**Why is attendance one record per (student, date)?**
A unique index on (studentId, date) with `REPLACE` on insert makes
re-marking idempotent: there is no way to end up with two conflicting rows
for the same day, and the UI never needs a separate "edit attendance"
mode — tapping the other button just replaces the mark.

**Trade-offs / limitations**
- No cloud sync or multi-teacher sharing: data lives on one device. A
  backup/export feature (like the CSV export in the GymTrack port) would be
  the natural next step.
- No SDK or device testing was possible in this environment, so the first
  Android Studio run is the real compile check; the code follows the same
  Room / view-binding patterns as the previously shipped Android ports.
- The launcher icon is vector-only (adaptive icon XML), so there is no
  legacy PNG icon for pre-API-26 devices; min SDK is 24, but the adaptive
  icon path covers API 26+, matching the earlier ports.
- Dates are plain yyyy-MM-dd strings with regex validation rather than a
  date picker, to keep the UI code small; a picker would be nicer UX.

**How would I extend it?**
Add a monthly fee-collection report (PDF, like the receipt), SMS/WhatsApp
reminders to defaulters from the Fees tab, a CSV backup/restore, and a
date picker for the attendance and payment dates.
