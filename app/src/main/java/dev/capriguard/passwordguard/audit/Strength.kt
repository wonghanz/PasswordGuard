package dev.capriguard.passwordguard.audit

/* ------------------------------------------------------------------ model */

/** What kind of observation a finding is. Drives grouping in the UI. */
enum class FindingClass(val label: String) {
    Size("Size"),
    Composition("Composition"),
    Known("Known to guessers"),
    Pattern("Pattern"),
    Context("Tied to you"),
    Hygiene("Hygiene"),
    Benign("No concern"),
}

enum class Tone { Calm, Warm, Alert }

/**
 * One observation about the thing you typed. `delta` is the only number that
 * moves the reading, and every delta is printed in the README as a divisor of
 * the guess budget, so a reader can disagree with a specific row rather than
 * with the app.
 */
data class Finding(
    val key: String,
    val label: String,
    /** What we saw. Never contains the secret itself. */
    val detail: String,
    val klass: FindingClass,
    /**
     * Guess-budget divisor as log2. 8 means "this cuts your budget to a 256th".
     * Positive numbers only: a finding can make a secret easier to guess, never
     * harder, because "harder" is already carried by length and alphabet.
     */
    val delta: Int,
    /** False means the detector matched a shape, not a value — UI says "possible". */
    val certain: Boolean = true,
    /**
     * True when [detail] quotes a fragment of what you typed. Safe to render on
     * this screen, never safe to copy out or send to an endpoint.
     */
    val quotesInput: Boolean = false,
)

enum class Mode { Passphrase, Email }

/**
 * The whole reading. Two numbers are reported and neither is a percentage.
 *
 * `budgetBits` is our estimate of how many tries a stranger needs, stated as a
 * power of two. `naiveBits` is the number a charset-times-length meter would
 * show for the same string; it is included because it is almost always far
 * larger than [budgetBits], and the gap between them is the actual point of this
 * app.
 */
data class Audit(
    val mode: Mode,
    val findings: List<Finding>,
    val length: Int,
    val alphabetSize: Int,
    val budgetBits: Int?,
    val naiveBits: Int?,
    /** Email readings are descriptive; they have no strength band. */
    val band: String,
    val bandTone: Tone,
    /** Set when we refused to pick a band, and why. */
    val inconclusive: String? = null,
    val contextWordsUsed: Int = 0,
) {
    val count: Int get() = findings.count { it.klass != FindingClass.Benign }
    val uncertain: Int get() = findings.count { !it.certain && it.klass != FindingClass.Benign }

    val classified: List<Pair<FindingClass, List<Finding>>>
        get() = FindingClass.entries.map { k -> k to findings.filter { it.klass == k } }.filter { it.second.isNotEmpty() }

    companion object {
        /** Below this, guessing is a rounding error for anyone serious about it. */
        const val BITS_WEAK = 24
        /** Comfortable against a bulk attack, still thin against one aimed at you. */
        const val BITS_WORKABLE = 40
        /** Where a unique, well-built secret for a single account should sit. */
        const val BITS_SOLID = 60
        /** Above this our model stops being able to see the difference. */
        const val BITS_STRONG = 80
    }
}

fun bandOf(bits: Int): Pair<String, Tone> = when {
    bits < Audit.BITS_WEAK -> "Guessable in one sitting" to Tone.Alert
    bits < Audit.BITS_WORKABLE -> "Weak where it matters" to Tone.Alert
    bits < Audit.BITS_SOLID -> "Workable, not durable" to Tone.Warm
    bits < Audit.BITS_STRONG -> "Solid for one account" to Tone.Calm
    else -> "Strong by any standard we can see" to Tone.Calm
}

/**
 * Text for the clipboard. Structured facts only — a detail that quotes a fragment
 * of what you typed is replaced with a marker, so copying this out cannot carry
 * the secret or the address.
 */
fun auditText(a: Audit): String = render(a, forAi = false)

/**
 * Text for the optional model pass. Tighter than the clipboard version: finding
 * keys and labels, no quoted fragments, no input. An endpoint receiving this can
 * explain the reading without ever seeing the thing that was read.
 */
fun aiBrief(a: Audit): String = render(a, forAi = true)

private fun render(a: Audit, forAi: Boolean): String = buildString {
    val what = if (a.mode == Mode.Passphrase) "secret" else "email address"
    appendLine("PasswordGuard — reading of a ${a.length}-character $what")
    appendLine()
    appendLine("Verdict: ${a.band}")
    if (a.mode == Mode.Passphrase) {
        a.budgetBits?.let { appendLine("Our guess budget: about 2^$it tries ($it bits)") }
        a.naiveBits?.let { appendLine("A charset x length meter would claim: 2^$it ($it bits)") }
    } else {
        appendLine("Descriptive reading. An email address is not a secret and gets no strength band.")
    }
    if (a.inconclusive != null) appendLine("Refused to band this because: ${a.inconclusive}")
    appendLine()
    for ((klass, items) in a.classified) {
        if (!forAi) appendLine("${klass.label}:")
        items.forEach {
            val maybe = if (it.certain) "" else " (possible)"
            val d = if (it.delta > 0) " [cuts budget 2^${it.delta}]" else ""
            val head = if (forAi) "- ${it.key}$maybe$d" else "  - ${it.label}$maybe$d"
            appendLine(head)
            if (!forAi) {
                appendLine("        ${if (it.quotesInput) "detail withheld: it quotes what you typed" else it.detail}")
            }
        }
        appendLine()
    }
    if (a.contextWordsUsed > 0) {
        appendLine("Checked against ${a.contextWordsUsed} context word(s) you typed in. Those never left the device either.")
    }
    appendLine("Deltas are divisors of a guess budget and every one of them is listed in the README. These are heuristics, not measurements of how safe you are.")
    appendLine("PasswordGuard gives no time-to-crack figure: it would be a guess about somebody else's hardware.")
    if (forAi) {
        appendLine()
        appendLine("Note: no fragment of the input is included above, deliberately. Do not ask for it.")
    }
}
