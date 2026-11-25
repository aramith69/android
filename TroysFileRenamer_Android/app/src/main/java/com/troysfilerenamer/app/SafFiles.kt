package com.troysfilerenamer.app

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import java.util.concurrent.atomic.AtomicLong

/**
 * File access through Android's Storage Access Framework. The user picks a folder with the
 * system folder picker and Android grants this app access to that folder only, so no storage
 * permissions are needed. USB drives and SD cards work the same way.
 */
class SafFiles(private val resolver: ContentResolver) {

    private val nextId = AtomicLong(1)

    private class Entry(val docId: String, val name: String)

    fun displayName(docUri: Uri): String? = try {
        resolver.query(
            docUri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null
        )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    } catch (e: Exception) {
        null
    }

    fun rootName(tree: Uri): String {
        val docId = DocumentsContract.getTreeDocumentId(tree)
        val docUri = DocumentsContract.buildDocumentUriUsingTree(tree, docId)
        return displayName(docUri)
            ?: docId.substringAfterLast(':').substringAfterLast('/').ifBlank { "Storage" }
    }

    /** Walks the chosen folder and everything below it. Call from a background thread. */
    fun scan(scope: CoroutineScope, tree: Uri, onProgress: (String) -> Unit): List<Candidate> {
        val out = ArrayList<Candidate>()
        val rootId = DocumentsContract.getTreeDocumentId(tree)
        walk(scope, tree, rootId, listOf(rootName(tree)), out, onProgress)
        return out
    }

    private fun walk(
        scope: CoroutineScope,
        tree: Uri,
        parentId: String,
        path: List<String>,
        out: MutableList<Candidate>,
        onProgress: (String) -> Unit,
    ) {
        scope.ensureActive()

        val files = ArrayList<Entry>()
        val dirs = ArrayList<Entry>()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
        try {
            resolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                ),
                null, null, null
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0) ?: continue
                    val name = c.getString(1) ?: continue
                    val mime = c.getString(2)
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        dirs.add(Entry(id, name))
                    } else {
                        files.add(Entry(id, name))
                    }
                }
            }
        } catch (e: Exception) {
            return   // folder could not be read; skip it
        }

        val videos = files.filter { Naming.isVideo(it.name) }
        if (videos.isNotEmpty()) {
            onProgress(path.joinToString(" / "))
            val show = Naming.showName(path)
            if (show.isNotBlank()) {
                val dirNames = HashSet<String>()
                files.forEach { dirNames.add(it.name.lowercase()) }
                dirs.forEach { dirNames.add(it.name.lowercase()) }
                val dirKey = tree.toString() + "|" + parentId
                val dirLabel = path.joinToString(" / ")
                for (v in videos.sortedBy { it.name }) {
                    val se = Naming.parseSeasonEpisode(v.name) ?: continue
                    out.add(
                        Candidate(
                            id = nextId.getAndIncrement(),
                            docUri = DocumentsContract.buildDocumentUriUsingTree(tree, v.docId),
                            dirKey = dirKey,
                            dirLabel = dirLabel,
                            show = show,
                            oldName = v.name,
                            newName = Naming.buildNewName(show, se.first, se.second, v.name),
                            season = se.first,
                            episode = se.second,
                            dirNames = dirNames,
                        )
                    )
                }
            }
        }

        for (d in dirs.sortedBy { it.name }) {
            walk(scope, tree, d.docId, path + d.name, out, onProgress)
        }
    }

    /** Renames one file. Never overwrites: a name that already exists is skipped. */
    fun rename(c: Candidate): Pair<Boolean, String> {
        val newLower = c.newName.lowercase()
        val oldLower = c.oldName.lowercase()
        if (newLower in c.dirNames && newLower != oldLower) {
            return false to "Already exists"
        }
        return try {
            val result = DocumentsContract.renameDocument(resolver, c.docUri, c.newName)
            if (result == null) {
                false to "This storage does not allow renaming"
            } else {
                c.dirNames.remove(oldLower)
                c.dirNames.add(newLower)
                val actual = displayName(result)
                if (actual != null && actual != c.newName) true to "Saved as $actual" else true to "Renamed"
            }
        } catch (e: Exception) {
            false to (e.message ?: "Rename failed")
        }
    }
}
