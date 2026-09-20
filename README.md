# PasswordGuard — check a password offline, on your Android phone

**PasswordGuard** judges a password, passphrase or email address entirely on your
phone. It does not multiply length by character variety and hand you a green bar.
It asks the question a real guesser asks — *how would I reach this string first?* —
and reports the cheapest route it can find: a breach list, a word list, a keyboard
row, a repeated block, a birthday, or a name you typed into the context field.

The difference between those two methods is the app. A meter that scores
`Summer2019` at "59 bits" is wrong by 41 bits — a factor of roughly two trillion —
and the mistake is the same one every online checker makes. PasswordGuard prints
both numbers side by side.

No upload. No account. No key required. Nothing you type is stored.

Build instructions are below. The same page exists as a standalone, machine-
readable document at [`docs/index.html`](docs/index.html) in this repository.

---

## Why this exists

The skill this came from is `email-and-password-best-practices`, and the honest
translation of it onto a phone is not a web service. Checking a candidate password
by pasting it into a website means the first thing you do with a secret is hand it
to a stranger's server. The advice has been the same for years; the tooling people
actually use contradicts it.

So the hard requirement here was that the reading must exist **on the device, with
no network, with no key**. Everything except the optional plain-words pass works in
airplane mode, forever.

## What it actually runs

| Pass | What it does | Needs network? |
| --- | --- | --- |
| Alphabet sizing | Counts which character classes are present and sizes the alphabet from them | No |
| Baseline | `length × log2(alphabet)` — the number a naive meter would print | No |
| Common-list match | Position of the string in the 216 entries this app ships | No |
| Lookalike folding | Re-applies `0→o 1→i 3→e 4→a 5→s 7→t @→a $→s !→i` and re-checks the list | No |
| Affix check | Catches a listed password with a year or a punctuation mark stuck on it | No |
| Word check | Whole string, or word-plus-digits, against 221 embedded English words | No |
| Sequence check | Keyboard-row runs and ascending/descending runs of 4 or more | No |
| Repetition check | Smallest unit that tiles the whole string | No |
| Date check | Eight digits, or separated triples, that parse on a real calendar | No |
| Context check | Against the words *you* supply — surname, birth year, pet, town | No |
| Email reading | Provider behaviours that surprise people, year-in-address, role names, throwaway domains | No |
| Plain-words pass | Optional: sends the **structural summary only** to your own model endpoint | Yes, only if you configure it |

The two embedded corpora are small and that is stated in the app, not just here. A
small list can prove a hit. It can never clear a miss.

## The model, stated so you can argue with it

Every detector proposes its own cost for guessing the string, in bits. The reading
keeps the **smallest**, because that is the route an attacker takes. The `−N bits`
figure on a finding card is *not* a penalty stacked on the others; it says this
route is N bits cheaper than treating the string as random.

| Detector | Route cost | Confidence |
| --- | --- | --- |
| Exact entry on the shipped list | `log2(rank + 1)`, plus 2 if re-cased | reads a value |
| List entry with lookalike swaps undone | `log2(rank + 1) + 3` | reads a value |
| List entry with a prefix or suffix | `log2(216 × 400)` ≈ **16.4** | *possible* |
| One English word | `log2(221)` ≈ **7.8** | *possible* |
| A word plus up to four digits | `log2(221) + log2(2000)` ≈ **18.8** | *possible* |
| Keyboard-row run of *L* | `log2(47 × L)` | reads a pattern |
| Ascending or descending run of *L* | `log2(95 × L)` | reads a pattern |
| String tiled from a unit of length *U* | `U × log2(alphabet) + log2(10)` | reads a pattern |
| An embedded calendar date | `log2(36526 × 6)` ≈ **17.7** | *possible* |
| Equals one of your context words | `log2(10)`, plus `log2(2000)` if digits | reads a value |
| Contains one of your context words | `log2(200)`, plus `log2(2000)` if digits | *possible* |
| Nothing matched | `length × log2(alphabet)` | the fallback |

### Bands

| Budget | Reading |
| --- | --- |
| under 24 bits | Guessable in one sitting |
| 24 – 39 | Weak where it matters |
| 40 – 59 | Workable, not durable |
| 60 – 79 | Solid for one account |
| 80 and up | Strong by any standard we can see |
| unsizeable input | **Inconclusive** |

`Inconclusive` is not decoration. If the string contains a non-ASCII symbol or a
pictograph, the alphabet size is a number this app does not have, so every figure
on the screen would be wrong, and it says so instead of inventing one. Accented
Latin and other scripts are *not* refused — they widen the alphabet by a stated
floor of 50 and the reading is marked conservative.

### Three worked examples

| Input | Naive meter | PasswordGuard | Why |
| --- | --- | --- | --- |
| `password` | 2^37 | **2^1** | second entry on a list everybody carries |
| `Summer2019` | 2^59 | **2^18** | a word from a small list, plus four digits |
| `correct horse battery staple` | 2^122 | **2^122** | nothing matched; length did the work |

The middle row is the one worth internalising: 41 bits of the naive figure were
never real.

## What it will not tell you

**No time-to-crack figure.** Converting bits into years requires assuming somebody
else's hardware, and it silently assumes they are attacking *every* password rather
than yours. Any app that prints "centuries" is making both assumptions and hiding
them. PasswordGuard prints bits and the route that produced them.

**No breach check.** That needs a network call to a database of other people's
leaks. This app will not make it, which means the question "has this already been
exposed?" is genuinely outside its scope.

**No percentage.** There is no probability anywhere in this build, and the bars you
see are thresholds against the published table above.

**No password generation, and no storage.** PasswordGuard is a checker that
forgets. It is not a manager and does not want your other passwords.

## What it cannot see

A password chosen well against a bulk attack is still weak against somebody who
knows you. The context-words field is the only part of this app that gets at that,
and it is an approximation of what one acquaintance could guess, not a model of a
targeted attacker. If you have a public name, a dog with an Instagram account, or a
birthday on LinkedIn, assume a serious attacker against that account has a longer
context list than the one you typed.

## Bring your own key, structure only

Settings holds a base URL, a model name and a key. They go to this app's private
DataStore and nowhere else; the manifest sets `allowBackup=false`, so they cannot
ride out in a cloud backup, a device transfer or an `adb backup`.

The optional pass sends a structural summary: lengths, finding keys, bit deltas. It
does **not** send the input, and findings whose on-screen detail quotes a fragment
of the input are flagged `quotesInput` and withheld from the copy and network paths
entirely. That is the whole point of the design: you can get an explanation out of a
model without ever handing it the thing you were asking about.

Turn the switch off and the app makes zero requests.

## Privacy, stated

- No analytics, no crash reporter, no telemetry, no ad or attribution SDK.
- The input lives in memory for as long as the screen is in the foreground, and is
  wiped when the app stops. It is never written to DataStore, a file, or a log.
- `FLAG_SECURE` is set, so the screen cannot be screenshotted and does not appear
  as a thumbnail in the recents switcher.
- There is deliberately **no share target**. Accepting a shared secret from another
  app would put that app, and the share sheet in between, in charge of the thing
  this screen exists to protect.
- No storage, contacts, accounts or camera permission. Only `INTERNET`, and only
  for the pass you switch on yourself.

One residual risk, stated rather than glossed: Android's text field holds an
immutable `String`, which cannot be zeroed from application code, and a system
autofill service or a hardware keyboard's own cache may see keystrokes. This build
cannot disable autofill at the Compose version it pins. What it does instead is
minimise how long the value is reachable — the wipe on background — and refuse to
persist it anywhere of ours.

## Requirements

- Android 8.0 (API 26) or newer, arm64/x86_64.
- No runtime permission is requested at any point.

## Build from source

```bash
git clone https://github.com/wonghanz/PasswordGuard.git
cd PasswordGuard
export ANDROID_HOME=/path/to/Android/Sdk
./gradlew --no-daemon :app:assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01, compileSdk 35. The debug APK is
signed with the standard Android debug key. There is no backend, so there is
nothing to self-host.

## FAQ

**Does PasswordGuard upload my password?**
No, and there is no path by which it could. The optional AI pass sends a structural
summary — lengths and pattern names — and the fragments that findings print on
screen are withheld from every copy and network path.

**Why does it give me a number instead of "strong / weak"?**
Because "strong" without a stated model is a mood. You get a bit budget, the route
that produced it, and the table above, so a disagreement can be about a specific
row rather than about the app.

**Why won't it tell me how long it takes to crack?**
Because every such figure is a guess about an attacker's hardware multiplied by a
guess about their motivation. The arithmetic is a measurement of nothing, and apps
that print "3 quadrillion years" for `Summer2019` are the reason people trust weak
passwords.

**Is this a password manager?**
No. It stores nothing at all, including after you close it. It is a checker that
forgets.

**Can it tell me whether my password has already leaked?**
No. That needs a query against a breach corpus over the network, which this app
will not do. It checks against a small embedded list, and says plainly that being
absent from a small list proves nothing.

**Why is the "words a guesser could tie to you" field the useful one?**
Because every published corpus is built from mistakes that are nobody in
particular. The attacks that actually land on an individual account use that
person's name, child's name, street and year. Without the context field this app
can only tell you how guessable you are to a stranger.

**What does the email half actually do?**
An email address is not a secret, so it gets no strength band — saying so is most
of the value. What it reads is the things people learn afterwards: that Gmail
ignores dots so your "alias" is one mailbox, that a plus-tag is a label rather than
a shield, that a four-digit group in the local part reads as a birth year, and
whether the domain is one that exists to receive spam.

**Do I need an account or an API key?**
Neither. Everything works offline forever. The key field is for the optional
explanation pass, and it is yours.

## Repo map

```
app/src/main/java/dev/capriguard/passwordguard/
├── MainActivity.kt              routes, FLAG_SECURE, the wipe on background
├── core/Core.kt                 palette, continuous-corner shapes, glass, type ramp
├── core/Settings.kt             DataStore settings and the OpenAI-compatible client
├── audit/Strength.kt            findings model, bands, redacted renderings
├── audit/Known.kt               the 216-entry and 221-entry corpora, keyboard rows
├── audit/Passphrase.kt          the route model — every number in the table above
├── audit/EmailShape.kt          provider behaviours, year and role detection
├── audit/PasswordGuardViewModel.kt  in-memory state, recompute, optional pass
└── ui/Screens.kt                every screen
```

## Related

- [VerifyGuard](https://github.com/wonghanz/VerifyGuard) — what a photo says about how it was made.
- [LeakGuard](https://github.com/wonghanz/LeakGuard) — what a photo says about you.

## Contributing

Issues and pull requests welcome. If you think a route cost is wrong, open one with
the copied reading attached — a specific disagreement about a specific number is
the most useful thing you can file here. Pull requests that raise a score without
adding a detector will be declined.

## License

MIT — see [LICENSE](LICENSE).
