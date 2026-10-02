package com.bips.dictionary

import android.net.Uri
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bips.dictionary.data.SampleData
import com.bips.dictionary.ui.screens.HomeScreen
import com.bips.dictionary.ui.screens.SearchScreen
import com.bips.dictionary.ui.screens.ThemeMode
import com.bips.dictionary.ui.screens.WordScreen

@Composable
fun DictionaryApp(themeMode: ThemeMode, onCycleTheme: () -> Unit) {
    val nav = rememberNavController()
    val recents = remember { mutableStateListOf<String>().apply { addAll(SampleData.initialRecents) } }

    fun openWord(word: String) {
        SampleData.find(word)?.let { found ->
            recents.remove(found.headword)
            recents.add(0, found.headword)
            while (recents.size > 10) recents.removeAt(recents.lastIndex)
        }
        nav.navigate("word/${Uri.encode(word)}")
    }

    // No custom motion in the basic version: transitions are switched off.
    NavHost(
        navController = nav,
        startDestination = "home",
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        composable("home") {
            HomeScreen(
                wordOfTheDay = SampleData.find(SampleData.WORD_OF_THE_DAY)!!,
                recents = recents.mapNotNull { SampleData.find(it) },
                themeMode = themeMode,
                onCycleTheme = onCycleTheme,
                onSearch = { nav.navigate("search") },
                onOpenWord = ::openWord,
            )
        }
        composable("search") {
            SearchScreen(onBack = { nav.popBackStack() }, onOpenWord = ::openWord)
        }
        composable(
            route = "word/{headword}",
            arguments = listOf(navArgument("headword") { type = NavType.StringType }),
        ) { entry ->
            val word = entry.arguments?.getString("headword").orEmpty()
            WordScreen(
                requested = word,
                entry = SampleData.find(word),
                onBack = { nav.popBackStack() },
                onOpenWord = ::openWord,
            )
        }
    }
}
