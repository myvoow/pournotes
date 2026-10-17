@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package app.pournotes.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.pournotes.data.Lesson
import app.pournotes.thisWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The whole week, day by day; arrows move a week, the calendar button comes back to this one. */
@Composable
fun TimetableScreen(monday: LocalDate, lessons: List<Lesson>?, error: String?, onWeek: (LocalDate) -> Unit, onBack: () -> Unit) {
    val bar = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var detail by remember { mutableStateOf<Lesson?>(null) }
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

    Scaffold(
        Modifier.nestedScroll(bar.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text("Emploi du temps") },
                subtitle = { Text("Semaine du ${WEEK.format(monday)}") },
                navigationIcon = { IconButton(onBack) { Icon(Icons.back, "Retour") } },
                actions = {
                    IconButton({ onWeek(monday.minusWeeks(1)) }) { Icon(Icons.prev, "Semaine précédente") }
                    IconButton({ onWeek(thisWeek()) }, enabled = monday != thisWeek()) { Icon(Icons.calendar, "Cette semaine") }
                    IconButton({ onWeek(monday.plusWeeks(1)) }) { Icon(Icons.next, "Semaine suivante") }
                },
                scrollBehavior = bar,
            )
        },
    ) { inner ->
        // Weeks slide in from the side they come from.
        AnimatedContent(
            monday,
            Modifier.fillMaxSize().padding(top = inner.calculateTopPadding()),
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally(spatial) { it * dir } + fadeIn(effects)) togetherWith (slideOutHorizontally(spatial) { -it * dir } + fadeOut(effects))
            },
            label = "week",
        ) { _ ->
            when {
                error != null -> Column(Modifier.fillMaxSize().padding(16.dp)) {
                    BentoCard(MaterialTheme.colorScheme.errorContainer) {
                        Text(error, style = MaterialTheme.typography.bodyMedium)
                        Button({ onWeek(monday) }, shapes = ButtonDefaults.shapes()) { Text("Réessayer") }
                    }
                }
                lessons == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { ContainedLoadingIndicator(Modifier.size(72.dp)) }
                else -> Week(lessons, inner.calculateBottomPadding()) { detail = it }
            }
        }
    }

    detail?.let { DetailSheet(it, null, {}) { detail = null } }
}

@Composable
private fun Week(lessons: List<Lesson>, bottom: androidx.compose.ui.unit.Dp, onOpen: (Lesson) -> Unit) {
    val days = lessons.groupBy { it.start.toLocalDate() }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = bottom + 24.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (days.isEmpty()) item { Column(Modifier.padding(top = 16.dp)) { Empty("Pas de cours cette semaine.") } }
        days.forEach { (day, list) ->
            item("d$day") { SectionHeader(Icons.clock, dayLabel(day), trailing = "${list.size} cours") }
            itemsIndexed(list, key = { _, l -> "l${l.id}${l.start}" }) { i, l ->
                GroupItem(i, list.size, { onOpen(l) }) { LessonRow(l) }
            }
        }
    }
}

private val WEEK = DateTimeFormatter.ofPattern("d MMMM", Locale.FRENCH)
