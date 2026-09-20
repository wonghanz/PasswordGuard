package dev.capriguard.passwordguard

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.capriguard.passwordguard.audit.PasswordGuardViewModel
import dev.capriguard.passwordguard.core.GuardTheme
import dev.capriguard.passwordguard.ui.HomeScreen
import dev.capriguard.passwordguard.ui.SettingsScreen

private enum class Route { Home, Settings }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // A screen whose whole job is holding a secret should not be a thumbnail in
        // the recents switcher, and should not survive a screenshot of itself.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContent { GuardTheme { PasswordGuardRoot() } }
    }
}

@Composable
fun PasswordGuardRoot(vm: PasswordGuardViewModel = viewModel()) {
    var route by remember { mutableStateOf(Route.Home) }
    val owner = LocalLifecycleOwner.current

    // Leaving the app — recents, home, another task — clears the field. Whatever
    // Android holds in memory for the String is beyond us, but nothing of ours stays.
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) vm.wipe() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    // Back leaves Settings; from Home the hardware and gesture back close the app.
    BackHandler(enabled = route == Route.Settings) { route = Route.Home }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = route,
            transitionSpec = {
                if (targetState == Route.Settings) {
                    (slideInHorizontally(tween(230)) { it / 5 } + fadeIn(tween(140))) togetherWith
                        (slideOutHorizontally(tween(230)) { -it / 6 } + fadeOut(tween(120)))
                } else {
                    (slideInHorizontally(tween(230)) { -it / 6 } + fadeIn(tween(140))) togetherWith
                        (slideOutHorizontally(tween(230)) { it / 5 } + fadeOut(tween(120)))
                }
            },
            label = "route",
        ) { current ->
            when (current) {
                Route.Home -> HomeScreen(vm = vm, onOpenSettings = { route = Route.Settings })
                Route.Settings -> SettingsScreen(vm = vm, onBack = { route = Route.Home })
            }
        }
    }
}
