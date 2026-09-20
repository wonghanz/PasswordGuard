package dev.capriguard.passwordguard.audit

/**
 * The email half of the skill this app comes from.
 *
 * An email address is not a secret and this panel never treats it as one. What it
 * does read is the things people only find out afterwards: which characters a
 * provider ignores, which digits in the local part read as a birth year, and
 * whether the address is on a domain that is already known to harvesters.
 */
object EmailShape {

    /** Providers that ignore dots and plus-tags in the local part. */
    private val dotIgnored = setOf("gmail.com", "googlemail.com")
    private val plusIgnored = setOf("gmail.com", "googlemail.com", "outlook.com", "hotmail.com", "yahoo.com")
    private val dashIgnored = setOf("yahoo.com", "ymail.com", "rocketmail.com")

    /** Addresses that exist to receive spam and expire on their own. */
    private val disposable = setOf(
        "mailinator.com", "guerrillamail.com", "sharklasers.com", "10minutemail.com",
        "temp-mail.org", "tempmail.com", "yopmail.com", "trashmail.com",
        "throwawaymail.com", "getnada.com", "dispostable.com", "maildrop.cc",
        "mohmal.com", "emailondeck.com", "fakeinbox.com", "tempinbox.com",
    )

    private val roleNames = setOf(
        "admin", "root", "postmaster", "webmaster", "info", "contact", "sales",
        "support", "help", "billing", "abuse", "noreply", "no-reply", "donotreply",
        "office", "hr", "jobs", "careers", "press", "privacy",
    )

    fun analyze(input: String, context: List<String> = emptyList()): Audit {
        val s = input.trim()
        if (s.isEmpty()) {
            return Audit(
                mode = Mode.Email, findings = emptyList(), length = 0, alphabetSize = 0,
                budgetBits = null, naiveBits = null, band = "Nothing to read",
                bandTone = Tone.Warm, inconclusive = "The field is empty.",
            )
        }
        val ctx = context.map { it.trim().lowercase() }.filter { it.length >= 2 }
        val findings = mutableListOf<Finding>()

        val at = s.lastIndexOf('@')
        val local = if (at >= 0) s.substring(0, at) else s
        val domain = if (at >= 0) s.substring(at + 1) else ""
        val bareDomain = domain.substringAfterLast(':').lowercase()

        fun add(
            key: String,
            label: String,
            detail: String,
            klass: FindingClass,
            certain: Boolean = true,
            quotes: Boolean = false,
        ) {
            findings += Finding(key, label, detail, klass, delta = 0, certain = certain, quotesInput = quotes)
        }

        // ---- structure ------------------------------------------------------
        when {
            at < 0 -> add(
                "form.at", "No @ sign",
                "Nothing after an @, so this is not an address any mail system will accept.",
                FindingClass.Hygiene,
            )
            local.isEmpty() -> add("form.local", "Empty local part", "Everything before the @ is missing.", FindingClass.Hygiene)
            domain.isEmpty() -> add("form.domain", "Empty domain", "Everything after the @ is missing.", FindingClass.Hygiene)
            local.startsWith(".") || local.endsWith(".") || local.contains("..") -> add(
                "form.dots", "Leading, trailing or doubled dot",
                "Gmail strips dots entirely, so \".\" and \"..\" collapse into the same mailbox as the plain " +
                    "address. Elsewhere most providers reject it outright.",
                FindingClass.Hygiene,
            )
            s.any { it.isWhitespace() } -> add("form.space", "Contains a space", "Rejected by every provider and by most sign-up forms.", FindingClass.Hygiene)
            else -> add(
                "form.ok", "Well-formed",
                "One @, a local part and a domain. Nothing here stops mail flowing.",
                FindingClass.Benign, certain = false,
            )
        }

        // ---- identifier linkage ---------------------------------------------
        val lowerLocal = local.lowercase()
        yearIn(local)?.let { y ->
            add(
                "id.year", "Carries a four-digit year",
                "\"$y\" reads as a year. When an address carries a birth year, it is a free data point for " +
                    "anyone building a profile, and it is the answer to half the security questions people " +
                    "invent. If it is a graduation or start year, same story with a different date.",
                FindingClass.Context, certain = false, quotes = true,
            )
        }
        if (Regex("\\d{7,}").containsMatchIn(local)) {
            add(
                "id.phone", "Carries a phone-number-shaped run",
                "Seven or more digits in a row is what a number looks like. Harvesters match addresses to " +
                    "numbers on exactly that assumption.",
                FindingClass.Context, certain = false,
            )
        }
        ctx.filter { it.length >= 3 }.maxByOrNull { it.length }?.let { w ->
            when {
                lowerLocal == w -> add(
                    "id.exact", "Is exactly one of your context words",
                    "The local part is a word you told us is yours, with nothing else. Trivially guessable " +
                        "once anyone knows that word — which for a name they usually do.",
                    FindingClass.Context,
                )
                lowerLocal.startsWith(w) || lowerLocal.endsWith(w) -> add(
                    "id.edge", "Starts or ends with one of your context words",
                    "A word you supplied sits on one edge of the local part. The characters on the other side " +
                        "are the only thing narrowing the field, and separators like . _ - barely count.",
                    FindingClass.Context, certain = false,
                )
                else -> add(
                    "id.inside", "Contains one of your context words",
                    "A word you supplied appears inside the local part.", FindingClass.Context, certain = false,
                )
            }
        }

        // ---- provider behaviour ---------------------------------------------
        if (local.contains('+')) {
            val tag = local.substringAfterLast('+')
            if (dotIgnored.contains(bareDomain) || plusIgnored.contains(bareDomain)) {
                add(
                    "prov.plus", "A plus-tag on a provider that ignores it",
                    "\"+$tag\" is delivered to the same mailbox as \"${local.substringBeforeLast('+')}\". That " +
                        "makes it a label for you, not a separate address: a tracker that strips the tag, or " +
                        "simply knows the rule, is back to one identity. Some sign-up forms also reject the " +
                        "plus sign outright.",
                    FindingClass.Hygiene, quotes = true,
                )
            } else {
                add(
                    "prov.plus.unsupported", "A plus-tag on an unknown provider",
                    "\"+$tag\" is only a real second address if this provider supports subaddressing and " +
                        "delivers it. Many treat it as an invalid character and refuse the sign-up.",
                    FindingClass.Hygiene, certain = false, quotes = true,
                )
            }
        }
        if (bareDomain in dotIgnored && local.count { it == '.' } > 0) {
            add(
                "prov.dots", "Dots on a provider that ignores them",
                "The dotted and undotted forms of this address are the same mailbox. The common belief that " +
                    "dots make a Gmail address harder to guess or use as an alias is not true; it just looks different.",
                FindingClass.Hygiene,
            )
        }
        if (bareDomain in dashIgnored && local.contains('-')) {
            add(
                "prov.dash", "A dash on a provider that truncates at it",
                "On this provider the part after the first dash is commonly dropped, so \"name-tag\" arrives at " +
                        "\"name\". It is a label, not a shield.",
                FindingClass.Hygiene,
            )
        }

        // ---- domain reputation ----------------------------------------------
        when {
            bareDomain in disposable -> add(
                "dom.disposable", "A throwaway domain",
                "$bareDomain exists to receive mail briefly and then disappear. Fine for one download link; " +
                    "a bad place to be reachable at, and most services now reject these at sign-up.",
                FindingClass.Hygiene,
            )
            domain.isEmpty() -> Unit
            bareDomain.contains('.') && bareDomain !in dotIgnored &&
                bareDomain !in setOf(
                    "yahoo.com", "outlook.com", "hotmail.com", "icloud.com", "proton.me",
                    "protonmail.com", "zoho.com", "aol.com",
                ) -> add(
                "dom.custom", "On a domain you or an employer control",
                "You can stop receiving at this address whenever you like, which free webmail does not offer. " +
                    "That is the single practical advantage worth having, and it is a revocation power, not a " +
                    "privacy one.",
                FindingClass.Benign, certain = false,
            )
            domain.isNotEmpty() -> add(
                "dom.free", "On a free webmail domain",
                "Deliverability is good and the address is as permanent as the provider's terms. Revoking it " +
                    "means leaving, and it is one of the domains a guesser assumes an address lives on.",
                FindingClass.Hygiene, certain = false,
            )
        }
        if (lowerLocal in roleNames) {
            add(
                "role.general", "A role address",
                "\"$lowerLocal@\" is published on company websites and collected by harvesters by design, so " +
                    "expect it to be in spam lists from the first week. It also means anyone can address the " +
                    "whole function, not one person.",
                FindingClass.Hygiene, quotes = true,
            )
        }
        if (local.isNotEmpty() && local.length <= 2 && domain.isNotEmpty()) {
            add(
                "role.short", "A very short local part",
                "Two characters or fewer. Unlikely to be unique by accident, and mail systems may already " +
                    "reserve such names.",
                FindingClass.Hygiene, certain = false,
            )
        }

        findings += Finding(
            key = "note.notsecret", label = "An address is not a secret",
            detail = "This panel is descriptive. It gives no strength band, because an email address is meant " +
                "to be given out — that is what makes it an address. The thing it is worth checking is what the " +
                "address reveals and which variations your provider silently collapses.",
            klass = FindingClass.Benign, delta = 0,
        )

        val certain = findings.count { it.certain }
        return Audit(
            mode = Mode.Email,
            findings = findings,
            length = s.length,
            alphabetSize = 0,
            budgetBits = null,
            naiveBits = null,
            band = when {
                findings.any { it.key == "dom.disposable" || it.key.startsWith("form.") && it.key != "form.ok" } ->
                    "Will cause trouble"
                certain == 0 -> "Nothing definite to report"
                else -> "Workable, with caveats"
            },
            bandTone = when {
                findings.any { it.key == "dom.disposable" || (it.key.startsWith("form.") && it.key != "form.ok") } -> Tone.Alert
                certain == 0 -> Tone.Warm
                else -> Tone.Calm
            },
            contextWordsUsed = ctx.size,
        )
    }

    /** A four-digit group that sits in the range where a birth year plausibly lands. */
    private fun yearIn(local: String): Int? =
        Regex("(?<!\\d)(19[6-9]\\d|20[0-1]\\d)(?!\\d)").find(local)?.value?.toIntOrNull()
}
