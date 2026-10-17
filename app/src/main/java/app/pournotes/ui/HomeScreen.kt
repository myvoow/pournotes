@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.pournotes.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pournotes.Home
import app.pournotes.data.Grade
import app.pournotes.data.Homework
import app.pournotes.data.Lesson
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    home: Home?,
    refreshing: Boolean,
    error: String?,
    notice: String?,
    onNoticeShown: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onToggle: (Homework) -> Unit,
    onTimetable: () -> Unit,
    onSettings: () -> Unit,
) {
    val bar = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(notice) {
        if (notice != null) {
            snackbar.showSnackbar(notice)
            onNoticeShown()
        }
    }
    var detail by remember { mutableStateOf<Any?>(null) }
    val profile = home?.profile

    Scaffold(
        Modifier.nestedScroll(bar.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(profile?.let { "Bonjour ${firstName(it.name)}" } ?: "Pournotes", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                subtitle = profile?.let { p ->
                    { Text(listOf(p.className, p.school).filter { it.isNotBlank() }.joinToString(" · "), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                },
                navigationIcon = { IconButton(onTimetable) { Icon(Icons.calendar, "Emploi du temps complet") } },
                actions = { IconButton(onSettings) { Icon(Icons.settings, "Paramètres") } },
                scrollBehavior = bar,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        val pull = rememberPullToRefreshState()
        val pulling = refreshing && home != null
        PullToRefreshBox(
            pulling, onRefresh,
            Modifier.fillMaxSize().padding(top = inner.calculateTopPadding()),
            state = pull,
            indicator = { PullToRefreshDefaults.LoadingIndicator(state = pull, isRefreshing = pulling, modifier = Modifier.align(Alignment.TopCenter)) },
        ) {
            val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
            // Home first, else the error, else loading. Keyed by kind so a refresh does not cross-fade the whole page.
            AnimatedContent(home ?: error ?: Unit, contentKey = { it::class }, transitionSpec = { fadeIn(effects) togetherWith fadeOut(effects) }, label = "home") { s ->
                when (s) {
                    is Home -> HomeContent(s, error, inner.calculateBottomPadding(), { detail = it }, onToggle)
                    is String -> Failure(s, refreshing, onRefresh, onLogout)
                    else -> Loading()
                }
            }
        }
    }

    detail?.let { d -> DetailSheet(d, home, onToggle) { detail = null } }
}

/** A lesson, homework or grade in full, in a bottom sheet. */
@Composable
fun DetailSheet(d: Any, home: Home?, onToggle: (Homework) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (d) {
                is Lesson -> LessonDetail(d)
                // Read the homework back from the state so the button follows the toggle.
                is Homework -> HomeworkDetail(home?.homework?.find { it.id == d.id } ?: d, onToggle)
                is Grade -> GradeDetail(d)
            }
        }
    }
}

@Composable
private fun HomeContent(home: Home, error: String?, bottom: androidx.compose.ui.unit.Dp, onOpen: (Any) -> Unit, onToggle: (Homework) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val prefs = LocalPrefs.current
    val now = LocalDateTime.now()
    val next = home.lessons.firstOrNull { it.end.isAfter(now) && !it.canceled }
    val day = (next ?: home.lessons.firstOrNull())?.start?.toLocalDate()
    val dayLessons = home.lessons.filter { it.start.toLocalDate() == day }
    val homework = home.homework?.take(6)?.takeIf { prefs.showHomework }
    val todo = home.homework?.count { !it.done }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottom + 24.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item("error") {
            AnimatedVisibility(
                error != null,
                enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
            ) {
                Surface(Modifier.padding(bottom = 10.dp), shape = MaterialTheme.shapes.large, color = scheme.errorContainer) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.warning, null)
                        Text("Données non actualisées : ${error.orEmpty()}", Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        if (prefs.showNext) item("next") { NextLesson(next, now, springy().padding(bottom = 10.dp)) { onOpen(it) } }

        if (prefs.showStats) item("stats") {
            Row(springy().fillMaxWidth().height(IntrinsicSize.Min).padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Moyenne générale", home.grades?.overall ?: "—", home.grades?.period, scheme.tertiaryContainer, Modifier.weight(1f).fillMaxHeight())
                StatTile(
                    "Devoirs à faire", todo?.toString() ?: "—",
                    when (todo) { null -> null; 0 -> "Tout est fait"; else -> "d'ici 2 semaines" },
                    scheme.secondaryContainer, Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }

        item("dayHeader") { SectionHeader(Icons.clock, day?.let(::dayLabel) ?: "Emploi du temps", springy()) }
        if (dayLessons.isEmpty()) item("dayEmpty") { Empty("Aucun cours d'ici la semaine prochaine.") }
        itemsIndexed(dayLessons, key = { _, l -> "l${l.id}${l.start}" }) { i, l ->
            GroupItem(i, dayLessons.size, { onOpen(l) }, springy()) { LessonRow(l) }
        }

        if (homework != null) {
            item("hwHeader") { SectionHeader(Icons.book, "Devoirs", springy(), trailing = todo?.takeIf { it > 0 }?.let { "$it à faire" }) }
            if (homework.isEmpty()) item("hwEmpty") { Empty("Aucun devoir à venir.") }
            itemsIndexed(homework, key = { _, h -> "h${h.id}" }) { i, h ->
                GroupItem(i, homework.size, { onOpen(h) }, springy()) { HomeworkRow(h, onToggle) }
            }
        }

        home.grades?.takeIf { prefs.showGrades }?.let { g ->
            val latest = g.latest.take(GRADE_COUNTS[prefs.grades])
            item("gHeader") { SectionHeader(Icons.star, "Dernières notes", springy(), trailing = g.period) }
            if (latest.isEmpty()) item("gEmpty") { Empty("Pas encore de note sur cette période.") }
            itemsIndexed(latest, key = { i, _ -> "g$i" }) { i, grade ->
                GroupItem(i, latest.size, { onOpen(grade) }, springy()) { GradeRow(grade) }
            }
        }
    }
}

/** List items slide and fade with the theme's springs when the data changes. */
@Composable
private fun LazyItemScope.springy() = Modifier.animateItem(
    fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
    placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
    fadeOutSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
)

@Composable
private fun NextLesson(next: Lesson?, now: LocalDateTime, modifier: Modifier, onOpen: (Lesson) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    if (next == null) {
        BentoCard(scheme.surfaceContainerHigh, modifier) {
            Text("Plus de cours prévu", style = MaterialTheme.typography.headlineSmall)
            Text("Rien à l'emploi du temps d'ici la fin de la semaine prochaine.", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        }
        return
    }
    BentoCard(scheme.primaryContainer, modifier, onClick = { onOpen(next) }) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(relative(next, now), scheme.primary)
            if (next.test) Pill("Contrôle", scheme.tertiary)
        }
        Text(next.subject, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Meta(Icons.clock, "${HM.format(next.start)} – ${HM.format(next.end)}")
            next.room?.let { Meta(Icons.place, it) }
            next.teacher?.let { Meta(Icons.person, it) }
        }
        next.status?.let { Pill(it, scheme.surfaceContainerLowest, Modifier.padding(top = 4.dp), Icons.warning) }
    }
}

@Composable
internal fun RowScope.LessonRow(l: Lesson) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.width(52.dp)) {
        Text(HM.format(l.start), style = MaterialTheme.typography.titleMedium)
        Text(HM.format(l.end), style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
    }
    Box(Modifier.padding(horizontal = 12.dp).width(4.dp).height(40.dp).background(subjectColor(l.color), CircleShape))
    Column(Modifier.weight(1f)) {
        Text(
            l.subject, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
            textDecoration = if (l.canceled) TextDecoration.LineThrough else null,
            color = if (l.canceled) scheme.onSurfaceVariant else scheme.onSurface,
        )
        val meta = listOfNotNull(l.room, l.teacher).joinToString(" · ")
        if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    l.status?.let { Pill(it, if (l.canceled) scheme.errorContainer else scheme.tertiaryContainer, Modifier.padding(start = 8.dp).widthIn(max = 140.dp)) }
}

@Composable
private fun RowScope.HomeworkRow(h: Homework, onToggle: (Homework) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val prefs = LocalPrefs.current
    val text by animateColorAsState(if (h.done) scheme.onSurfaceVariant else scheme.onSurface, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "done")
    // Round when there is something to do, squarer once done: the shape says the state too.
    FilledTonalIconToggleButton(
        checked = h.done,
        onCheckedChange = {
            if (prefs.haptics) haptics.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
            onToggle(h)
        },
        shapes = IconButtonDefaults.toggleableShapes(),
    ) {
        if (h.done) Icon(Icons.check, "Marquer comme à faire") else Spacer(Modifier.size(24.dp))
    }
    Column(Modifier.weight(1f).padding(start = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SubjectDot(h.color)
            Text(h.subject, Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            h.description.ifBlank { "Pas de consigne" }, style = MaterialTheme.typography.bodyMedium, color = text, maxLines = 2, overflow = TextOverflow.Ellipsis,
            textDecoration = if (h.done) TextDecoration.LineThrough else null,
        )
        Text(dueLabel(h.due), style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
    }
}

@Composable
private fun RowScope.GradeRow(g: Grade) {
    val scheme = MaterialTheme.colorScheme
    GradeBadge(g, Modifier.widthIn(min = 76.dp))
    Column(Modifier.weight(1f).padding(start = 14.dp)) {
        Text(g.subject, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            listOfNotNull(SHORT.format(g.date), g.comment).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
    g.classAverage?.let {
        Column(Modifier.padding(start = 8.dp), horizontalAlignment = Alignment.End) {
            Text("Classe", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            Text(it, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun GradeBadge(g: Grade, modifier: Modifier = Modifier) {
    Surface(modifier, shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
            Text(g.value, style = MaterialTheme.typography.titleLarge, maxLines = 1)
            if (g.value.firstOrNull()?.isDigit() == true) Text("/${g.outOf}", Modifier.padding(start = 2.dp, bottom = 2.dp), style = MaterialTheme.typography.labelMedium)
        }
    }
}

// ---- Bottom sheet details ----

@Composable
private fun LessonDetail(l: Lesson) {
    Text(l.subject, style = MaterialTheme.typography.headlineMedium)
    Pill("${dayLabel(l.start.toLocalDate())} · ${HM.format(l.start)} – ${HM.format(l.end)}", MaterialTheme.colorScheme.primaryContainer, icon = Icons.clock)
    Info("Salle", l.room)
    Info("Professeur", l.teacher)
    Info("Groupe", l.group)
    Info("Statut", l.status)
    if (l.test) Info("Évaluation", "Contrôle prévu")
}

@Composable
private fun HomeworkDetail(h: Homework, onToggle: (Homework) -> Unit) {
    Text(h.subject, style = MaterialTheme.typography.headlineMedium)
    Pill(dueLabel(h.due), MaterialTheme.colorScheme.secondaryContainer, icon = Icons.book)
    Text(h.description.ifBlank { "Pas de consigne" }, style = MaterialTheme.typography.bodyLarge)
    val shapes = ButtonDefaults.shapes(pressedShape = ButtonDefaults.mediumPressedShape)
    val action = if (h.done) "Remettre à faire" else "Marquer comme fait"
    if (h.done) OutlinedButton(onClick = { onToggle(h) }, modifier = Modifier.fillMaxWidth().height(56.dp), shapes = shapes) { Text(action) }
    else Button(onClick = { onToggle(h) }, modifier = Modifier.fillMaxWidth().height(56.dp), shapes = shapes) { Icon(Icons.check, null); Text(action, Modifier.padding(start = 8.dp)) }
}

@Composable
private fun GradeDetail(g: Grade) {
    Text(g.subject, style = MaterialTheme.typography.headlineMedium)
    Row(verticalAlignment = Alignment.Bottom) {
        Text(g.value, style = MaterialTheme.typography.displayMedium)
        if (g.value.firstOrNull()?.isDigit() == true) Text("/${g.outOf}", Modifier.padding(start = 4.dp, bottom = 8.dp), style = MaterialTheme.typography.titleLarge)
    }
    Info("Date", LONG.format(g.date))
    Info("Moyenne de la classe", g.classAverage)
    Info("Note la plus haute", g.max)
    Info("Note la plus basse", g.min)
    Info("Coefficient", g.coefficient)
    Info("Commentaire", g.comment)
}

@Composable
private fun Info(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, Modifier.weight(1.4f), style = MaterialTheme.typography.bodyLarge)
    }
}

// ---- States ----

@Composable
private fun Loading() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        ContainedLoadingIndicator(Modifier.size(72.dp))
        Text("Connexion à PRONOTE…", Modifier.padding(top = 16.dp), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun Failure(message: String, busy: Boolean, onRetry: () -> Unit, onLogout: () -> Unit) {
    // Scrollable so that pull-to-refresh also works here.
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        BentoCard(MaterialTheme.colorScheme.errorContainer) {
            Icon(Icons.warning, null, Modifier.size(32.dp))
            Text("Impossible de charger PRONOTE", style = MaterialTheme.typography.headlineSmall)
            Text(message, style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onRetry, enabled = !busy, shapes = ButtonDefaults.shapes()) { Icon(Icons.refresh, null); Text("Réessayer", Modifier.padding(start = 8.dp)) }
                OutlinedButton(onLogout, shapes = ButtonDefaults.shapes()) { Text("Se déconnecter") }
            }
        }
    }
}

@Composable
internal fun Empty(text: String) {
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Text(text, Modifier.padding(20.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---- Formatting ----

internal val HM = DateTimeFormatter.ofPattern("HH:mm")
private val SHORT = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH)
private val LONG = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)

/** PRONOTE names people "NOM Prénom": the first word not in capitals. */
private fun firstName(name: String) = name.split(" ").firstOrNull { it != it.uppercase() } ?: name

internal fun dayLabel(d: LocalDate): String = when (d) {
    LocalDate.now() -> "Aujourd'hui"
    LocalDate.now().plusDays(1) -> "Demain"
    else -> LONG.format(d).replaceFirstChar { it.titlecase(Locale.FRENCH) }
}

private fun dueLabel(d: LocalDate): String = when (d) {
    LocalDate.now() -> "Pour aujourd'hui"
    LocalDate.now().plusDays(1) -> "Pour demain"
    else -> "Pour ${LONG.format(d)}"
}

private fun relative(l: Lesson, now: LocalDateTime): String = when {
    !now.isBefore(l.start) -> "En cours · jusqu'à ${HM.format(l.end)}"
    l.start.toLocalDate() == now.toLocalDate() -> Duration.between(now, l.start).toMinutes().let { if (it < 60) "Dans ${it + 1} min" else "À ${HM.format(l.start)}" }
    else -> "${dayLabel(l.start.toLocalDate())} · ${HM.format(l.start)}"
}
