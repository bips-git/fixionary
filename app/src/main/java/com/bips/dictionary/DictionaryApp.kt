package com.bips.dictionary

import android.net.Uri
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import android.provider.Settings
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bips.dictionary.data.DictionaryRepository
import com.bips.dictionary.data.WordEntry
import com.bips.dictionary.data.WordLoad
import com.bips.dictionary.speech.rememberSpeaker
import com.bips.dictionary.ui.screens.FailedScreen
import com.bips.dictionary.ui.screens.FavouritesScreen
import com.bips.dictionary.ui.screens.HistoryScreen
import com.bips.dictionary.ui.screens.HomeScreen
import com.bips.dictionary.ui.screens.PreparingScreen
import com.bips.dictionary.ui.screens.SearchScreen
import com.bips.dictionary.ui.screens.SettingsScreen
import com.bips.dictionary.ui.screens.SettingsState
import com.bips.dictionary.ui.screens.WordScreen
import java.time.LocalDate

@Composable
fun DictionaryApp(settings: SettingsState, vm: AppViewModel = viewModel()) {
    val state by vm.dbState.collectAsStateWithLifecycle()
    when (val s = state) {
        DbState.Preparing -> PreparingScreen()
        is DbState.Failed -> FailedScreen(s.message, onRetry = vm::load)
        is DbState.Ready -> DictionaryNav(s.repo, vm, settings)
    }
}

private fun NavHostController.openWord(word: String) = navigate("word/${Uri.encode(word)}")

@Composable
private fun DictionaryNav(
    repo: DictionaryRepository,
    vm: AppViewModel,
    settings: SettingsState,
) {
    val nav = rememberNavController()
    val speaker = rememberSpeaker()
    LaunchedEffect(settings.accent) { speaker.setAccent(settings.accent.locale) }
    val wordOfTheDay: State<WordEntry?> = produceState<WordEntry?>(null, repo) {
        value = repo.wordOfTheDay(LocalDate.now().toEpochDay())
    }

    // Short, quiet transitions. Skipped entirely when the phone's animation scale is off.
    val context = LocalContext.current
    val animate = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
    NavHost(
        navController = nav,
        startDestination = "home",
        enterTransition = {
            if (animate) fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 14 } else EnterTransition.None
        },
        exitTransition = { if (animate) fadeOut(tween(120)) else ExitTransition.None },
        popEnterTransition = { if (animate) fadeIn(tween(200)) else EnterTransition.None },
        popExitTransition = {
            if (animate) fadeOut(tween(160)) + slideOutHorizontally(tween(220)) { it / 14 } else ExitTransition.None
        },
    ) {
        composable("home") {
            HomeScreen(
                wordOfTheDay = wordOfTheDay.value,
                recents = vm.history.take(10),
                onOpenSettings = { nav.navigate("settings") },
                onSearch = { nav.navigate("search") },
                onOpenWord = nav::openWord,
                onOpenFavourites = { nav.navigate("favourites") },
                onOpenHistory = { nav.navigate("history") },
            )
        }
        composable("settings") {
            SettingsScreen(settings = settings, onBack = { nav.popBackStack() })
        }
        composable("favourites") {
            FavouritesScreen(
                items = vm.favourites.toList(),
                onBack = { nav.popBackStack() },
                onOpenWord = nav::openWord,
                onRemove = vm::removeFavourite,
            )
        }
        composable("history") {
            HistoryScreen(
                items = vm.history.toList(),
                onBack = { nav.popBackStack() },
                onOpenWord = nav::openWord,
                onRemove = vm::removeHistory,
                onClear = vm::clearHistory,
            )
        }
        composable("search") {
            SearchScreen(repo = repo, onBack = { nav.popBackStack() }, onOpenWord = nav::openWord)
        }
        composable(
            route = "word/{headword}",
            arguments = listOf(navArgument("headword") { type = NavType.StringType }),
        ) { backStack ->
            val word = backStack.arguments?.getString("headword").orEmpty()
            val load by produceState<WordLoad>(WordLoad.Loading, word, repo) {
                value = repo.entry(word)?.let { WordLoad.Found(it) } ?: WordLoad.NotFound
            }
            WordScreen(
                requested = word,
                load = load,
                onBack = { nav.popBackStack() },
                onOpenWord = nav::openWord,
                onViewed = vm::recordViewed,
                canSpeak = speaker.ready,
                onSpeak = speaker::speak,
                isFavourite = vm.isFavourite(word),
                onToggleFavourite = vm::toggleFavourite,
            )
        }
    }
}
