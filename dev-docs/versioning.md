# Versioning

How Saros (core + IntelliJ plugin) is versioned and which versions can work together in one
session. Breaking changes per version are listed in [breaking-changes.md](breaking-changes.md).

## Version format

```
MAJOR.MINOR.PATCH[.QUALIFIER]
```

| Part      | Example        | Meaning                                                    |
|-----------|----------------|------------------------------------------------------------|
| MAJOR     | `0`            | Large rewrites (e.g. a new network stack).                 |
| MINOR     | `4`            | The compatibility line: peers must match on MAJOR.MINOR.   |
| PATCH     | `0`            | Compatible fixes and features.                             |
| QUALIFIER | `DEV-7653d4b51b` | Optional build tag. CI builds use `DEV-<10-char commit sha>`. |

- The version is set in one place: `pluginVersion` in [`intellij/build.gradle.kts`](../intellij/build.gradle.kts).
- The qualifier is appended with `./gradlew -PversionQualifier=<qualifier> ...`. CI passes
  `DEV-<sha>`; local builds without the property have no qualifier (e.g. `0.4.0`).
- The build writes the version into `saros.properties`; Saros reads it from there at startup and
  shares it with its contacts, who compare it before a session starts.

## Compatibility rule

Two Saros instances can start a session when **MAJOR and MINOR are equal**. PATCH and QUALIFIER
are ignored, so a release, a local build and a CI dev build of the same line work together:

| Local                  | Remote                 | Result                 |
|------------------------|------------------------|------------------------|
| `0.4.0`                | `0.4.3`                | compatible             |
| `0.4.0`                | `0.4.0.DEV-0acfbbf9bc` | compatible             |
| `0.4.2.DEV-abc`        | `0.4.0.DEV-def`        | compatible             |
| `0.3.9.DEV-abc`        | `0.4.0`                | refused (local older)  |
| `0.5.0`                | `0.4.1`                | refused (local newer)  |

The rule lives in `Version.determineCompatibilityWith` in
[`core/src/saros/versioning/Version.java`](../core/src/saros/versioning/Version.java) and is
tested in [`VersionManagerTest`](../core/test/junit/saros/versioning/VersionManagerTest.java).
Both peers run the check, so a change to the rule itself only takes effect once **both** sides run
a build that contains it.

## When to bump which part

Because same-MINOR builds connect without any warning, the MINOR version is what protects users
from talking past each other.

- **MINOR** (reset PATCH to 0): any change that an older build of the same line could not
  understand or that changes what it may expect from a peer, e.g.
  - activities, packet extensions or negotiation messages are added, removed or changed,
  - the XStream allow-list or serialized classes change,
  - the stream/transport protocol, encryption or authentication between peers changes,
  - a raised minimum IntelliJ or Java version.
- **PATCH**: everything else (bug fixes, UI changes, refactoring, dependency updates that do not
  change what is sent over the wire).
- **MAJOR**: rewrites that break everything at once, e.g. the planned Smack 3 → Smack 4 migration
  (see [`SECURITY_REVIEW.md`](../SECURITY_REVIEW.md)).

If you are unsure whether a change is breaking, treat it as breaking and bump MINOR.

## Release checklist

1. Decide the new version with the rules above and update `pluginVersion` in
   `intellij/build.gradle.kts`.
2. Add a section for the version to [breaking-changes.md](breaking-changes.md), or move the
   entries collected under "Unreleased" into it. List "none" if nothing breaks.
3. Merge to `master`. CI builds and tests it and uploads the installable plugin zip as the
   `saros-intellij-dev-<sha>` artifact.

The release process in `docs/contribute/processes/release.md` describes the original Eclipse
update-site releases and no longer applies.
