package com.bips.dictionary

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bips.dictionary.data.DictionaryInstaller
import com.bips.dictionary.data.DictionaryRepository
import com.bips.dictionary.data.SearchHit
import com.bips.dictionary.data.UserDatabase
import com.bips.dictionary.data.UserRepository
import com.bips.dictionary.data.toHit
import com.bips.dictionary.data.WordEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface DbState {
    data object Preparing : DbState
    data class Ready(val repo: DictionaryRepository) : DbState
    data class Failed(val message: String) : DbState
}

/** Owns the open dictionary database and the in-memory history (persisted in Phase 6). */
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val _dbState = MutableStateFlow<DbState>(DbState.Preparing)
    val dbState: StateFlow<DbState> = _dbState.asStateFlow()

    val history = mutableStateListOf<SearchHit>()
    val favourites = mutableStateListOf<SearchHit>()
    private var db: SQLiteDatabase? = null
    private val user = UserRepository(UserDatabase(app))

    init {
        load()
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            val (h, f) = withContext(Dispatchers.IO) { user.history() to user.favourites() }
            // Anything viewed while loading is already at the front of the list; keep it.
            val fresh = history.map { it.headword }.toSet()
            history.addAll(h.filter { it.headword !in fresh })
            favourites.clear()
            favourites.addAll(f)
        }
    }

    fun isFavourite(headword: String) = favourites.any { it.headword == headword }

    fun load() {
        _dbState.value = DbState.Preparing
        viewModelScope.launch {
            try {
                val opened = withContext(Dispatchers.IO) { DictionaryInstaller.open(getApplication<Application>()) }
                db = opened
                _dbState.value = DbState.Ready(DictionaryRepository(opened))
            } catch (e: Exception) {
                _dbState.value = DbState.Failed(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun recordViewed(entry: WordEntry) {
        val hit = entry.toHit()
        history.removeAll { it.headword == hit.headword }
        history.add(0, hit)
        while (history.size > UserRepository.HISTORY_LIMIT) history.removeAt(history.lastIndex)
        viewModelScope.launch(Dispatchers.IO) { user.recordViewed(hit) }
    }

    fun toggleFavourite(entry: WordEntry) {
        val hit = entry.toHit()
        if (isFavourite(hit.headword)) removeFavourite(hit.headword)
        else {
            favourites.add(0, hit)
            viewModelScope.launch(Dispatchers.IO) { user.addFavourite(hit) }
        }
    }

    fun removeFavourite(headword: String) {
        favourites.removeAll { it.headword == headword }
        viewModelScope.launch(Dispatchers.IO) { user.removeFavourite(headword) }
    }

    fun removeHistory(headword: String) {
        history.removeAll { it.headword == headword }
        viewModelScope.launch(Dispatchers.IO) { user.removeHistory(headword) }
    }

    fun clearHistory() {
        history.clear()
        viewModelScope.launch(Dispatchers.IO) { user.clearHistory() }
    }

    override fun onCleared() {
        db?.close()
        user.close()
    }
}
