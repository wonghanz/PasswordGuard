package dev.capriguard.passwordguard.audit

import kotlin.math.ln

private const val LN2 = 0.6931471805599453

private fun log2(x: Double): Double = if (x <= 0.0) 0.0 else ln(x) / LN2

/**
 * The guess-budget model.
 *
 * Every detector proposes its own way of guessing the string, in bits, and the
 * reading takes the *smallest* — the easiest path we can see. Deltas are therefore
 * not additive; each one answers "how much closer than a random string did this
 * pattern bring a guesser". That is the whole model, and it is why the README can
 * print it as a table.
 *
 * No wall-clock figure is ever produced. Converting bits to seconds requires
 * assuming somebody else's hardware, and that assumption is where the dishonest
 * versions of this screen get their false precision.
 */
object Passphrase {

    /** Baseline: guesses for a uniform random string of this length and alphabet. */
    fun alphabetOf(s: String): Int {
        var size = 0
        if (s.any { it in 'a'..'z' }) size += 26
        if (s.any { it in 'A'..'Z' }) size += 26
        if (s.any { it in '0'..'9' }) size += 10
        if (s.any { it.code in 33..126 && !it.isDigit() && !it.isLetter() }) size += 33
        // Accented Latin and other scripts are still letters. Their real space is
        // larger than 50, so this is a floor and the reading it produces is
        // conservative rather than exact.
        if (s.any { it.code > 126 && (it.isLetter() || it.isDigit()) }) size += 50
        return size
    }

    fun analyze(raw: String, context: List<String> = emptyList()): Audit {
        val s = raw
        if (s.isBlank()) {
            return Audit(
                mode = Mode.Passphrase, findings = emptyList(), length = s.length,
                alphabetSize = 0, budgetBits = null, naiveBits = null,
                band = "Nothing to read", bandTone = Tone.Warm,
                inconclusive = "The field is empty.",
            )
        }

        val lower = s.lowercase()
        val ctx = context.map { it.trim().lowercase() }.filter { it.length >= 2 }
        val findings = mutableListOf<Finding>()
        val routes = mutableListOf<Double>()

        val alpha = alphabetOf(s)
        val exoticSymbols = s.any { it.code > 126 && !it.isLetterOrDigit() }
        val nonAsciiLetters = s.any { it.code > 126 && it.isLetterOrDigit() }

        // ---- baseline -------------------------------------------------------
        if (alpha == 0) {
            return Audit(
                mode = Mode.Passphrase, findings = emptyList(), length = s.length,
                alphabetSize = 0, budgetBits = null, naiveBits = null,
                band = "Inconclusive", bandTone = Tone.Warm,
                inconclusive = "Every character is outside the alphabets this app knows how to size.",
            )
        }
        val naive = s.length * log2(alpha.toDouble())
        routes += naive

        // ---- known corpora --------------------------------------------------
        val exactHit = Known.common.indexOf(lower)
        if (exactHit >= 0) {
            // Re-casing a listed entry is a real mutation cost, but a small one.
            val caseCost = if (s == lower) 0.0 else 2.0
            val bits = log2((exactHit + 1).toDouble()) + caseCost
            routes += bits
            findings += Finding(
                key = "known.exact", label = "On the common list",
                detail = "It is the ${exactHit + 1}th entry of the ${Known.common.size} this app ships. " +
                    "Anyone running a list gets it in that many tries.",
                klass = FindingClass.Known, delta = (naive.toInt() - bits.toInt()).coerceAtLeast(0),
            )
        } else {
            val deleetHit = Known.common.indexOf(Known.deleet(s))
            if (deleetHit >= 0) {
                val bits = log2((deleetHit + 1).toDouble()) + 3.0
                routes += bits
                findings += Finding(
                    key = "known.leet", label = "A listed password in disguise",
                    detail = "Undoing the usual lookalike swaps (0→o, @→a, 1→i, 3→e) lands on entry " +
                        "${deleetHit + 1} of the common list. Guessers try that folding first.",
                    klass = FindingClass.Known, delta = (naive.toInt() - bits.toInt()).coerceAtLeast(0),
                )
            }
            findings += Finding(
                key = "known.miss", label = "Not on the shipped list",
                detail = "Absent from ${Known.common.size} embedded entries. That is not the same as absent " +
                    "from a breach corpus — this list is far smaller than any real one.",
                klass = FindingClass.Benign, delta = 0, certain = false,
            )
        }

        // ---- a listed password with something on the end --------------------
        if (exactHit < 0) {
            val onListWithAffix = Known.common.any {
                it.length >= 4 && (lower.startsWith(it) || lower.endsWith(it))
            }
            if (onListWithAffix) {
                val bits = log2(Known.common.size.toDouble() * 400.0)
                routes += bits
                findings += Finding(
                    key = "known.mutation", label = "A listed password plus an affix",
                    detail = "Strip the characters on one edge and what is left is on the common list. Appending " +
                        "a year, a number or a punctuation mark to a known password is the most productive " +
                        "mutation there is, so every list entry gets re-tried with a few hundred suffixes and " +
                        "prefixes around it.",
                    klass = FindingClass.Known, delta = (naive.toInt() - bits.toInt()).coerceAtLeast(0),
                    certain = false,
                )
            }
        }

        // ---- dictionary word ------------------------------------------------
        val bareWord = lower in Known.words
        val wordPlusDigits = Regex("^(\\p{L}+)[^\\p{L}\\p{N}]?(\\d{1,4})$").find(s)
            ?.takeIf { it.groupValues[1].lowercase() in Known.words }
        if (bareWord) {
            val bits = log2(Known.words.size.toDouble())
            routes += bits
            findings += Finding(
                key = "word.bare", label = "A single English word",
                detail = "One word out of the ${Known.words.size} we carry. Word lists are older and smaller " +
                    "than password lists, so this is reachable even if the word is unusual-looking.",
                klass = FindingClass.Known, delta = (naive.toInt() - bits.toInt()).coerceAtLeast(0),
                certain = false,
            )
        } else if (wordPlusDigits != null) {
            val bits = log2(Known.words.size.toDouble()) + log2(2000.0)
            routes += bits
            findings += Finding(
                key = "word.digits", label = "A word with a number bolted on",
                detail = "\"${wordPlusDigits.groupValues[1]}\" is in our word list and " +
                    "\"${wordPlusDigits.groupValues[2]}\" is the trailing number. Word-then-digits is the most " +
                    "productive mutation there is, so it is tried early and exhaustively.",
                klass = FindingClass.Known, delta = (naive.toInt() - bits.toInt()).coerceAtLeast(0),
                certain = false, quotesInput = true,
            )
        }

        // ---- sequences ------------------------------------------------------
        if (Known.keyboardRun(s)) {
            val bits = log2(47.0 * s.length)
            routes += bits
            findings += Finding(
                key = "seq.keyboard", label = "A keyboard run",
                detail = "At least four characters in a straight line on one row, forwards or backwards. " +
                    "Every layout has the same handful of rows, so a generator enumerates them rather than guessing.",
                klass = FindingClass.Pattern, delta = (naive.toInt() - bits.toInt()).coerceAtLeast(0),
            )
        }
        runMonotone(s)?.let { (what, len) ->
            val bits = log2(95.0 * len)
            routes += bits
            findings += Finding(
                key = "seq.monotone", label = "An ascending or descending run",
                detail = "$what of $len characters in order. Ordered runs are enumerated, not guessed.",
                klass = FindingClass.Pattern, delta = (naive.toInt() - bits.toInt()).coerceAtLeast(0),
            )
        }

        // ---- repetition -----------------------------------------------------
        smallestUnit(s)?.let { unit ->
            val bits = unit.length * log2(alpha.toDouble()) + log2(10.0)
            routes += bits
            findings += Finding(
                key = "rep.unit", label = "A repeated block",
                detail = "The whole thing is \"${unit.take(12)}\" repeated ${s.length / unit.length} times. " +
                    "Only the unit carries information; the rest is a count.",
                klass = FindingClass.Pattern, delta = (naive.toInt() - bits.toInt()).coerceAtLeast(0),
                certain = unit.length > 1, quotesInput = true,
            )
        }

        // ---- dates ----------------------------------------------------------
        dateHit(s)?.let { (text, year) ->
            val bits = log2(36526.0 * 6.0)
            routes += bits
            findings += Finding(
                key = "date.shape", label = "A calendar date",
                detail = "\"$text\" parses as a date. Eight digits that any calendar can generate are worth " +
                    "far less than eight random ones, and a guesser aimed at a person tries birthdays first.",
                klass = FindingClass.Pattern, delta = (naive.toInt() - bits.toInt()).coerceAtLeast(0),
                certain = false, quotesInput = true,
            )
            if (year in 1900..2100) {
                findings += Finding(
                    key = "date.year", label = "A year inside it",
                    detail = "The four-digit group reads as $year. If that is a birth year, a graduation year " +
                        "or a join date, it is public information about you, not entropy.",
                    klass = FindingClass.Context, delta = 0, certain = false, quotesInput = true,
                )
            }
        }

        // ---- context words --------------------------------------------------
        if (ctx.isNotEmpty()) {
            val hasDigits = s.any { it.isDigit() }
            val exact = ctx.firstOrNull { it == lower }
            if (exact != null) {
                val bits = log2(10.0) + if (hasDigits) log2(2000.0) else 0.0
                routes += bits
                findings += Finding(
                    key = "ctx.exact", label = "Exactly one of your context words",
                    detail = "The whole entry is one of the ${ctx.size} word(s) you supplied" +
                        if (hasDigits) ", with digits attached." else ".",
                    klass = FindingClass.Context, delta = (naive.toInt() - bits.toInt()).coerceAtLeast(0),
                )
            } else {
                val inside = ctx.filter { it.length >= 3 && lower.contains(it) }.maxByOrNull { it.length }
                if (inside != null) {
                    val bits = log2(200.0) + if (hasDigits) log2(2000.0) else 0.0
                    routes += bits
                    findings += Finding(
                        key = "ctx.inside", label = "Carries one of your context words",
                        detail = "One of your ${ctx.size} word(s) appears inside it with something attached on " +
                            "at least one side. Prefixing and suffixing a known word is the first mutation a " +
                            "targeted guesser tries, so the surrounding characters buy less than they look like " +
                            "they buy.",
                        klass = FindingClass.Context, delta = (naive.toInt() - bits.toInt()).coerceAtLeast(0),
                        certain = false,
                    )
                }
            }
        }

        // ---- characters we cannot size --------------------------------------
        if (exoticSymbols) {
            findings += Finding(
                key = "charset.exotic", label = "A symbol outside ASCII",
                detail = "It contains a non-ASCII symbol or pictograph. How many of those a guesser would try " +
                    "is a number we do not have, so every figure on this screen would be wrong. We say we cannot " +
                    "read it rather than invent a size for the alphabet.",
                klass = FindingClass.Composition, delta = 0,
            )
        }
        if (nonAsciiLetters) {
            findings += Finding(
                key = "charset.accented", label = "Non-ASCII letters counted conservatively",
                detail = "Accented or other-script letters widen the alphabet by a flat 50. The true space is " +
                    "larger, so the budget below is a floor, not a best case.",
                klass = FindingClass.Composition, delta = 0, certain = false,
            )
        }

        // ---- informational composition --------------------------------------
        val classes = listOf(
            s.any { it in 'a'..'z' }, s.any { it in 'A'..'Z' },
            s.any { it in '0'..'9' }, s.any { it.code in 33..126 && !it.isDigit() && !it.isLetter() },
        ).count { it }
        findings += Finding(
            key = "comp.classes", label = "Character classes present",
            detail = "$classes of 4 (lower, upper, digit, symbol), from an alphabet of $alpha. " +
                "We report this and deliberately do not penalise you for a missing class: composition rules are " +
                "deprecated in current guidance because a predictable symbol is worth nothing.",
            klass = FindingClass.Composition, delta = 0, certain = true,
        )
        if (s.length < 8) {
            findings += Finding(
                key = "size.short", label = "Short",
                detail = "${s.length} characters. Length is the only component you control for free; below " +
                    "about 8 no alphabet is large enough to rescue it.",
                klass = FindingClass.Size, delta = 0,
            )
        } else if (s.length < 14) {
            findings += Finding(
                key = "size.mid", label = "Mid-length",
                detail = "${s.length} characters. Enough for a machine-generated secret; thin for something a " +
                    "human is remembering under pressure.",
                klass = FindingClass.Size, delta = 0,
            )
        } else {
            findings += Finding(
                key = "size.long", label = "Long",
                detail = "${s.length} characters. Length outearns every other property here, which is why the " +
                    "advice is four words rather than one word plus a symbol.",
                klass = FindingClass.Size, delta = 0, certain = true,
            )
        }

        // ---- decide ---------------------------------------------------------
        val budget = routes.minOrNull()
        val uncertain = findings.count { !it.certain && it.klass != FindingClass.Benign }
        val live = findings.filter { it.klass != FindingClass.Benign }

        val inconclusive: String? = when {
            exoticSymbols -> "A non-ASCII symbol or pictograph sets the size of the alphabet, and we do not " +
                "know that size. A number built on a guess would look like a measurement, so there is none here."
            budget == null -> "No detector produced a reading."
            else -> null
        }

        val bits = if (inconclusive == null) budget?.toInt() else null
        val (band, tone) = if (bits != null) bandOf(bits) else "Inconclusive" to Tone.Warm
        if (inconclusive == null && bits != null && live.isNotEmpty() && uncertain == live.size) {
            findings += Finding(
                key = "model.allshape", label = "Every finding is a shape match",
                detail = "Nothing here read a value out of a corpus; all $uncertain findings matched patterns. " +
                    "The band is kept because the arithmetic still holds, but it is softer than it looks.",
                klass = FindingClass.Hygiene, delta = 0, certain = false,
            )
        }

        return Audit(
            mode = Mode.Passphrase,
            findings = findings,
            length = s.length,
            alphabetSize = alpha,
            budgetBits = bits,
            naiveBits = if (inconclusive == null) naive.toInt() else null,
            band = band,
            bandTone = tone,
            inconclusive = inconclusive,
            contextWordsUsed = ctx.size,
        )
    }

    /** Longest strictly ascending or descending letter/digit run of 4+. */
    private fun runMonotone(s: String): Pair<String, Int>? {
        var bestLen = 0
        var bestKind = ""
        var bestDir = ""
        var i = 1
        while (i < s.length) {
            val prev = s[i - 1].lowercaseChar()
            val cur = s[i].lowercaseChar()
            val numeric = prev.isDigit() && cur.isDigit()
            val alphabetic = prev in 'a'..'z' && cur in 'a'..'z'
            if (!(numeric || alphabetic)) { i++; continue }
            val step = cur.code - prev.code
            if (step != 1 && step != -1) { i++; continue }
            var j = i
            while (j < s.length) {
                val a = s[j - 1].lowercaseChar()
                val b = s[j].lowercaseChar()
                val sameKind = if (numeric) a.isDigit() && b.isDigit() else a in 'a'..'z' && b in 'a'..'z'
                if (sameKind && b.code - a.code == step) j++ else break
            }
            val len = j - i + 1
            if (len > bestLen) {
                bestLen = len
                bestKind = if (numeric) "digits" else "letters"
                bestDir = if (step == 1) "ascending" else "descending"
            }
            i = j
        }
        return if (bestLen >= 4) "$bestDir $bestKind" to bestLen else null
    }

    /** Smallest repeating unit when the whole string is that unit tiled, else null. */
    private fun smallestUnit(s: String): String? {
        if (s.length < 4) return null
        for (len in 1..s.length / 2) {
            if (s.length % len != 0) continue
            val unit = s.substring(0, len)
            if ((0 until s.length / len).all { s.substring(it * len, it * len + len) == unit }) return unit
        }
        return null
    }

    /** Eight digits (with or without separators) that parse as a real calendar date. */
    private fun dateHit(s: String): Pair<String, Int>? {
        val compact = Regex("\\d{8}").find(s)?.value
        if (compact != null) parseDate(compact)?.let { return compact to it }
        val sep = Regex("\\d{1,4}[-/.,_]\\d{1,2}[-/.,_]\\d{1,4}").find(s)?.value
        if (sep != null) {
            val parts = sep.split('-', '/', '.', '_', ',').map { it.trimStart('0').ifEmpty { "0" } }
            val digits = sep.filter { it.isDigit() }
            parseDate(digits)?.let { return sep to it }
            if (parts.size == 3 && parts[2].length == 4) parseOrdering(parts[0], parts[1], parts[2])
                ?.let { return sep to it }
        }
        return null
    }

    /** Try yyyymmdd, ddmmyyyy and mmddyyyy over an eight-digit run. */
    private fun parseDate(d: String): Int? {
        if (d.length != 8) return null
        val y1 = d.substring(0, 4).toIntOrNull() ?: return null
        val m1 = d.substring(4, 6).toIntOrNull() ?: return null
        val day1 = d.substring(6, 8).toIntOrNull() ?: return null
        if (valid(y1, m1, day1)) return y1
        val day2 = d.substring(0, 2).toIntOrNull() ?: return null
        val m2 = d.substring(2, 4).toIntOrNull() ?: return null
        val y2 = d.substring(4, 8).toIntOrNull() ?: return null
        if (valid(y2, m2, day2)) return y2
        val m3 = d.substring(0, 2).toIntOrNull() ?: return null
        val day3 = d.substring(2, 4).toIntOrNull() ?: return null
        if (valid(y2, m3, day3)) return y2
        return null
    }

    private fun parseOrdering(a: String, b: String, year: String): Int? {
        val y = year.toIntOrNull() ?: return null
        val p = a.toIntOrNull() ?: return null
        val q = b.toIntOrNull() ?: return null
        if (valid(y, p, q) || valid(y, q, p)) return y
        return null
    }

    private fun valid(y: Int, m: Int, d: Int): Boolean {
        if (y < 1900 || y > 2100 || m < 1 || m > 12 || d < 1 || d > 31) return false
        val days = when (m) {
            2 -> if (y % 4 == 0 && (y % 100 != 0 || y % 400 == 0)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
        return d <= days
    }
}
