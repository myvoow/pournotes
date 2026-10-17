package app.pournotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import app.pournotes.ui.HomeScreen
import app.pournotes.ui.LoginScreen
import app.pournotes.ui.PournotesTheme
import app.pournotes.ui.SettingsScreen
import app.pournotes.ui.TimetableScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val vm: AppViewModel = viewModel()
            PournotesTheme(vm.prefs) { App(vm) }
        }
    }
}

@Composable
private fun App(vm: AppViewModel) {
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    BackHandler(vm.signedIn && vm.screen != Screen.Home) { vm.screen = Screen.Home }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        // Each screen grows out of the previous one, on the theme's springs.
        val target = if (vm.signedIn) vm.screen else null
        AnimatedContent(target, transitionSpec = { (fadeIn(effects) + scaleIn(spatial, 0.92f)) togetherWith fadeOut(effects) }, label = "screen") { s ->
            when (s) {
                null -> LoginScreen(vm.busy, vm.loginError, vm::login, vm::loginQr)
                Screen.Home -> HomeScreen(
                    home = vm.home, refreshing = vm.refreshing, error = vm.homeError,
                    notice = vm.notice, onNoticeShown = { vm.notice = null },
                    onRefresh = vm::refresh, onLogout = vm::logout, onToggle = vm::toggle,
                    onTimetable = vm::openTimetable, onSettings = { vm.screen = Screen.Settings },
                )
                Screen.Timetable -> TimetableScreen(vm.monday, vm.week, vm.weekError, vm::showWeek) { vm.screen = Screen.Home }
                Screen.Settings -> SettingsScreen(vm.prefs, vm::updatePrefs, { vm.screen = Screen.Home }, vm::logout)
            }
        }
    }
}
