# Breaking changes

Changes that break compatibility with earlier builds: between peers in a session, with the IDE,
with XMPP servers, or for people building Saros. Newest first. How versions are chosen and which
versions can work together is described in [versioning.md](versioning.md).

When you make a breaking change, add it under **Unreleased** in the same pull request and bump
the MINOR version (see [versioning.md](versioning.md#when-to-bump-which-part)).

## Unreleased

_Nothing yet._

## 0.4 (IntelliJ 2026.3 port, current `master`)

**Peers:** cannot collaborate with 0.3.x (MINOR differs).

### Session compatibility

- Same MAJOR.MINOR is now enough to start a session, regardless of PATCH or qualifier
  (`7653d4b`). Before that commit, any build with a qualifier (all CI builds) only connected to the
  exact same version. Builds older than `7653d4b` still apply the old rule, so **both peers need
  `7653d4b` or later**.
- Within 0.4, builds from before the security fixes (PR #2) and builds after them were not tested
  together; use builds from `7653d4b` or later on both sides.

### Requirements

- IntelliJ IDEA 2026.3 or newer (since-build 263), or another IDE on the same platform version.
  Older IDEs cannot load the plugin.
- Installing or updating the plugin requires an IDE restart (`require-restart="true"`).

### Network and security (PR #2, see [SECURITY_REVIEW.md](../SECURITY_REVIEW.md))

- The XMPP connection now **requires TLS** and validates the server certificate with the JDK trust
  store. Servers without TLS or with self-signed/invalid certificates can no longer be used.
- Contact list (roster) updates from anyone other than your own server are ignored.
- The account file is readable by its owner only.
- Peer paths that are root-relative on Windows (`\foo`, `C:foo`) are rejected.
- The XStream allow-list for data from peers is narrower; types outside it are rejected.

### Sharing

- A `.idea` directory at the root of a shared directory is no longer shared (`0acfbbf`). It holds
  local IDE settings, e.g. the ones created when a joined directory is opened as a new project.
- The hidden IntelliJ start-screen project ("IntelliJ IDEA Home") is no longer offered as a place
  for shared files.

### Build

- Gradle 9.9.2 and JDK 25 are required to build; bytecode targets Java 21.
- Only `core` and `intellij` are built. The `eclipse`, `server`, `lsp`, `stf` and `stf.test`
  modules remain in the tree but are not built or released; `buildSrc` was removed.
- PMD and STF workflows were removed from CI.
