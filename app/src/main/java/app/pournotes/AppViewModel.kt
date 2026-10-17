package app.pournotes

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.pournotes.data.Creds
import app.pournotes.data.Grades
import app.pournotes.data.Homework
import app.pournotes.data.Lesson
import app.pournotes.data.Profile
import app.pournotes.data.Pronote
import app.pournotes.data.PronoteException
import app.pournotes.data.Vault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import android.content.Context
import app.pournotes.ui.Prefs
import app.pournotes.ui.loadPrefs
import app.pournotes.ui.save
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job

/** What the home screen shows. Homework and grades are null when the account has no access to them. */
enum class Screen { Home, Timetable, Settings }

data class Home(val profile: Profile, val lessons: List<Lesson>, val homework: List<Homework>?, val grades: Grades?)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val vault = Vault(app)
    private var creds: Creds? = vault.load()
    private var session: Pronote? = null
    private var remember = creds != null

    var signedIn by mutableStateOf(creds != null); private set
    var busy by mutableStateOf(false); private set
    var loginError by mutableStateOf<String?>(null); private set

    var home by mutableStateOf<Home?>(null); private set
    var refreshing by mutableStateOf(false); private set
    var homeError by mutableStateOf<String?>(null); private set

    /** One-shot message for the snackbar. */
    var notice by mutableStateOf<String?>(null)

    var screen by mutableStateOf(Screen.Home)

    private val sp = app.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    var prefs by mutableStateOf(loadPrefs(sp)); private set

    fun updatePrefs(p: Prefs) {
        prefs = p
        p.save(sp)
    }

    /** The timetable screen: the Monday it shows, and that week's lessons once loaded. */
    var monday by mutableStateOf(thisWeek()); private set
    var week by mutableStateOf<List<Lesson>?>(null); private set
    var weekError by mutableStateOf<String?>(null); private set
    private var weekJob: Job? = null

    fun openTimetable() {
        screen = Screen.Timetable
        if (week == null) showWeek(monday)
    }

    fun showWeek(m: LocalDate) {
        monday = m
        week = null
        weekError = null
        weekJob?.cancel()
        weekJob = viewModelScope.launch {
            try {
                week = withContext(Dispatchers.IO) {
                    withSession { p -> p.lessons(p.week(m)).distinctBy { it.id to it.start }.sortedBy { it.start } }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                weekError = e.friendly()
            }
        }
    }

    init {
        if (signedIn) refresh()
    }

    fun login(url: String, user: String, pass: String, remember: Boolean) =
        signIn(remember) { Pronote.connect(url, user, pass) to Creds(url, user, pass) }

    /** [qr]: the QR code's text; [pin]: the 4 digits chosen when it was made. */
    fun loginQr(qr: String, pin: String, remember: Boolean) = signIn(remember) {
        val id = vault.deviceId
        val p = Pronote.fromQr(qr, pin, id)
        p to Creds(p.url, p.username, "", id)
    }

    private fun signIn(remember: Boolean, connect: () -> Pair<Pronote, Creds>) {
        busy = true
        loginError = null
        this.remember = remember
        if (!remember) vault.clear()
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { connect() } }
                .onSuccess { (p, c) ->
                    adopt(p, c)
                    signedIn = true
                    refresh()
                }
                .onFailure { loginError = it.friendly() }
            busy = false
        }
    }

    /** Mobile-app tokens are single use: keep the one this login handed out. */
    private fun adopt(p: Pronote, c: Creds) {
        session = p
        creds = c.copy(pass = p.token ?: c.pass).also { if (remember) vault.save(it) }
    }

    fun refresh() {
        if (refreshing) return
        refreshing = true
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { load() } }
                .onSuccess { home = it; homeError = null }
                .onFailure { homeError = it.friendly() }
            refreshing = false
        }
    }

    /** Ticks a homework at once, then tells PRONOTE; puts it back if PRONOTE refuses. */
    fun toggle(hw: Homework) {
        val done = !hw.done
        update(hw.id, done)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { withSession { it.setDone(hw.id, done) } } }
                .onFailure { update(hw.id, !done); notice = "Impossible d'enregistrer : ${it.friendly()}" }
        }
    }

    fun logout() {
        vault.clear()
        creds = null
        session = null
        home = null
        homeError = null
        week = null
        monday = thisWeek()
        screen = Screen.Home
        signedIn = false
    }

    private fun update(id: String, done: Boolean) {
        home = home?.let { h -> h.copy(homework = h.homework?.map { if (it.id == id) it.copy(done = done) else it }) }
    }

    /** A kept session may have expired on the server: on any failure, log in again once. */
    private fun load(): Home = withSession(::fetch)

    private fun <T> withSession(block: (Pronote) -> T): T {
        session?.let { s -> runCatching { return block(s) } }
        return block(reconnect())
    }

    private fun reconnect(): Pronote {
        val c = creds ?: throw PronoteException("Reconnecte-toi.")
        return Pronote.connect(c.url, c.user, c.pass, c.uuid).also { adopt(it, c) }
    }

    private fun fetch(p: Pronote): Home {
        val today = LocalDate.now()
        val week = p.week(today)
        val lessons = (p.lessons(week) + p.lessons(week + 1))
            .filter { !it.end.toLocalDate().isBefore(today) }
            .distinctBy { it.id to it.start }
            .sortedBy { it.start }
        val homework = runCatching {
            p.homework(week, week + 2).distinctBy { it.id }.filter { !it.due.isBefore(today) }.sortedWith(compareBy({ it.due }, { it.subject }))
        }.getOrNull()
        val grades = runCatching { p.grades() }.getOrNull()
        return Home(p.profile(), lessons, homework, grades)
    }
}

/** This week's Monday, or next week's from Saturday on. */
fun thisWeek(): LocalDate = LocalDate.now().let { if (it.dayOfWeek >= DayOfWeek.SATURDAY) it.plusWeeks(1) else it }
    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

private fun Throwable.friendly(): String = when (this) {
    is PronoteException -> message.orEmpty()
    is IOException -> "Serveur PRONOTE injoignable. Vérifie ta connexion et l'adresse."
    else -> message ?: "Erreur inattendue."
}
