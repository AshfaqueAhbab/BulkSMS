# BulkSMS

An Android app for sending SMS messages to multiple contacts at once.

## Features

- **Contact list** — add, edit, and delete contacts (stored locally)
- **Bulk SMS** — select multiple contacts and compose a single message that gets sent to all of them
- **Import from phone** — pull contacts directly from the device's address book; duplicates are skipped automatically
- **Search** — filter contacts in real time by name or phone number
- **Select All** — select every contact currently visible in the filtered list in one tap

## How it works

1. Add contacts manually or import them from your phone's address book
2. Use the search bar to filter by name or keyword (e.g. "Client", "VIP")
3. Tap contacts to select them, or hit **Select All** to select all filtered results
4. Tap **Compose SMS** to write your message and send it to everyone selected

## Permissions

| Permission | Why |
|---|---|
| `SEND_SMS` | Send SMS messages to selected contacts |
| `READ_CONTACTS` | Import contacts from the device address book |

Both dangerous permissions are requested at runtime before they are first used.

## Tech stack

- Java, minSdk 24, targetSdk 36
- AndroidX + Material 3
- RecyclerView for the contact list
- Gson + SharedPreferences for local storage
- ContentResolver + ContactsContract for phone contact import

## Building

```bash
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.
