package com.bips.dictionary.data

import android.content.Context
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import java.io.File

/**
 * Copies assets/dictionary.db into app storage on first launch (and again after every app update,
 * so a rebuilt dictionary is picked up), then opens it read-only. Blocking: call off the main thread.
 */
object DictionaryInstaller {
    private const val ASSET = "dictionary.db"

    fun open(context: Context): SQLiteDatabase {
        val dir = context.noBackupFilesDir          // 76 MB must not go into auto-backup
        val dbFile = File(dir, ASSET)
        val stampFile = File(dir, "dictionary.stamp")
        val stamp = installStamp(context)

        if (!dbFile.exists() || !stampFile.exists() || stampFile.readText() != stamp) {
            val tmp = File(dir, "dictionary.db.tmp")
            tmp.delete()                              // a half-written file from an interrupted copy
            try {
                context.assets.open(ASSET).use { input ->
                    tmp.outputStream().use { out -> input.copyTo(out, 256 * 1024) }
                }
            } catch (e: java.io.IOException) {
                tmp.delete()
                throw java.io.IOException("Could not prepare the dictionary. Free up some storage and try again.", e)
            }
            if (dbFile.exists()) dbFile.delete()
            check(tmp.renameTo(dbFile)) { "Could not install the dictionary file" }
            stampFile.writeText(stamp)
        }
        return SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
    }

    /** Changes whenever the app is installed or updated. */
    @Suppress("DEPRECATION")
    private fun installStamp(context: Context): String {
        val pm = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= 33) {
            pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            pm.getPackageInfo(context.packageName, 0)
        }
        return info.lastUpdateTime.toString()
    }
}
