# widget-mi-negocio Specification

## Purpose

An Android AppWidget named "Mi Negocio" that provides at-a-glance daily financial metrics (today's sales + pending balance) on the device home screen. Data is sourced exclusively from Room (`ResumenDiarioDao`) via a WorkManager periodic task. No Supabase calls are made from the widget or its worker.

## Requirements

### W1: AppWidgetProvider

The SHALL exist a class `MiNegocioWidgetProvider : AppWidgetProvider()` in `optoapp/.../widget/` registered in `AndroidManifest.xml` as a `<receiver>` with `<intent-filter>` for `android.appwidget.action.APPWIDGET_UPDATE`.

The provider SHALL use `RemoteViews` — no Jetpack Compose in the widget layout. The widget layout SHALL display:

- **"Hoy: S/ X"** where X = today's `ventas_monto_total` from the latest `ResumenDiarioEntity` for today's date
- **"Por cobrar: S/ Y"** where Y = today's `saldo_pendiente_total` from the same row
- Both values SHALL be formatted with two decimal places (e.g., "S/ 150.00")

#### Scenario: Widget renders today's data

- GIVEN `ResumenDiarioDao` returns a row for today's date with `ventasMontoTotal = 250.00` and `saldoPendienteTotal = 80.00`
- WHEN `MiNegocioWidgetProvider.onUpdate()` builds the RemoteViews
- THEN the widget displays "Hoy: S/ 250.00" and "Por cobrar: S/ 80.00"

#### Scenario: No data for today

- GIVEN `ResumenDiarioDao` returns NO row for today's date
- WHEN the widget builds its RemoteViews
- THEN "Hoy: S/ 0.00" and "Por cobrar: S/ 0.00" are displayed

---

### W2: WorkManager Periodic Refresh

The system SHALL schedule a periodic WorkManager task (`MiNegocioWidgetWorker`) that refreshes the widget data at least every 6 hours. The worker SHALL:

- Use `PeriodicWorkRequestBuilder` with `repeatInterval(6, TimeUnit.HOURS)`
- Inject `ResumenDiarioDao` via Hilt (`@HiltWorker`)
- Call `appWidgetManager.notifyAppWidgetViewDataChanged()` or `updateAppWidget()` to refresh visible RemoteViews

The worker SHALL run on a background thread (no main-thread DB access). The widget SHALL also refresh immediately in `onUpdate()` when first placed or after a system reboot.

#### Scenario: Worker refreshes widget data

- GIVEN a periodic WorkManager task is scheduled
- WHEN the worker runs
- THEN it reads `ResumenDiarioDao` for today's data from Room
- AND the widget's RemoteViews are updated with fresh values

#### Scenario: Worker does not call Supabase

- GIVEN the worker executes
- WHEN it fetches data
- THEN all reads are from Room only
- AND no Supabase client or RPC calls are made

---

### W3: Tap Opens AnalisisNegocioScreen

The widget SHALL respond to tap by opening `AnalisisNegocioScreen`. The `RemoteViews` SHALL set a `PendingIntent` via `setOnClickPendingIntent(R.id.widget_container, pendingIntent)`.

The `PendingIntent` SHALL use `Intent(context, AnalisisNegocioActivity::class.java)` — or directly navigate to the `AnalisisNegocioScreen` composable destination if using Compose Navigation. `FLAG_UPDATE_CURRENT` SHALL be used to ensure the intent updates on reconfiguration.

#### Scenario: Widget tap navigates to AnalisisNegocioScreen

- GIVEN the widget is placed on the home screen
- WHEN the user taps the widget
- THEN `AnalisisNegocioScreen` opens (or becomes visible if already in the back stack)

---

### W4: Minimal Refresh on `onUpdate()`

The `MiNegocioWidgetProvider.onUpdate()` SHALL attempt a one-shot immediate read from Room for all widget instances, rather than waiting for the next WorkManager tick. This ensures the widget shows current data when first placed or after reboot.

If the Room read returns no data for today (e.g., first app launch with no sync), the widget SHALL display zeros gracefully — no error text, no crash.

#### Scenario: onUpdate reads Room immediately

- GIVEN the widget is placed on the home screen for the first time
- WHEN `onUpdate()` is called
- THEN the widget immediately queries `ResumenDiarioDao` (not WorkManager)
- AND displays today's data or zeros if none exists

---

### W5: Hilt Injection

Hilt SHALL be wired for widget injection per the `hilt.android.launcher` artifact. The `MiNegocioWidgetProvider` SHALL use `@AndroidEntryPoint(AppWidgetProvider::class)` and inject the `ResumenDiarioDao` via field injection in the `onUpdate()` path. The WorkManager worker SHALL use `@HiltWorker`.

#### Scenario: Provider injects DAO

- GIVEN the Hilt component graph is initialized
- WHEN `MiNegocioWidgetProvider` is instantiated by the launcher
- THEN `ResumenDiarioDao` is injected successfully
- AND the `onUpdate()` path reads from Room without error

(Note: AppWidgetProvider instantiation by the Android launcher may occur before Hilt is ready. The `@AndroidEntryPoint` and `hilt.android.launcher` artifact handle this timing.)
