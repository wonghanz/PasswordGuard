# Contributing

PasswordGuard is small on purpose: one job, no backend, no storage, and a route
table you can read in one screen. Changes that add surface area need to earn it.

## Before you open a PR

```bash
./gradlew :app:assembleDebug          # must compile
```

That is the gate. There is no emulator lane in this project's CI on purpose.

## The rules the UI holds to

These are not style preferences; they are why the app does not look generated.

- **One accent.** Capri teal (`Palette.CapriDeep` / `CapriBright`). It is spent on
  the primary action, the active state and progress. A second hue in a new screen
  is a rejection.
- **One grey family.** Cool greys only.
- **One radius scale.** Cards 22dp, controls 15dp, chips pill — all superellipses
  from `ContinuousRoundedShape`, never `RoundedCornerShape` for card chrome.
- **Light and dark on day one.** Every new surface must render in both before it
  is done.
- **No emoji in chrome.** Material Symbols only.
- **Numbers get tabular figures.** Use `TextStyle.tnum()` on anything that counts.

## Adding a detector

`audit/Passphrase.kt` is the whole scoring surface. A new route needs:

1. a cost in bits, derived from a count you can state out loud (a list size, an
   alphabet, a calendar), not tuned to produce a pleasing band;
2. a `detail` string a non-expert can act on, phrased as what the pattern means
   rather than as advice;
3. the same figure reflected in the README and `docs/index.html` tables;
4. `certain = false` whenever the detector matched a shape rather than read a
   value out of a corpus;
5. and — critically — `quotesInput = true` if the detail prints any fragment of
   what the user typed. That flag is what keeps the clipboard export and the
   optional model pass free of the secret.

A detector that cannot meet all five belongs in the list marked `Benign`, not
dropped. Silence about what the tool cannot see is the failure mode this project
cares most about.

## Three lines the project does not cross

1. **No percentage, no time-to-crack duration, no "you are at risk" language.**
   The output is a bit budget, the route that produced it, and a band name from a
   published table.
2. **The input never leaves the device, and never outlives the screen.** The
   optional model pass takes structure only. If you want to send a fragment of the
   password to an endpoint — even a hash, even for a breach check — open an issue
   first, because that is the one architectural promise here that is not
   negotiable.
3. **No corpus may be enlarged without stating what it still cannot clear.** A
   longer list makes hits more likely and misses no less meaningless. If you add
   entries, the "absent from this list proves nothing" text has to stay.

## On honesty in output

Absence is not evidence. A password that trips none of these detectors is not a
password that has been checked against the world — it is a password whose shape
this app did not recognise. That is why an empty finding list prints a caveat and
not a compliment, and why `Inconclusive` is a real output rather than a failure
state.

PRs that sharpen the language are welcome. PRs that sharpen the *claim* are not.

## Layout

```
core/       palette, shapes, glass material, type ramp, DataStore, AI client
audit/      findings model, corpora, the route model, email reading, ViewModel
ui/         screens
```

`ui/` reads state from the ViewModel and never touches a repository directly.

## Licensing

By opening a PR you license your contribution under the same MIT terms as the
project.
