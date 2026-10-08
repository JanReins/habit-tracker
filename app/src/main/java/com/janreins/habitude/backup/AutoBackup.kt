package com.janreins.habitude.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.janreins.habitude.HabitudeApplication
import com.janreins.habitude.domain.Backup
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Saves a backup file to a folder the user picked, once a week, keeping the newest few.
 * Android's own cloud backup stays off; this only writes where the user chose.
 */
object AutoBackup {
    private const val WORK_NAME = "weekly-backup"

    /** Starts the weekly backup, or stops it when [on] is false. */
    fun schedule(context: Context, on: Boolean) {
        val work = WorkManager.getInstance(context)
        if (on) {
            work.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<AutoBackupWorker>(7, TimeUnit.DAYS).build(),
            )
        } else {
            work.cancelUniqueWork(WORK_NAME)
        }
    }

    /** Keeps access to the picked folder across restarts. */
    fun keepAccess(context: Context, folder: Uri) {
        context.contentResolver.takePersistableUriPermission(
            folder,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
    }

    fun releaseAccess(context: Context, folder: Uri) {
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                folder,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
    }

    /** The folder's name as the file picker shows it, or null if it can't be read. */
    fun folderName(context: Context, folder: Uri): String? = runCatching {
        val doc = DocumentsContract.buildDocumentUriUsingTree(folder, DocumentsContract.getTreeDocumentId(folder))
        context.contentResolver.query(doc, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
    }.getOrNull()

    /**
     * Writes today's backup into the chosen folder and removes the oldest automatic ones.
     * Returns false (and remembers the failure) if there's no folder or it can't be written.
     */
    suspend fun runNow(context: Context): Boolean {
        val app = context.applicationContext as HabitudeApplication
        val folder = app.settings.autoBackupFolder?.let(Uri::parse) ?: return false
        val ok = runCatching {
            val text = Backup.export(app.repository.snapshot(), app.settings.eveningNudge, LocalDateTime.now())
            write(context, folder, Backup.autoFileName(LocalDate.now()), text)
        }.isSuccess
        app.settings.autoBackupFailed = !ok
        if (ok) app.settings.lastAutoBackup = System.currentTimeMillis()
        return ok
    }

    private fun write(context: Context, folder: Uri, name: String, text: String) {
        val resolver = context.contentResolver
        val parentId = DocumentsContract.getTreeDocumentId(folder)
        val parent = DocumentsContract.buildDocumentUriUsingTree(folder, parentId)

        // What's in the folder now, by name.
        val children = mutableMapOf<String, String>()
        resolver.query(
            DocumentsContract.buildChildDocumentsUriUsingTree(folder, parentId),
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) children[cursor.getString(1)] = cursor.getString(0)
        }
        fun delete(fileName: String) {
            val id = children[fileName] ?: return
            runCatching { DocumentsContract.deleteDocument(resolver, DocumentsContract.buildDocumentUriUsingTree(folder, id)) }
        }

        // A second backup on the same day replaces the first rather than adding "(1)" copies.
        delete(name)
        val file = DocumentsContract.createDocument(resolver, parent, "application/json", name)
            ?: error("Couldn't create the backup file")
        resolver.openOutputStream(file, "wt")!!.use { it.write(text.toByteArray()) }

        Backup.autoBackupsToDelete(children.keys.filter { it != name } + name).forEach(::delete)
    }
}

class AutoBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result =
        if (AutoBackup.runNow(applicationContext)) Result.success() else Result.failure()
}
