package com.bips.dictionary.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Favourites and history live in their own file (user.db), so replacing the dictionary never
 * touches them. Rows are keyed by headword text, not dictionary row ids (ids change on rebuild),
 * and keep a snapshot of the first sense so lists render without querying the dictionary.
 */
class UserDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "user.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE favourites (headword TEXT PRIMARY KEY NOT NULL, pos TEXT NOT NULL, " +
                "definition TEXT NOT NULL, added_at INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE history (headword TEXT PRIMARY KEY NOT NULL, pos TEXT NOT NULL, " +
                "definition TEXT NOT NULL, viewed_at INTEGER NOT NULL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
}

/** Blocking calls: run them off the main thread. */
class UserRepository(private val helper: UserDatabase) {
    private val db: SQLiteDatabase get() = helper.writableDatabase

    fun favourites(): List<SearchHit> = list("favourites", "added_at")
    fun history(): List<SearchHit> = list("history", "viewed_at")

    private fun list(table: String, orderColumn: String): List<SearchHit> =
        db.rawQuery(
            "SELECT headword, pos, definition FROM $table ORDER BY $orderColumn DESC, rowid DESC",
            null,
        ).use { c ->
            buildList { while (c.moveToNext()) add(SearchHit(c.getString(0), c.getString(1), c.getString(2))) }
        }

    fun recordViewed(hit: SearchHit) {
        db.execSQL(
            "INSERT OR REPLACE INTO history (headword, pos, definition, viewed_at) VALUES (?, ?, ?, ?)",
            arrayOf<Any?>(hit.headword, hit.pos, hit.definition, System.currentTimeMillis()),
        )
        db.execSQL(
            "DELETE FROM history WHERE headword NOT IN " +
                "(SELECT headword FROM history ORDER BY viewed_at DESC, rowid DESC LIMIT $HISTORY_LIMIT)"
        )
    }

    fun addFavourite(hit: SearchHit) {
        db.execSQL(
            "INSERT OR REPLACE INTO favourites (headword, pos, definition, added_at) VALUES (?, ?, ?, ?)",
            arrayOf<Any?>(hit.headword, hit.pos, hit.definition, System.currentTimeMillis()),
        )
    }

    fun removeFavourite(headword: String) { db.delete("favourites", "headword = ?", arrayOf(headword)) }
    fun removeHistory(headword: String) { db.delete("history", "headword = ?", arrayOf(headword)) }
    fun clearHistory() { db.delete("history", null, null) }
    fun close() = helper.close()

    companion object { const val HISTORY_LIMIT = 100 }
}

fun WordEntry.toHit(): SearchHit {
    val first = senses.first()
    return SearchHit(headword, first.pos, first.definition)
}
