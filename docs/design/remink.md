# Design: Remink

> Spec: docs/superpowers/specs/2026-05-19-remink-design.md

## Context

Remink is a greenfield Android app. There is no existing code — this design establishes all structural decisions from scratch. The app targets the Mudita Compact (e-ink, de-googled Android) and shares its stack with CalmCast (Kotlin 1.9+, Jetpack Compose + Material3, Navigation Compose, MVVM + StateFlow, Room, `com.mudita:MMD:1.0.0`). Min SDK 24, Target SDK 34, root package `com.remink`.

---

## Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Dependency injection | Hilt | Consistent with CalmCast stack; reduces boilerplate for ViewModel injection and scoped DB access |
| Navigation model | Navigation Compose for List/Add/Detail; separate `Activity` task for AlarmScreen | AlarmScreen must launch over the lock screen from a `BroadcastReceiver` — starting a Compose NavHost destination from a receiver requires an Activity anyway; keeping it a separate task avoids polluting the main back stack |
| Alarm API | `AlarmManager.setAlarmClock()` exclusively | Min SDK 24 satisfies the API 21+ requirement; `setAlarmClock()` is exempt from Doze and appears in the system next-alarm indicator per spec requirement |
| Repository abstraction | Interface + `ReminderRepositoryImpl` | Enables fake substitution in ViewModel unit tests without in-memory Room |
| Form state holder | `AddReminderViewModel` only (no separate UiState class at first) | The form has four fields; a sealed `UiState` would be premature — a flat StateFlow of a data class suffices |
| WakeLock holder | `AlarmActivity` directly | AlarmActivity owns the screen lifecycle; no need for a separate service |
| Threading | Room queries via `Dispatchers.IO`; ViewModels collect on `Dispatchers.Main` | Standard coroutine pattern; no RxJava |
| Clock abstraction | `Clock` interface with a `SystemClock` impl injected into `AlarmScheduler` and ViewModels | Required to make scheduling validation and alarm-trigger tests deterministic without real time |
| Character limit enforcement | `visualTransformation` on the TextField limited to 200 chars | Hard block in the ViewModel before persistence; UI enforces `maxLength` via `onValueChange` guard |

---

## Package layout

```
com.remink
├── MainActivity.kt                  — single-activity host for Navigation Compose
├── ReminkApplication.kt             — Hilt application class

├── alarm/
│   ├── AlarmActivity.kt             — full-screen over-lock-screen Activity
│   ├── AlarmReceiver.kt             — BroadcastReceiver, receives AlarmManager intent
│   ├── AlarmScheduler.kt            — schedules / cancels exact alarms
│   └── BootReceiver.kt              — reschedules alarms on BOOT_COMPLETED

├── data/
│   ├── db/
│   │   ├── ReminkDatabase.kt        — Room @Database class
│   │   ├── ReminderDao.kt           — DAO interface
│   │   └── Converters.kt            — TypeConverters if needed (none required; all fields are Long/String)
│   ├── model/
│   │   └── Reminder.kt              — Room @Entity
│   └── repository/
│       ├── ReminderRepository.kt    — interface
│       └── ReminderRepositoryImpl.kt

├── di/
│   └── AppModule.kt                 — Hilt @Module: provides DB, DAO, repository, AlarmScheduler, Clock

├── ui/
│   ├── navigation/
│   │   ├── NavGraph.kt              — NavHost, route constants
│   │   └── Routes.kt                — sealed object / string constants for routes
│   ├── list/
│   │   ├── ReminderListScreen.kt    — Compose screen
│   │   └── ReminderListViewModel.kt
│   ├── add/
│   │   ├── AddReminderScreen.kt     — Compose screen + date/time dialogs
│   │   └── AddReminderViewModel.kt
│   ├── detail/
│   │   ├── ReminderDetailScreen.kt  — Compose screen
│   │   └── ReminderDetailViewModel.kt
│   └── theme/
│       ├── Color.kt                 — black/white/disabled-grey palette
│       ├── Type.kt                  — typography scale
│       └── Theme.kt                 — MaterialTheme wrapper, elevation = 0 everywhere

└── util/
    └── Clock.kt                     — interface + SystemClock impl
```

---

## Data model

### `Reminder` entity

Room entity: `@Entity(tableName = "reminders")`

| Field | Kotlin type | Column | Nullable | Notes |
|---|---|---|---|---|
| `id` | `Long` | `id` | No | `@PrimaryKey(autoGenerate = true)`, starts at 0, Room assigns on insert |
| `scheduledAt` | `Long` | `scheduled_at` | No | Unix epoch milliseconds; the AlarmManager fires at this time |
| `message` | `String` | `message` | No | 1–200 characters; enforced at the ViewModel layer before insert |
| `createdAt` | `Long` | `created_at` | No | Populated by the repository at insert time using `Clock.now()` |
| `acknowledgedAt` | `Long?` | `acknowledged_at` | Yes | Null until the alarm is dismissed; non-null means the row is inactive |

No migration is needed for v1 — this is the initial schema. `fallbackToDestructiveMigration()` is acceptable during development; a named migration must be added before any production release.

---

## Interfaces and contracts

### `Clock`

```
interface Clock {
    fun now(): Long   // returns System.currentTimeMillis() in production
}
```

Used by `ReminderRepositoryImpl` (to stamp `createdAt`) and `AddReminderViewModel` (to validate scheduledAt > now).

---

### `ReminderDao`

All suspend functions are called from `Dispatchers.IO` via the repository. The Flow-returning query is observed on the collection site.

```
@Dao interface ReminderDao {
    @Insert
    suspend fun insert(reminder: Reminder): Long  // returns generated id

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM reminders WHERE acknowledged_at IS NULL ORDER BY scheduled_at ASC")
    fun getAllUnacknowledged(): Flow<List<Reminder>>  // not suspend; Room emits on DB changes

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): Reminder?

    @Query("UPDATE reminders SET acknowledged_at = :timestamp WHERE id = :id")
    suspend fun markAcknowledged(id: Long, timestamp: Long)

    @Query("SELECT * FROM reminders WHERE acknowledged_at IS NULL AND scheduled_at > :now")
    suspend fun getFutureUnacknowledged(now: Long): List<Reminder>  // used by BootReceiver
}
```

---

### `ReminderRepository`

```
interface ReminderRepository {
    fun getUnacknowledgedReminders(): Flow<List<Reminder>>
    suspend fun addReminder(scheduledAt: Long, message: String): Long  // returns new id
    suspend fun getById(id: Long): Reminder?
    suspend fun deleteById(id: Long)
    suspend fun markAcknowledged(id: Long)
    suspend fun getFutureUnacknowledged(): List<Reminder>  // used by BootReceiver
}
```

`ReminderRepositoryImpl` holds a `ReminderDao` and a `Clock`. `addReminder` constructs the `Reminder` with `createdAt = clock.now()` and delegates to `dao.insert`. `markAcknowledged` passes `clock.now()` as the timestamp. All suspend methods run on `Dispatchers.IO` internally using `withContext`.

---

### `AlarmScheduler`

Not an interface in v1 (no substitution needed beyond the `AlarmManager` mock at the boundary).

```
class AlarmScheduler(context: Context, alarmManager: AlarmManager) {
    fun schedule(reminderId: Long, scheduledAt: Long)
    fun cancel(reminderId: Long)
}
```

`schedule`: constructs a `PendingIntent` with action `com.remink.ACTION_ALARM`, extra `EXTRA_REMINDER_ID = reminderId`, targeting `AlarmReceiver`. Calls `alarmManager.setAlarmClock(AlarmClockInfo(scheduledAt, showIntent), pendingIntent)`. `showIntent` is a `PendingIntent` pointing to `MainActivity` (opens the app when the user taps the system clock indicator).

`cancel`: constructs the same `PendingIntent` (same request code = `reminderId.toInt()`) and calls `alarmManager.cancel(pendingIntent)`.

Request code is derived from `reminderId.toInt()`. For v1 this is sufficient; a note should be left that request codes wrap at `Int.MAX_VALUE` — not a practical concern for local reminders.

---

### `AlarmReceiver`

`BroadcastReceiver`. Receives `com.remink.ACTION_ALARM`. Extracts `EXTRA_REMINDER_ID` from the intent. Starts `AlarmActivity` with the reminder id as an extra and flag `FLAG_ACTIVITY_NEW_TASK`. Does not perform any DB work itself.

---

### `BootReceiver`

`BroadcastReceiver`. Receives `android.intent.action.BOOT_COMPLETED`. In `onReceive`, uses `goAsync()` to get a `PendingResult`, launches a coroutine on `Dispatchers.IO` to call `repository.getFutureUnacknowledged()`, then iterates and calls `alarmScheduler.schedule(r.id, r.scheduledAt)` for each. Calls `pendingResult.finish()` when done.

`BootReceiver` must be injected with a pre-built `ReminderRepository` and `AlarmScheduler`. Because Hilt does not inject into `BroadcastReceiver` by default without `@AndroidEntryPoint`, both receivers are annotated `@AndroidEntryPoint` and use `@Inject` field injection.

---

### `ReminderListViewModel`

```
@HiltViewModel class ReminderListViewModel @Inject constructor(
    repository: ReminderRepository
) : ViewModel() {

    val reminders: StateFlow<List<Reminder>>
    // Derived from repository.getUnacknowledgedReminders(), converted via stateIn(
    //   scope = viewModelScope,
    //   started = SharingStarted.WhileSubscribed(5_000),
    //   initialValue = emptyList()
    // )
}
```

No events or user-driven actions — this screen is read-only. Navigation to Detail and Add is driven by the Compose screen directly via `NavController`.

---

### `AddReminderViewModel`

```
@HiltViewModel class AddReminderViewModel @Inject constructor(
    private val repository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler,
    private val clock: Clock
) : ViewModel() {

    data class FormState(
        val selectedDate: LocalDate? = null,
        val selectedTime: LocalTime? = null,
        val message: String = "",
        val errorMessage: String? = null,
        val isSaved: Boolean = false
    )

    val formState: StateFlow<FormState>

    fun onDateSelected(date: LocalDate)
    fun onTimeSelected(hour: Int, minute: Int)
    fun onMessageChanged(text: String)   // enforces 200-char cap
    fun onSaveClicked()                  // validates, persists, schedules alarm, sets isSaved = true
}
```

`onSaveClicked` flow: compute `scheduledAt` from date + time as epoch ms; if `scheduledAt <= clock.now()`, set `errorMessage = "Scheduled time has already passed."` and return; otherwise call `repository.addReminder(scheduledAt, message)` which returns the new `id`, then call `alarmScheduler.schedule(id, scheduledAt)`, then set `isSaved = true`. The Compose screen observes `isSaved` and calls `navController.popBackStack()` when it becomes true.

---

### `ReminderDetailViewModel`

```
@HiltViewModel class ReminderDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    val reminder: StateFlow<Reminder?>  // loaded by id from savedStateHandle["reminderId"]
    val isDeleted: StateFlow<Boolean>

    fun onDeleteConfirmed()
    // calls alarmScheduler.cancel(id) then repository.deleteById(id), sets isDeleted = true
}
```

---

### `AlarmActivity`

Not a Compose screen within the NavHost — it is a standalone `ComponentActivity`. It holds a `WakeLock` acquired in `onCreate` and released in `onDestroy` (as a safety net) and on Dismiss tap. Uses `WindowCompat` / `WindowInsetsController` for immersive mode. Sets `window.addFlags(FLAG_SHOW_WHEN_LOCKED or FLAG_TURN_SCREEN_ON or FLAG_KEEP_SCREEN_ON)` on API < 27; uses `setShowWhenLocked(true)` / `setTurnScreenOn(true)` on API 27+.

The activity receives `EXTRA_REMINDER_ID`, loads the `Reminder` via an injected `ReminderRepository` in a coroutine launched in `lifecycleScope`, and renders the alarm UI in Compose. On Dismiss tap: calls `repository.markAcknowledged(id)` via `lifecycleScope`, releases the wake lock, and calls `finish()`.

`AlarmActivity` is injected with `@AndroidEntryPoint`.

---

## Navigation

### Compose NavGraph (hosted in `MainActivity`)

| Route constant | Destination | Notes |
|---|---|---|
| `list` | `ReminderListScreen` | Start destination |
| `add` | `AddReminderScreen` | Navigated to from List via "+" button |
| `detail/{reminderId}` | `ReminderDetailScreen` | `reminderId: Long` passed as nav argument |

`AlarmActivity` is outside the NavGraph. It is started by `AlarmReceiver` with `Intent(context, AlarmActivity::class.java)` and `FLAG_ACTIVITY_NEW_TASK`. It finishes itself on dismiss — no NavController involvement.

### Back stack behaviour

- List → Add: `navigate("add")`. Cancel/back pops back to List without saving.
- List → Detail: `navigate("detail/$id")`. Delete pops back to List.
- Alarm: separate task; pressing back on the Alarm screen does nothing (back is consumed / `onBackPressed` is overridden to no-op — the user must tap Dismiss).

---

## AndroidManifest requirements

### Permissions

| Permission | Reason |
|---|---|
| `android.permission.RECEIVE_BOOT_COMPLETED` | BootReceiver reschedules alarms after reboot |
| `android.permission.USE_FULL_SCREEN_INTENT` | AlarmActivity launches over lock screen |
| `android.permission.WAKE_LOCK` | AlarmActivity keeps screen on |
| `android.permission.SCHEDULE_EXACT_ALARM` | Required on API 31+ for `setAlarmClock()` |
| `android.permission.POST_NOTIFICATIONS` | Required on API 33+ to post a notification as the full-screen-intent back channel |
| `android.permission.VIBRATE` | Omit unless a vibration on alarm fire is explicitly added |

No `INTERNET` or `ACCESS_NETWORK_STATE` permissions are declared.

On API 31+, `SCHEDULE_EXACT_ALARM` requires the user to grant it via system settings. On first launch, the app checks `AlarmManager.canScheduleExactAlarms()` and, if false, shows a single prompt screen directing the user to `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`. This check lives in `MainActivity.onCreate` (or a dedicated `PermissionCheckScreen` composable injected before the NavGraph).

### Receivers

```xml
<receiver android:name=".alarm.AlarmReceiver"
          android:exported="false">
    <intent-filter>
        <action android:name="com.remink.ACTION_ALARM" />
    </intent-filter>
</receiver>

<receiver android:name=".alarm.BootReceiver"
          android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

### AlarmActivity

```xml
<activity android:name=".alarm.AlarmActivity"
          android:showOnLockScreen="true"
          android:turnScreenOn="true"
          android:excludeFromRecents="true"
          android:taskAffinity=""
          android:exported="false" />
```

`taskAffinity=""` ensures AlarmActivity runs in its own task, separate from the main app task.

---

## Sequence diagrams

### Save reminder and schedule alarm

```
User taps Save
  → AddReminderViewModel.onSaveClicked()
      → validate scheduledAt > clock.now()
      → repository.addReminder(scheduledAt, message)   [returns newId]
      → alarmScheduler.schedule(newId, scheduledAt)
          → AlarmManager.setAlarmClock(AlarmClockInfo(scheduledAt, showPendingIntent), alarmPendingIntent)
      → formState.isSaved = true
  → AddReminderScreen observes isSaved, calls navController.popBackStack()
  → ReminderListScreen recomposes (Flow emits new row)
```

### Alarm fires

```
AlarmManager fires at scheduledAt
  → AlarmReceiver.onReceive()
      → extract reminderId from Intent
      → start AlarmActivity(reminderId) with FLAG_ACTIVITY_NEW_TASK
  → AlarmActivity.onCreate()
      → acquire WakeLock
      → set window flags (show over lock screen, turn screen on)
      → load Reminder by id from repository
      → render alarm UI (message + Dismiss button)
  → User taps Dismiss
      → repository.markAcknowledged(id)
      → release WakeLock
      → finish()
  → ReminderListScreen (if in back stack) recomposes (Flow emits updated list, row disappears)
```

### Delete reminder before alarm fires

```
User taps Delete on ReminderDetailScreen
  → confirmation dialog shown
  → User confirms
      → ReminderDetailViewModel.onDeleteConfirmed()
          → alarmScheduler.cancel(id)
              → AlarmManager.cancel(matchingPendingIntent)
          → repository.deleteById(id)
          → isDeleted = true
  → ReminderDetailScreen observes isDeleted, calls navController.popBackStack()
  → ReminderListScreen recomposes (row removed)
```

### Boot completed

```
Device restarts
  → android.intent.action.BOOT_COMPLETED broadcast
      → BootReceiver.onReceive()
          → goAsync() → PendingResult
          → coroutine on Dispatchers.IO:
              → repository.getFutureUnacknowledged()   [scheduled_at > now AND acknowledged_at IS NULL]
              → for each: alarmScheduler.schedule(r.id, r.scheduledAt)
              → pendingResult.finish()
```

---

## Build configuration

### `build.gradle.kts` — key dependencies

```
// Hilt
implementation("com.google.dagger:hilt-android:<version>")
kapt("com.google.dagger:hilt-android-compiler:<version>")

// Hilt Navigation Compose
implementation("androidx.hilt:hilt-navigation-compose:<version>")

// Room
implementation("androidx.room:room-runtime:<version>")
implementation("androidx.room:room-ktx:<version>")
kapt("androidx.room:room-compiler:<version>")

// Navigation Compose
implementation("androidx.navigation:navigation-compose:<version>")

// Compose BOM (Material3, UI, tooling preview)
implementation(platform("androidx.compose:compose-bom:<version>"))
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.ui:ui-tooling-preview")
debugImplementation("androidx.compose.ui:ui-tooling")

// Lifecycle / ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:<version>")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:<version>")

// Mudita MMD
implementation("com.mudita:MMD:1.0.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:<version>")

// Test
testImplementation("junit:junit:4.13.2")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:<version>")
testImplementation("androidx.arch.core:core-testing:<version>")
testImplementation("io.mockk:mockk:<version>")                     // mock AlarmManager boundary

// Instrumented / integration
androidTestImplementation("androidx.test.ext:junit:<version>")
androidTestImplementation("androidx.room:room-testing:<version>")  // in-memory Room
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
androidTestImplementation("com.google.dagger:hilt-android-testing:<version>")
kaptAndroidTest("com.google.dagger:hilt-android-compiler:<version>")
```

Exact version numbers should be resolved against the project's version catalogue (`libs.versions.toml`) once the Gradle project is initialised.

---

## Testing approach

### Unit tests (JVM, no device)

| Subject | Strategy |
|---|---|
| `AddReminderViewModel` | Inject a `FakeReminderRepository` (implements `ReminderRepository`) and a `FakeClock`. Assert that `onSaveClicked()` with a past time sets `errorMessage`; with a valid future time sets `isSaved = true` and calls `repository.addReminder`. |
| `ReminderListViewModel` | Inject `FakeReminderRepository` that exposes a `MutableStateFlow`. Push values in; assert the VM's `StateFlow` reflects them. |
| `ReminderDetailViewModel` | Inject fake; assert `onDeleteConfirmed()` calls both `cancel` on the scheduler and `deleteById` on the repository. |
| `AlarmScheduler` | Inject a mock `AlarmManager` (MockK). Assert `schedule()` calls `alarmManager.setAlarmClock(...)` with the correct epoch ms and pending intent extras; assert `cancel()` calls `alarmManager.cancel(...)`. |

`FakeReminderRepository` is a test-only class in `src/test/` implementing `ReminderRepository`. It holds a `MutableStateFlow<List<Reminder>>` and in-memory mutable lists.

`FakeClock` returns a fixed `Long` that the test controls.

### Instrumented tests (device / emulator)

| Subject | Strategy |
|---|---|
| `ReminderDao` | Use `Room.inMemoryDatabaseBuilder` in `@RunWith(AndroidJUnit4::class)`. Test insert, delete, `getAllUnacknowledged` ordering, `markAcknowledged`, `getFutureUnacknowledged`. These are the characterisation tests that must be written before any additional query logic is added. |
| Alarm trigger (partial) | The actual `AlarmManager` firing is not unit-testable on JVM. The integration test exercises the repository acknowledge flow and BootReceiver logic with an in-memory DB. Actual alarm delivery timing is validated manually on device or via an espresso test with a very short scheduled delay. |
| Compose UI | Use `ComposeTestRule` to assert list ordering, overdue row inversion (background colour), empty state visibility, Save button disabled state, character counter text. |

### What cannot be unit tested

The full alarm-fires-over-lock-screen path requires a running Android system. This is covered by a manual test checklist (or an instrumented test with real `AlarmManager` and a delay of ~10 seconds, flagged as slow and excluded from CI by default with a `@Category(SlowTest::class)` annotation).

---

## Open questions

1. **`com.mudita:MMD:1.0.0` usage** — the spec does not specify what MMD provides or whether it replaces any standard Android component (e.g. custom date picker, e-ink display utilities). Before implementation, confirm what APIs MMD exposes and whether the date/time picker dialogs should use MMD components rather than Material3 dialogs. *Assign to: engineer, resolve before UI work begins.*

2. **`POST_NOTIFICATIONS` and full-screen intent on API 33+** — `USE_FULL_SCREEN_INTENT` on Android 14+ requires a companion notification for the system to honour the full-screen intent. The design assumes a silent notification is posted alongside the AlarmActivity launch. Confirm whether this notification should be visible (e.g. a "Reminder firing" notification in the shade) or whether it can be a low-priority channel. *Assign to: engineer, resolve before AlarmReceiver implementation.*

3. **Alarm request code overflow** — `reminderId.toInt()` is used as the `PendingIntent` request code. If `id` exceeds `Int.MAX_VALUE` (unlikely in practice for a local reminder app, but theoretically possible), the cast silently overflows. A note in code is sufficient for v1; a proper fix (e.g. hash, or sequential int mapped in a table) is deferred. *Can decide during implementation.*

4. **`SCHEDULE_EXACT_ALARM` permission prompt screen** — the spec says "guide the user to grant it if missing, on first launch." This design places the check in `MainActivity.onCreate`. A separate composable destination (`PermissionGateScreen`) may be cleaner and is testable, but adds a route. *Can decide during implementation.*

5. **Two alarms firing close together** — the spec acknowledges this is out of scope for v1 but says "the system must not crash." The current design starts `AlarmActivity` with `FLAG_ACTIVITY_NEW_TASK` and no `FLAG_ACTIVITY_SINGLE_TOP`. Two AlarmActivity instances will stack. This is safe (each finishes independently) but untested. Confirm this is the acceptable v1 behaviour. *Can decide during implementation.*

---

## Out of scope

All items listed in the spec's Out of scope section, plus:

- Edit-in-place (reminders are delete-and-recreate only)
- Snooze
- Recurring reminders
- Notification-channel management UI
- Any network permission or connectivity-dependent feature
- Instrumented CI test for alarm timing (manual verification only in v1)
