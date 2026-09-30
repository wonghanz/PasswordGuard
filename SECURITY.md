# Security

PasswordGuard has no server. What is worth reporting is how it handles the secret
you type, what it keeps, and what it puts on the wire when you switch the optional
pass on.

## What is on the device

The API key is written to this app's private storage through Jetpack DataStore.
The manifest sets `allowBackup=false` and `fullBackupContent=false`, so the key is
not included in cloud backups, device transfers, or an `adb backup`. It is never
written to a log, and the settings screen masks it behind an explicit "Show key"
action.

**The password you type is never persisted anywhere.** Not to DataStore, not to a
file, not to a log, not to the clipboard unless you press *Copy reading* — and that
copy has every quoted fragment removed. `PasswordGuardViewModel.wipe()` clears it
when the activity stops.

`FLAG_SECURE` is set on the window: the screen cannot be screenshotted and does not
appear as a thumbnail in the recents switcher.

## What leaves the device

By default: nothing. Every detector — list matching, lookalike folding, affix
checking, word and sequence detection, repetition, calendar dates, context
containment, and the whole email reading — is pure Kotlin over the characters in
the field. **No runtime permission is requested at all.** `INTERNET` is declared
because the optional pass needs it, and with no key configured the app never opens
a socket.

If you turn the plain-words pass on, the app sends a structural summary to the base
URL you configured: lengths, finding keys, bit deltas, band. It does **not** send
the input. Findings whose on-screen detail quotes a fragment of it are flagged
`quotesInput` and withheld from the network path entirely, so the endpoint receives
a description of a shape and never the string.

Deliberately absent, and worth naming so nobody adds it casually: no breach-corpus
lookup. Submitting a prefix of a hash to check exposure is standard practice and
this app still refuses it, because the first thing a password tool should not do is
contact a third party about the password.

## Residual risks, stated

- Android's text field holds an immutable `String`. Application code cannot zero
  it, and it may persist in a heap dump until the process dies. This is why the
  field is wiped on background rather than claimed to be erased.
- A system autofill service, an IME with cloud candidate history, or a hardware
  keyboard's own buffer may observe keystrokes. This build cannot disable autofill
  at the Compose version it pins; it uses `KeyboardType.Password` and no share
  target, which narrows the surface without closing it.
- A device that is already compromised is out of scope. Nothing here defends
  against a keylogger.

## Scope and limits

- The reading is a model of guessability, not a measurement of safety. It cannot
  know what an attacker who knows you specifically would try, beyond the context
  words you choose to type.
- The embedded corpora are small: 216 common passwords, 221 English words. A miss
  against them proves nothing, and the UI says so in the same breath as the hit.
- Inputs containing a non-ASCII symbol or pictograph get no band at all. The
  alphabet cannot be sized, so a number would be invented rather than measured.
- Email readings are descriptive. An address is not a secret and gets no strength
  verdict.

## Reporting

Open a
[private security advisory](https://github.com/wonghanz/PasswordGuard/security/advisories/new)
rather than a public issue. Expect a reply within a week; this is a solo-maintained project, so
"soon" is honest and "24 hours" is not a promise worth making.
