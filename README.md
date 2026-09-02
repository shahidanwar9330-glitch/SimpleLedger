# Simple Ledger — Maine Liya / Maine Diya

A complete, offline-first Android app (Kotlin + Jetpack Compose + Room) for recording simple
personal/business transactions: what you received/took ("Maine Liya") and what you gave/paid
("Maine Diya"). No accounting jargon, no cloud, no VPS — everything lives in a local Room/SQLite
database on the phone.

## 1. What was built

- **Dashboard** — Maine Liya / Maine Diya / Mera Balance cards, + New Transaction, Recent Transactions.
- **New Transaction** — big LIYA/DIYA toggle, person picker (type-ahead, auto-creates new person),
  amount (validated, safe minor-unit math), currency, date picker, category picker (with inline
  "add new category"), note, payment method, reference. Duplicate-submit protection built in.
- **People / Accounts** — searchable list with per-person Liya/Diya/Balance; tap into a person to
  see their full running history; Add/Edit/Delete person (with confirmation).
- **Transaction Detail** — full detail view, Edit, Delete (with confirmation dialog).
- **Transactions tab** — search by person/category/note/reference, quick filters (All/Liya/Diya),
  category chips.
- **Reports** — Today / This Week / This Month / Custom range, totals + category breakdown.
- **Settings** — Backup Data / Restore Data (JSON file via Android's file picker, with a
  confirmation warning before restore overwrites anything), Last Backup timestamp, category
  management, light/dark/system theme, default currency.
- **Money safety** — all amounts are stored as `Long` minor units (e.g. paisa) derived via
  `BigDecimal`, never `Float`/`Double`, so totals can't drift from floating-point rounding.
- **Empty states, loading states, and confirmation dialogs** are implemented throughout.
- **Architecture is layered** (UI → ViewModel → Repository → Room DAOs) specifically so a future
  sync/API layer, or the WhatsApp/WeChat AI-detection pipeline described in the spec, can be
  added underneath `LedgerRepository` without touching any screen.

## 2. Project structure

```
SimpleLedger/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/ledger/simpleledger/
│       │   ├── SimpleLedgerApp.kt         # manual DI container (no Hilt, keeps build simple)
│       │   ├── MainActivity.kt
│       │   ├── data/
│       │   │   ├── SettingsPrefs.kt       # SharedPreferences: last backup, theme, currency
│       │   │   ├── db/
│       │   │   │   ├── entities/          # PersonEntity, CategoryEntity, TransactionEntity
│       │   │   │   ├── dao/               # PersonDao, CategoryDao, TransactionDao
│       │   │   │   ├── AppDatabase.kt
│       │   │   │   └── Converters.kt
│       │   │   ├── repository/LedgerRepository.kt   # single source of truth for the UI
│       │   │   ├── backup/BackupManager.kt          # JSON export/import (SAF file picker)
│       │   │   └── model/TransactionType.kt         # LIYA / DIYA
│       │   ├── ui/
│       │   │   ├── theme/                 # Color.kt, Type.kt, Theme.kt (light + dark)
│       │   │   ├── navigation/            # Screen.kt, NavGraph.kt (bottom nav + all routes)
│       │   │   ├── components/Components.kt   # cards, transaction row, dialogs, empty state
│       │   │   ├── dashboard/
│       │   │   ├── newtransaction/
│       │   │   ├── transactions/          # search & filter list
│       │   │   ├── transactiondetail/
│       │   │   ├── people/
│       │   │   ├── persondetail/
│       │   │   ├── addperson/
│       │   │   ├── reports/
│       │   │   └── settings/
│       │   └── util/Money.kt, DateUtils.kt
│       └── res/ (theme fallback, strings, launcher icon, FileProvider paths)
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 3. How to build/run it

This project was written directly as source files in a sandbox that has **no Android SDK, no
emulator, and no network access to Google's Maven repository** — so I could not run
`./gradlew build` myself to compile-verify it end to end. Everything above was written carefully
by hand for correctness (consistent imports, matching Compose/Room/Nav APIs for the pinned
library versions), but please treat the first build in Android Studio as the real verification
step, the way requirement #20 in your spec asks for.

To build it:

1. Open the `SimpleLedger/` folder in **Android Studio (Koala or newer)**. Android Studio will
   generate the Gradle wrapper automatically on first open (I could not ship the wrapper's binary
   `gradle-wrapper.jar` myself since this sandbox can't reach `services.gradle.org`).
2. Let Gradle sync — it will download Compose BOM 2024.06.00, Room 2.6.1, Navigation 2.7.7, KSP,
   and Gson from Google/Maven Central.
3. Run on a device or emulator with **minSdk 24+**.
4. First launch seeds the 10 default categories automatically (Payment, Purchase, Sale, Loan,
   Advance, Refund, Commission, Expense, Investment, Other).

If you'd rather use the command line: `cd SimpleLedger && gradle wrapper --gradle-version 8.7`
once (to generate the wrapper), then `./gradlew assembleDebug`.

## 4. What is implemented vs. what still needs your review

Implemented and functionally complete per the spec: dashboard, new transaction flow, person
accounts with balances, categories (default + custom), transaction CRUD with confirmation on
delete, search/filter, reports by period with category totals, JSON backup/restore with
confirmation before overwrite, light/dark theme, bottom navigation, offline-first (nothing in the
core app calls the network).

Two things worth your attention before you rely on this in production:

- **Attachment/photo on a transaction**: the field exists in the database (`attachmentUri`) but
  the New Transaction screen doesn't yet wire up the system photo picker to fill it in — this was
  cut to keep the first build's scope manageable. It's a small addition (`ActivityResultContracts
  .PickVisualMedia`) whenever you want it.
- **Database migrations**: the spec asks for careful migration handling. The schema is at version
  1 with no destructive fallback configured for upgrades — meaning if you change the schema later
  you must add a proper `Migration` object, or existing users' data won't open. This is called out
  in `AppDatabase.kt`.
- **Since I couldn't run a real build here**, do a first-build pass in Android Studio and fix any
  small import/version mismatches Gradle surfaces — the architecture and logic are complete, but
  I can't guarantee a zero-warning first compile without the toolchain to check it.

## 5. What remains for future WhatsApp/WeChat integration

The spec's future pipeline (WhatsApp/WeChat → Message Reader → AI Extractor → Detected
Transaction → User Approval → Ledger Database) was intentionally **not built**, per your
instructions — but the app is already shaped for it:

- `LedgerRepository.addTransaction(...)` is the single insertion point every future "Approve"
  action would call — no UI screen writes to the database directly.
- A future `AI Detected Transactions` screen would just be a new list + a `DetectedTransaction`
  staging table (not yet created) that a user approves/edits/rejects into a real `TransactionEntity`
  via the same repository method above — nothing before that point (message reading, AI parsing)
  should ever call `addTransaction` directly.
- Because amounts are already minor-unit `Long`s end-to-end, an AI parser only needs to produce
  the same shape (`personName`, `type`, `amountMinor`, `currency`, `date`, `category`) and hand it
  to a new "review" screen — no changes needed to the ledger core.
