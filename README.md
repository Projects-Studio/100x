# 100x

A task list that won't let you forget. Add a task, set a deadline, pick a reminder
interval (default 10 minutes). 100x sends a notification with a motivational line every
interval until you tap **Mark done**.

Everything runs on your phone. No server, no account, no internet needed.

## Features
- Tasks with a date + time deadline
- Reminder interval per task: 5 / 10 / 15 / 30 / 60 min, or any custom number of minutes
- Choose when nagging starts: right away, or only after the deadline passes
- A reminder always fires exactly at the deadline
- Each notification shows how long until (or how late past) the deadline, plus a random motivational line
- "Mark done" button right in the notification
- Reminders survive phone restarts
- Overdue tasks are highlighted; finished tasks are kept in a collapsible list

## Get the APK (no computer setup needed, about 5 minutes)

1. Sign in at github.com (free account) and create a new repository, e.g. `100x`. It can be private.
2. Click **uploading an existing file** and drag in *everything* from this folder.
   - Some browsers skip folders starting with a dot. If `.github/workflows/build.yml` didn't
     upload, use **Add file → Create new file**, type `.github/workflows/build.yml` as the
     name, and paste the contents of that file.
3. Commit. Open the **Actions** tab; the "Build 100x APK" run takes about 4–6 minutes.
4. When it's green, open the repo's **Releases** (right sidebar) on your phone and download `100x.apk`.
5. Open the file. Android will ask to allow installs from your browser/files app: allow it, then **Install**.

## Or build with Android Studio
Open this folder in Android Studio, connect your phone with USB debugging on, press **Run**.
Or use **Build → Build App Bundle(s)/APK(s) → Build APK(s)**.

## First launch
- Allow notifications when asked.
- If a yellow card appears at the top, tap each button in it. The most important one on most
  phones is **Unrestrict** (battery), otherwise Android may delay reminders while the screen is off.
- Xiaomi / Redmi / POCO, Oppo, Vivo, Realme, OnePlus: also enable **Autostart** for 100x in
  the phone's app settings, and set battery to **No restrictions**. These brands kill background
  alarms aggressively.

## Notes
- Very short intervals (1–4 minutes) may be bundled together by Android when the phone has been
  idle for a long time. 10 minutes is reliable.
- The APK is a debug-signed build, which is normal for personal sideloaded apps. To update, install
  a newer build over the old one; your tasks are kept.
