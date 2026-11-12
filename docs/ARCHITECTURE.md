# Architecture

## Project purpose

Nocturne Kernel Manager is an Android application project targeting the Samsung
Galaxy M11 (SM-M115 family) and Galaxy A11 (SM-A115 family), based on the
Qualcomm Snapdragon 450 platform.

The repository is currently in the **application-foundation phase**. Its purpose
at this stage is to establish a maintainable Android project, a predictable
build, a minimal launch screen, project documentation, and a repeatable
verification workflow.

No kernel-management feature is implemented in this phase.

## Current source layout

```text
app/
├── build.gradle.kts
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   ├── java/id/kazera/nocturne/kernelmanager/
    │   │   ├── MainActivity.kt
    │   │   ├── core/project/ProjectScope.kt
    │   │   └── ui/home/HomeScreen.kt
    │   └── res/
    │       ├── drawable/ic_launcher.xml
    │       └── values/
    │           ├── colors.xml
    │           ├── strings.xml
    │           └── themes.xml
    └── test/
        └── java/id/kazera/nocturne/kernelmanager/
            └── core/project/ProjectScopeTest.kt
```

### Package responsibilities

- **MainActivity**: Android entry point. It configures the system bars and
  attaches the initial screen.
- **core/project**: project target metadata and explicit scope boundaries.
- **ui/home**: the initial static presentation. It must not perform privileged
  commands or access kernel interfaces.
- **res**: application identity, launcher vector, colors, strings, and theme.
- **src/test**: local JVM tests for project-level invariants and scope.

## Planned boundaries for later phases

The following is a design direction, **not code that currently exists**.
Directories should be introduced when a real implementation needs them.

```text
core/
├── platform/       # Android API and device capability abstraction
├── privilege/      # Root-provider detection and command execution boundary
├── kernel/         # Validated kernel interface access
└── ui/             # Shared UI components and theme

domain/             # Platform-independent models and use cases
data/               # Repository implementations and data sources
feature/
└── <feature>/      # A feature's UI and presentation logic
```

Keep Android UI code separate from privileged operations. Device-specific
behavior must be isolated behind interfaces and must report unsupported
capabilities instead of assuming that a sysfs or procfs node exists. Any future
write operation must validate the target, current value, allowed range, and
failure path before it can be exposed to the UI.

## Scope guard

`core/project/ProjectScope.kt` records the target device families and keeps
these foundation-stage capabilities disabled:

- root command execution;
- kernel controls;
- device monitoring.

The unit tests protect those boundaries from accidental activation during
foundation work. Adding a future capability requires an intentional change to
the architecture, implementation, tests, and documentation.

## Build configuration

- Language: Kotlin
- Android Gradle Plugin: 9.4.0
- Gradle Wrapper: 9.6.0
- JDK: 17
- Compile SDK: Android API 36
- Target SDK: Android API 35
- Minimum SDK: Android API 26
- Application ID: `id.kazera.nocturne.kernelmanager`

## Verification

Run locally:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

GitHub Actions does not run on push or pull-request events. Start it manually
from **Actions → Android CI → Run workflow**, selecting `main`.
