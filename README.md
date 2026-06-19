# kotlin-commons

Common Kotlin libraries.

## Versioning

Versions follow `0.MINOR.PATCH` semantics, derived from Git tags (`v<version>`)
by the [axion-release](https://github.com/allegro/axion-release-plugin) plugin —
the version lives in the tags, not in any file. While pre-`1.0.0`:

| Change                                  | Bump  | Example           |
|-----------------------------------------|-------|-------------------|
| Backwards-compatible (additions, fixes) | patch | `0.3.1` → `0.3.2` |
| Backwards-incompatible (breaking)       | minor | `0.3.2` → `0.4.0` |

Only clean (tagged) commits produce non-`SNAPSHOT` versions; everything else is a
`-SNAPSHOT` used only for local development. Check the current version with:

```sh
./gradlew currentVersion
```

## Releasing

Releases are **driven by pull-request labels**. To cut a release, label the PR
and merge it:

| Label           | Effect on merge to `main`             |
|-----------------|---------------------------------------|
| `release:patch` | patch bump (e.g. `v0.3.1` → `v0.3.2`) |
| `release:minor` | minor bump (e.g. `v0.3.2` → `v0.4.0`) |

Merging a labeled PR runs [`release.yml`](.github/workflows/release.yml), which
tags the merge commit and publishes the artifacts to Cloudsmith in one job.
Choose the label at review time — the reviewer judging whether a change is
breaking is the right person to pick patch vs minor. Merging an unlabeled PR
releases nothing.

The same workflow can be run manually from the Actions tab (**Run workflow** →
choose `patch` or `minor`) to release the current `main` on demand.
