# Saros security review (core + intellij), November 2026

Scope: the modules that are built today, `core` and `intellij`, plus the jars they ship
(`core/libs/*`, `libs/picocontainer-*`). The `eclipse`, `server`, `lsp` and `stf` modules are not
built and were not reviewed. Line numbers refer to the state after the fixes in this branch.

Severity: **Critical** = remote code execution or credential theft by a remote attacker without user
interaction; **High** = remote attacker with a network position (MITM) or a contact can steal
credentials/data or tamper with state; **Medium** = needs local access, an opt-in setting, or only
leaks data; **Low/Info** = hardening.

## Summary

| # | Severity | Finding | Status |
|---|----------|---------|--------|
| 1 | High | Smack 3 TLS certificate validation is broken (CVE-2014-0363) | Fixed |
| 2 | High | STARTTLS is opportunistic: a MITM can strip TLS and read the password | Fixed |
| 3 | High | XStream allow-list admits any `Collection`/`Map` class and relocated third-party code | Fixed |
| 4 | Medium | Roster pushes from any sender are accepted (CVE-2014-0364) | Fixed |
| 5 | Medium | XMPP passwords are only obfuscated on disk | Partly fixed (file permissions) |
| 6 | Medium | Peer-to-peer SOCKS5 streams are unencrypted | Open (protocol change) |
| 7 | Medium | weupnp parses router XML with DTDs enabled (XXE) | Open, opt-in only |
| 8 | Low | Any XMPP user can open a bytestream to us | Open, documented |
| 9 | Low | Windows root-relative paths (`\foo`, `C:foo`) were not rejected for peer paths | Fixed |
| 10 | Low | Smack debug window shows SASL auth data | Open, opt-in only |
| 11 | Info | Smack 3.4.1 (2013) is unmaintained | Open, migration plan below |

## Findings

### 1. High - Smack 3 TLS certificate validation (CVE-2014-0363) - Fixed

`org.jivesoftware.smack.ServerTrustManager` in `core/libs/smack-3.4.1.jar` walks the chain itself
and does not check basicConstraints/nameConstraints, so any leaf certificate from a public CA can be
used to sign a certificate for any XMPP domain. A MITM gets the XMPP password (SASL PLAIN) and all
traffic.

Fix: `core/src/saros/communication/connection/XMPPTrustManager.java` uses the JDK's PKIX trust
manager (JDK `cacerts` or `javax.net.ssl.trustStore`) and additionally checks that a DNS
subjectAltName matches the configured XMPP domain (the domain the user typed, not the one the server
announces). It is installed through `ConnectionConfiguration.setCustomSSLContext` in
`ConnectionHandler.java:302-308`. Test: `XMPPTrustManagerTest`.

Limits: XmppAddr subjectAltNames and CN-only certificates are not accepted (RFC 6125 discourages
both); self-signed servers now fail unless their certificate is added to the JVM trust store, which
also was the case with Smack's default settings.

### 2. High - opportunistic STARTTLS - Fixed

With "use TLS" set, Smack 3 used `SecurityMode.enabled`: if the server (or a MITM) does not offer
STARTTLS the connection continues in plaintext and the password is sent in clear. IntelliJ always
creates accounts with TLS on (`ConnectButton.java`), so users believed they were protected.

Fix: `ConnectionHandler.java:304` sets `SecurityMode.required`; Smack then aborts with "Server does
not support security (TLS)". Accounts with TLS off still connect without TLS, as configured.

### 3. High - XStream allow-list too broad - Fixed

All peer data (`XStreamExtensionProvider`, every activity and negotiation extension) is deserialized
with `XStreamFactory.getSecureXStream()`. The allow-list already started from
`NoTypePermission.NONE`, but

- `allowTypeHierarchy(Collection.class)` / `(Map.class)` admitted every collection class on the IDE
  classpath (IntelliJ ships hundreds, e.g. `javax.management.openmbean.TabularDataSupport`,
  third-party lazy/transforming maps), which is the classic entry point for gadget chains, and
- `saros.**` also matched `saros.repackaged.picocontainer.**`, a relocated third-party DI container
  whose classes instantiate and invoke other classes reflectively.

`NoTypePermission.NONE` also clears XStream's built-in deny-list, so nothing else stopped these.

Fix: `XStreamFactory.java:56` only admits collection types from `java.util.*`;
`XStreamFactory.java:61` denies `saros.repackaged.**`. Test: `XStreamFactoryTest`.

### 4. Medium - roster push spoofing (CVE-2014-0364) - Fixed

The patched `core/patches/org/jivesoftware/smack/Roster.java` applied roster pushes from any sender,
so any XMPP user could add, rename or remove contacts in the local roster (contact spoofing ahead of
a session invitation). Fix: `Roster.java:816` only accepts pushes without `from` or from our own bare
JID (RFC 6121 2.1.6). Test: `org.jivesoftware.smack.RosterTest`.

### 5. Medium - password storage - Partly fixed

`XMPPAccountStore` stores accounts AES-encrypted in `~/.saros/config.dat`, but the key is
`System.getProperty("user.name")` (`ContainerContext.java:236`), the IV is all zeroes
(`XMPPAccountStore.java:650`) and the default key is `"Saros"` (`XMPPAccountStore.java:56`). This is
obfuscation: anyone who can read the file can decrypt it.

Fix in this branch: the file is chmod 600 on every save (`XMPPAccountStore.java:220`, test
`testAccountFileIsOnlyAccessibleByOwner`), so other local users cannot read it on Linux/macOS.

Remaining: store the password in the IDE's credential store (IntelliJ `PasswordSafe`, which uses the
OS keychain) and keep only non-secret account data in the file. This needs an interface in `core`
that `intellij` implements plus a migration of existing files; it was left out to keep this change
small.

### 6. Medium - unencrypted peer-to-peer streams - Open

Session data (including whole files during project negotiation) goes over direct SOCKS5 bytestreams
(`net/stream/Socks5StreamService.java`) in plaintext. Only IBB (through the server, over TLS) is
encrypted on the wire. Anyone on the network path between two peers can read the shared code. A fix
needs TLS on the bytestream with keys bound to the XMPP identities on both sides, which breaks
compatibility with older Saros clients; do it together with the Smack 4 migration. `core` has a
"force IBB transport" preference, but the IntelliJ UI does not expose it.

### 7. Medium - XXE in weupnp - Open (opt-in only)

`core/libs/weupnp.jar` (a customized snapshot) parses device descriptions from any LAN device that
answers SSDP with `XMLReaderFactory` and default features, i.e. with external entities enabled. A LAN
attacker could read local files. Discovery only runs when a UPnP gateway is configured
(`XMPPConnectionService.java:189`), and the IntelliJ UI offers no way to configure one, so the code
is unreachable in the IntelliJ plugin. Recommendation: remove UPnP from `core` once the Eclipse
plugin is gone for good, or replace the jar by `org.bitlet:weupnp:0.1.4` with a patched parser.

### 8. Low - unsolicited bytestreams - Open

`Socks5StreamService.incomingBytestreamRequest` (`Socks5StreamService.java:702`) and the IBB
service accept streams from any JID, not only from session participants. Data on these streams is
dispatched to the same extension handlers as normal XMPP traffic, which filter by session membership
where it matters; the handlers were not audited one by one. Recommendation: reject stream requests
from JIDs that are neither session participants nor in an ongoing negotiation.

### 9. Low - root-relative peer paths on Windows - Fixed

`PathUtils.normalize` rejected absolute paths and leading `..`, but on Windows `\foo` and `C:foo`
are not "absolute" in Java. Resolving them against a reference point drops the reference point.
The IntelliJ VFS lookup did not follow these paths, so no file outside the project could be written,
but the check now rejects any path with a root (`PathUtils.java:104`, test
`PathUtilsTest.testNormalizeRootRelative`). Archive entries (`DecompressArchiveTask`) and resource
activities both go through this check.

### 10. Low - Smack debug window - Open (opt-in only)

`Connection.DEBUG_ENABLED` (`XMPPConnectionService.java:170`) opens Smack's debugger, which shows
the raw stream including the base64 SASL PLAIN credentials. It is off by default; leave it that way.

### 11. Info - Smack 3.4.1 and other bundled jars

| Component | Version | Known issues | Action |
|-----------|---------|--------------|--------|
| smack / smackx | 3.4.1 (2013, jar in `core/libs`) | CVE-2014-0363, CVE-2014-0364; unmaintained | Mitigated (#1, #4); migrate, see below |
| weupnp | custom snapshot | XXE (#7) | Unreachable in IntelliJ |
| picocontainer | 2.11.2 patched, relocated | none known; unmaintained | Blocked from XStream (#3) |
| xstream | 1.4.21 | none known (CVE-2024-47072 fixed in 1.4.21) | Keep allow-list |
| xpp3 | 1.1.4c | none known; no DTD processing | Keep (needed by Smack 3) |
| libidn | 1.15 | none known | Keep |
| log4j2 | 2.26.1 | none known | - |
| commons-io / lang3 / codec | 2.22.0 / 3.22.0 / 1.23.0 | none known | - |
| junit (tests) | 4.13.2 | CVE-2020-15250 fixed in 4.13.1 | - |
| powermock (tests) | 2.0.9 | unmaintained, needs `--add-opens` | Replace when tests are touched |

## Smack 3 to Smack 4 migration

Is it needed for security? Not urgently: the two published Smack 3 CVEs are mitigated in this branch
without touching the network layer. It is needed in the medium term because Smack 3 gets no fixes,
lacks SCRAM-SHA-1/256 (only PLAIN/DIGEST-MD5 over TLS), has no XEP-0198 stream management and no
modern TLS configuration hooks, and pins old xpp3/xstream usage patterns.

Scope (what touches Smack today): `core/src/saros/net/**` (connection service, SOCKS5/IBB stream
services, STUN/UPnP glue), `communication/**` (chat/MUC, extensions, connection handler),
`misc/xstream/XStreamExtensionProvider` (packet extension provider API), the patched classes in
`core/patches/org/jivesoftware/**`, and IntelliJ UI code that uses `RosterEntry`/`Presence`.

Plan:
1. Put the Smack types behind Saros interfaces where they leak into UI code (roster, presence, JID).
2. Port `XStreamExtensionProvider` to Smack 4's `ExtensionElementProvider`/`IQProvider` and keep the
   wire format byte-identical (cover with round-trip tests against recorded stanzas).
3. Replace `XMPPConnectionService` with `XMPPTCPConnection` (`SecurityMode.required`, default
   `SSLContext`, SCRAM), drop the `core/patches` copies.
4. Port SOCKS5/IBB to Smack 4's bytestream managers; add TLS on SOCKS5 streams (#6) behind a
   protocol version bump.
5. Test against a real server (e.g. Prosody in a container) in CI.
