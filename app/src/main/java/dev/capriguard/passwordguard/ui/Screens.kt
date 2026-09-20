package dev.capriguard.passwordguard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.capriguard.passwordguard.audit.Audit
import dev.capriguard.passwordguard.audit.Finding
import dev.capriguard.passwordguard.audit.FindingClass
import dev.capriguard.passwordguard.audit.Known
import dev.capriguard.passwordguard.audit.Mode
import dev.capriguard.passwordguard.audit.PasswordGuardViewModel
import dev.capriguard.passwordguard.audit.Tone
import dev.capriguard.passwordguard.audit.auditText
import dev.capriguard.passwordguard.core.GlassCard
import dev.capriguard.passwordguard.core.GlassChip
import dev.capriguard.passwordguard.core.Labeled
import dev.capriguard.passwordguard.core.LiquidBackdrop
import dev.capriguard.passwordguard.core.LocalGlass
import dev.capriguard.passwordguard.core.Palette
import dev.capriguard.passwordguard.core.Radii
import dev.capriguard.passwordguard.core.tnum

/* ------------------------------------------------------------------ shell */

@Composable
fun ScreenShell(
    title: String,
    onBack: (() -> Unit)?,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    LiquidBackdrop(base = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    IconBadge(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                } else {
                    Spacer(Modifier.width(40.dp))
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(start = 6.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                action?.invoke()
            }
            content()
        }
    }
}

@Composable
private fun IconBadge(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(Radii.PillShape)
            .background(LocalGlass.current.scrim)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

private fun toneColor(t: Tone): Color = when (t) {
    Tone.Calm -> Palette.Calm
    Tone.Warm -> Palette.Warm
    Tone.Alert -> Palette.Alert
}

/* ------------------------------------------------------------------- home */

@Composable
fun HomeScreen(vm: PasswordGuardViewModel, onOpenSettings: () -> Unit) {
    val clipboard = LocalClipboardManager.current

    ScreenShell(title = "PasswordGuard", onBack = null, action = {
        IconBadge(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, "Settings") }
    }) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ModeSwitch(mode = vm.mode, onPick = vm::pickMode)
            SecretField(vm)
            ContextField(vm)

            if (vm.input.isEmpty()) {
                EmptyState(mode = vm.mode)
            } else {
                vm.audit?.let { a ->
                    BandCard(a)
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                        GlassChip(modifier = Modifier.clip(Radii.PillShape).clickable { vm.clearInput() }) {
                            Icon(Icons.Filled.Delete, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Clear", style = MaterialTheme.typography.labelMedium)
                        }
                        GlassChip(modifier = Modifier.clip(Radii.PillShape).clickable { clipboard.setText(AnnotatedString(auditText(a))) }) {
                            Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copy reading", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Text(
                        "The copy has every quoted fragment stripped. It says which patterns matched, not what matched in them.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FindingsCard(a)
                    if (vm.config.usable && vm.narrative == null && !vm.busyRemote) {
                        Button(
                            onClick = vm::askCoach,
                            shape = Radii.PillShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                        ) { Text("Explain it in plain words", style = MaterialTheme.typography.labelLarge) }
                    }
                    if (vm.busyRemote) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            LinearProgressIndicator(Modifier.width(52.dp).height(3.dp).clip(Radii.PillShape))
                            Text(
                                "Sending the structure only — lengths, pattern names, deltas. Not the input.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    vm.narrative?.let { NarrativeCard(it) }
                }
                MethodCard()
            }
            vm.error?.let { ErrorCard(it) }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun ModeSwitch(mode: Mode, onPick: (Mode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
        Mode.entries.forEach { m ->
            val on = m == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(Radii.ControlShape)
                    .background(if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else LocalGlass.current.scrim)
                    .border(0.7.dp, if (on) MaterialTheme.colorScheme.primary else LocalGlass.current.hairline, Radii.ControlShape)
                    .clickable(onClick = { onPick(m) })
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        if (m == Mode.Passphrase) Icons.Filled.Lock else Icons.Filled.AlternateEmail,
                        null,
                        modifier = Modifier.size(17.dp),
                        tint = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (m == Mode.Passphrase) "Passphrase" else "Email address",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SecretField(vm: PasswordGuardViewModel) {
    val email = vm.mode == Mode.Email
    OutlinedTextField(
        value = vm.input,
        onValueChange = vm::typeInput,
        label = { Text(if (email) "Address to read" else "Secret to judge", style = MaterialTheme.typography.labelMedium) },
        placeholder = {
            Text(
                if (email) "name.example@domain.com" else "Type or paste it. It stays on this screen.",
                style = MaterialTheme.typography.bodySmall,
            )
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace).tnum(),
        visualTransformation = if (email || vm.revealed) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (email) KeyboardType.Email else KeyboardType.Password,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { }),
        trailingIcon = if (email) null else {
            {
                IconButtonPad(onClick = vm::toggleReveal) {
                    Icon(
                        if (vm.revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        if (vm.revealed) "Hide" else "Show",
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        },
        shape = Radii.ControlShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        if (email) "An address is not a secret. This reads what it gives away, not how strong it is."
        else "Nothing is stored: not in a file, not in the app's own settings, not in a log. Android's text field holds a String we cannot erase from memory, which is why leaving the app wipes it.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun IconButtonPad(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(40.dp).clip(Radii.PillShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

@Composable
private fun ContextField(vm: PasswordGuardViewModel) {
    OutlinedTextField(
        value = vm.contextRaw,
        onValueChange = vm::setContextWords,
        label = { Text("Words a guesser could tie to you", style = MaterialTheme.typography.labelMedium) },
        placeholder = { Text("optional, comma separated — your name, a birthday year, a pet, a town", style = MaterialTheme.typography.bodySmall) },
        singleLine = false,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, autoCorrectEnabled = false, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { }),
        shape = Radii.ControlShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp),
    )
    Text(
        "Optional, and the most useful thing here. A list that reaches everybody cannot see your surname; these words let the reading approximate an attacker who can. Also kept in memory only.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/* ----------------------------------------------------------------- result */

@Composable
private fun BandCard(a: Audit) {
    val tone = toneColor(a.bandTone)
    GlassCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(9.dp).clip(Radii.PillShape).background(tone))
                Text(a.band, style = MaterialTheme.typography.headlineSmall, color = tone)
            }
            a.inconclusive?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (a.mode == Mode.Passphrase) {
                a.budgetBits?.let {
                    Labeled(
                        "Our guess budget",
                        "about 2^$it tries  ·  $it bits",
                        tint = tone,
                    )
                }
                a.naiveBits?.let { n ->
                    Labeled("What a charset meter claims", "2^$n  ·  $n bits")
                    val gap = n - (a.budgetBits ?: n)
                    if (gap > 3) {
                        Text(
                            "That meter would tell you this is $gap powers of two stronger than we think it is. " +
                                "The gap is the whole reason this screen exists.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Labeled("Characters", a.length.toString())
                Labeled("Alphabet", a.alphabetSize.toString())
            } else {
                Labeled("Findings", a.count.toString())
                Labeled("Only a guess", a.uncertain.toString())
            }
            Text(
                "No time-to-crack figure, on purpose. Turning bits into seconds means assuming somebody else's " +
                    "hardware, and that assumption is where the confident numbers on this screen would come from.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FindingsCard(a: Audit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text("What we found", style = MaterialTheme.typography.titleMedium)
        if (a.findings.isEmpty()) {
            Text(
                "No detector fired. That is the weakest possible statement about you — it means this app's " +
                    "patterns did not match, not that nothing would.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        a.classified.forEach { (klass, items) ->
            Text(
                klass.label.uppercase(),
                style = MaterialTheme.typography.labelSmall.tnum(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            items.forEach { FindingRow(it) }
        }
    }
}

@Composable
private fun FindingRow(f: Finding) {
    val tone = when {
        f.klass == FindingClass.Benign -> MaterialTheme.colorScheme.onSurfaceVariant
        f.delta >= 24 -> Palette.Alert
        f.delta >= 8 -> Palette.Warm
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    GlassCard(Modifier.fillMaxWidth(), padding = 14.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Box(Modifier.width(3.dp).heightIn(min = 28.dp).background(tone).clip(Radii.PillShape))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(f.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f, fill = false))
                    if (!f.certain) {
                        Text(
                            "possible",
                            style = MaterialTheme.typography.labelSmall,
                            color = Palette.Warm,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (f.delta > 0) {
                        Text(
                            "−${f.delta} bits",
                            style = MaterialTheme.typography.labelSmall.tnum(),
                            color = tone,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Text(
                    f.detail,
                    style = if (f.klass == FindingClass.Size) MaterialTheme.typography.bodySmall.tnum()
                    else MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MethodCard() {
    var open by remember { mutableStateOf(false) }
    GlassCard(Modifier.fillMaxWidth(), padding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = { open = !open }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("How this number was made", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(if (open) "less" else "more", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            if (open) {
                Text(
                    "Each detector proposes its own way of guessing what you typed, in bits. The reading keeps the " +
                        "smallest — the easiest path we can see. A −N figure is not a penalty stacked on the others; " +
                        "it says this path is N bits cheaper than treating the string as random.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Labeled("under 24 bits", "Guessable in one sitting")
                Labeled("24 to 39", "Weak where it matters")
                Labeled("40 to 59", "Workable, not durable")
                Labeled("60 to 79", "Solid for one account")
                Labeled("80 and up", "Strong by any standard we can see")
                Text(
                    "The corpora are embedded and small: ${Known.common.size} common passwords, " +
                        "${Known.words.size} English words. " +
                        "A small list can prove a hit and can never clear a miss. Sources and the full table are " +
                            "in the README.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NarrativeCard(text: String) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("Plain words from your model", style = MaterialTheme.typography.titleSmall)
            Text(text, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Generated from the structural summary above. It was not given the input, and it was not given " +
                    "any fragment of it.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    GlassCard(Modifier.fillMaxWidth(), shape = Radii.ControlShape) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Could not continue", style = MaterialTheme.typography.titleSmall, color = Palette.Alert)
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/* ------------------------------------------------------------- empty state */

@Composable
private fun EmptyState(mode: Mode) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            if (mode == Mode.Passphrase) "What a guesser actually tries"
            else "What an address gives away",
            style = MaterialTheme.typography.displaySmall,
        )
        Text(
            if (mode == Mode.Passphrase)
                "A meter that multiplies length by character variety is selling you a number that is almost " +
                    "always too big. This one asks how a guesser would reach your string first — through a " +
                    "breach list, a word list, a keyboard row, a birthday — and reports the cheapest route it " +
                    "can find. On this device, with no key and no network."
            else
                "Dots that Gmail ignores, a plus-tag that is really a label, a year in the local part that " +
                    "answers half the security questions people invent. Read off the characters themselves, on " +
                    "this device, with no key and no network.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        GlassCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("What this will never do", style = MaterialTheme.typography.titleSmall)
                Text(
                    "It will not store what you type, show you a percentage, promise you a number of years to " +
                        "crack it, or send the input to a model — including when you turn the optional plain-words " +
                        "pass on. It does not upload anything to check you against a breach, because that would " +
                        "mean telling a server about the secret you came here to keep from servers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        GlassCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("What it cannot see", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Whether your account has already been breached — that needs a network call this app will " +
                        "not make. And a password chosen well against a bulk attack is still weak against " +
                        "somebody who knows you; that is what the context-words field is for, and it is only an " +
                        "approximation of what a real acquaintance could guess.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/* --------------------------------------------------------------- settings */

@Composable
fun SettingsScreen(vm: PasswordGuardViewModel, onBack: () -> Unit) {
    var reveal by remember { mutableStateOf(false) }

    ScreenShell(title = "Settings", onBack = onBack) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Every reading on the main screen is computed on the device and needs none of this. The " +
                    "endpoint below is only for the optional plain-words pass, which explains a reading that " +
                    "already exists.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Plain-words pass", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(
                    checked = vm.config.visionOn,
                    onCheckedChange = vm::setCoachOn,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
            Field("Base URL", vm.config.baseUrl, "https://api.example.com", onDone = vm::setBaseUrl)
            Field("Model", vm.config.model, "gpt-4o-mini", onDone = vm::setModel)
            Field(
                label = "API key",
                value = vm.config.apiKey,
                placeholder = "Typed here, kept on this device",
                masked = !reveal,
                secret = true,
                onDone = vm::setApiKey,
            )
            Text(
                "What goes out is the structural summary: lengths, pattern names, bit deltas. Not the input, " +
                    "and not the fragments the findings card prints on screen — those are withheld from every " +
                    "copy path. Turn the switch off and no request is ever made.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { reveal = !reveal }) {
                Text(if (reveal) "Hide key" else "Show key", style = MaterialTheme.typography.labelMedium)
            }
            Text(
                "The key lives in this app's private storage and the manifest sets allowBackup=false, so it " +
                    "cannot ride out in a cloud backup, a device transfer or an adb backup. Your own API key is " +
                    "yours: this build ships without one and without anyone else's quota.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    placeholder: String,
    onDone: (String) -> Unit,
    masked: Boolean = false,
    secret: Boolean = false,
) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
        singleLine = !secret,
        visualTransformation = if (masked) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (secret) KeyboardType.Password else KeyboardType.Uri,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone(text) }),
        shape = Radii.ControlShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
