@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.pournotes.ui

import android.content.SharedPreferences
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Every interface choice. Indexes point into the lists of Theme.kt and [GRADE_COUNTS]. */
data class Prefs(
    val theme: Int = 0,
    val palette: Int = 0,
    val black: Boolean = false,
    val corners: Int = 1,
    val font: Int = 0,
    val textScale: Int = 1,
    val expressive: Boolean = true,
    val haptics: Boolean = true,
    val subjectColors: Boolean = true,
    val showNext: Boolean = true,
    val showStats: Boolean = true,
    val showHomework: Boolean = true,
    val showGrades: Boolean = true,
    val grades: Int = 1,
)

val GRADE_COUNTS = listOf(3, 5, 10)

fun loadPrefs(sp: SharedPreferences) = Prefs().run {
    copy(
        theme = sp.getInt("theme", theme), palette = sp.getInt("palette", palette), black = sp.getBoolean("black", black),
        corners = sp.getInt("corners", corners), font = sp.getInt("font", font), textScale = sp.getInt("textScale", textScale),
        expressive = sp.getBoolean("expressive", expressive), haptics = sp.getBoolean("haptics", haptics),
        subjectColors = sp.getBoolean("subjectColors", subjectColors), showNext = sp.getBoolean("showNext", showNext),
        showStats = sp.getBoolean("showStats", showStats), showHomework = sp.getBoolean("showHomework", showHomework),
        showGrades = sp.getBoolean("showGrades", showGrades), grades = sp.getInt("grades", grades),
    )
}

fun Prefs.save(sp: SharedPreferences) = sp.edit()
    .putInt("theme", theme).putInt("palette", palette).putBoolean("black", black)
    .putInt("corners", corners).putInt("font", font).putInt("textScale", textScale)
    .putBoolean("expressive", expressive).putBoolean("haptics", haptics)
    .putBoolean("subjectColors", subjectColors).putBoolean("showNext", showNext)
    .putBoolean("showStats", showStats).putBoolean("showHomework", showHomework)
    .putBoolean("showGrades", showGrades).putInt("grades", grades)
    .apply()

@Composable
fun SettingsScreen(prefs: Prefs, onChange: (Prefs) -> Unit, onBack: () -> Unit, onLogout: () -> Unit) {
    val bar = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var confirm by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme

    Scaffold(
        Modifier.nestedScroll(bar.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Paramètres") },
                navigationIcon = { IconButton(onBack) { Icon(Icons.back, "Retour") } },
                scrollBehavior = bar,
            )
        },
    ) { inner ->
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(inner).padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(Icons.palette, "Apparence")
            BentoCard(scheme.surfaceContainerLow) {
                Label("Thème")
                Choices(listOf("Système", "Clair", "Sombre"), prefs.theme) { onChange(prefs.copy(theme = it)) }
                Toggle("Noir pur en thème sombre", "Économise la batterie des écrans OLED", prefs.black) { onChange(prefs.copy(black = it)) }
                Label("Couleurs")
                Swatches(prefs.palette) { onChange(prefs.copy(palette = it)) }
            }
            BentoCard(scheme.surfaceContainerLow) {
                Label("Coins")
                Choices(listOf("Droits", "Arrondis", "Très arrondis"), prefs.corners) { onChange(prefs.copy(corners = it)) }
                Label("Police")
                Choices(listOf("Système", "Serif", "Mono"), prefs.font) { onChange(prefs.copy(font = it)) }
                Label("Taille du texte")
                Choices(listOf("Petite", "Normale", "Grande", "Très grande"), prefs.textScale) { onChange(prefs.copy(textScale = it)) }
            }

            SectionHeader(Icons.refresh, "Mouvement")
            BentoCard(scheme.surfaceContainerLow) {
                Toggle("Animations expressives", "Ressorts avec léger rebond ; sinon des mouvements plus sobres", prefs.expressive) { onChange(prefs.copy(expressive = it)) }
                Toggle("Vibrations", "Quand tu coches un devoir", prefs.haptics) { onChange(prefs.copy(haptics = it)) }
            }

            SectionHeader(Icons.home, "Accueil")
            BentoCard(scheme.surfaceContainerLow) {
                Toggle("Prochain cours", null, prefs.showNext) { onChange(prefs.copy(showNext = it)) }
                Toggle("Moyenne et devoirs à faire", null, prefs.showStats) { onChange(prefs.copy(showStats = it)) }
                Toggle("Devoirs", null, prefs.showHomework) { onChange(prefs.copy(showHomework = it)) }
                Toggle("Dernières notes", null, prefs.showGrades) { onChange(prefs.copy(showGrades = it)) }
                if (prefs.showGrades) {
                    Label("Notes affichées")
                    Choices(GRADE_COUNTS.map { "$it" }, prefs.grades) { onChange(prefs.copy(grades = it)) }
                }
                Toggle("Couleurs des matières", "Celles choisies par l'établissement sur PRONOTE", prefs.subjectColors) { onChange(prefs.copy(subjectColors = it)) }
            }

            SectionHeader(Icons.person, "Compte")
            Button(
                onClick = { confirm = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(containerColor = scheme.errorContainer, contentColor = scheme.onErrorContainer),
            ) {
                Icon(Icons.logout, null)
                Text("Se déconnecter", Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        icon = { Icon(Icons.logout, null) },
        title = { Text("Se déconnecter ?") },
        text = { Text("Tes identifiants enregistrés seront effacés de ce téléphone.") },
        confirmButton = { Button({ confirm = false; onLogout() }) { Text("Se déconnecter") } },
        dismissButton = { TextButton({ confirm = false }) { Text("Annuler") } },
    )
}

@Composable
private fun Label(text: String) {
    Text(text, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun Toggle(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, onChange)
    }
}

/** One choice among a few short labels, as an Expressive connected button group: the chosen one fills and rounds out. */
@Composable
private fun Choices(options: List<String>, selected: Int, onPick: (Int) -> Unit) {
    val style = MaterialTheme.typography.labelLarge
    val c = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
        options.forEachIndexed { i, label ->
            ToggleButton(
                checked = i == selected,
                onCheckedChange = { if (i != selected) onPick(i) },
                modifier = Modifier.weight(label.length + 6f).semantics { role = Role.RadioButton },
                colors = ToggleButtonDefaults.toggleButtonColors(containerColor = c.surfaceContainerHighest, contentColor = c.onSurfaceVariant),
                shapes = when (i) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                Text(label, maxLines = 1, style = style, autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = style.fontSize))
            }
        }
    }
}

/** The palettes as colour swatches; the chosen one turns into a scalloped badge. */
@Composable
private fun Swatches(selected: Int, onPick: (Int) -> Unit) {
    val context = LocalContext.current
    val cookie = MaterialShapes.Cookie9Sided.toShape()
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PALETTES.forEachIndexed { i, name ->
            val s = SEEDS[i]?.let { schemeFrom(it, false) }
                ?: if (Build.VERSION.SDK_INT >= 31) dynamicLightColorScheme(context) else schemeFrom(SEEDS[1]!!, false)
            Column(Modifier.width(72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface({ onPick(i) }, Modifier.size(52.dp), shape = if (i == selected) cookie else CircleShape, color = s.primary, contentColor = s.onPrimary) {
                    if (i == selected) Icon(Icons.check, "Choisie", Modifier.padding(14.dp))
                }
                Text(name, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}
