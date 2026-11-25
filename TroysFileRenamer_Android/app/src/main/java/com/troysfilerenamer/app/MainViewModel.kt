package com.troysfilerenamer.app

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val files = SafFiles(app.contentResolver)
    private var scanJob: Job? = null

    val folders = mutableStateListOf<FolderEntry>()

    var candidates by mutableStateOf<List<Candidate>>(emptyList())
        private set
    var selected by mutableStateOf<Set<Long>>(emptySet())
        private set
    var statuses by mutableStateOf<Map<Long, String>>(emptyMap())
        private set
    var scanning by mutableStateOf(false)
        private set
    var renaming by mutableStateOf(false)
        private set
    var progress by mutableStateOf("")
        private set
    var renamedTotal by mutableStateOf(0)
        private set
    var showOnlyChanges by mutableStateOf(true)
    var message by mutableStateOf<String?>(null)

    fun isDone(id: Long): Boolean = statuses[id]?.startsWith("✓") == true

    fun addFolder(uri: Uri) {
        if (folders.any { it.uri == uri }) return
        try {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: Exception) {
            // Permission lasts for this session even if it can't be kept.
        }
        viewModelScope.launch(Dispatchers.IO) {
            val label = files.rootName(uri)
            withContext(Dispatchers.Main) {
                if (folders.none { it.uri == uri }) folders.add(FolderEntry(uri, label))
            }
        }
    }

    fun removeFolder(entry: FolderEntry) {
        folders.remove(entry)
    }

    fun startScan() {
        if (folders.isEmpty()) {
            message = "Add at least one folder first."
            return
        }
        scanJob?.cancel()
        candidates = emptyList()
        selected = emptySet()
        statuses = emptyMap()
        progress = ""
        scanning = true
        val roots = folders.map { it.uri }
        scanJob = viewModelScope.launch(Dispatchers.IO) {
            val found = ArrayList<Candidate>()
            try {
                for (root in roots) {
                    found.addAll(files.scan(this, root) { p -> progress = p })
                }
                val changed = found.count { it.changed }
                withContext(Dispatchers.Main) {
                    candidates = found
                    selected = found.filter { it.changed }.map { it.id }.toSet()
                    message = "Found ${found.size} episode files, $changed to rename"
                }
            } finally {
                withContext(NonCancellable + Dispatchers.Main) { scanning = false }
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
    }

    fun toggle(id: Long) {
        selected = if (id in selected) selected - id else selected + id
    }

    fun selectAll() {
        selected = candidates.filter { it.changed && !isDone(it.id) }.map { it.id }.toSet()
    }

    fun selectNone() {
        selected = emptySet()
    }

    fun clearResults() {
        candidates = emptyList()
        selected = emptySet()
        statuses = emptyMap()
        renamedTotal = 0
    }

    fun renameSelected() {
        val todo = candidates.filter { it.id in selected && it.changed && !isDone(it.id) }
        if (todo.isEmpty() || renaming) return
        renaming = true
        viewModelScope.launch(Dispatchers.IO) {
            var ok = 0
            var bad = 0
            val updated = statuses.toMutableMap()
            try {
                for (c in todo) {
                    val (success, text) = files.rename(c)
                    updated[c.id] = (if (success) "✓ " else "✗ ") + text
                    if (success) ok++ else bad++
                    statuses = updated.toMap()
                }
            } finally {
                withContext(NonCancellable + Dispatchers.Main) {
                    val doneIds = todo.map { it.id }.toSet()
                    selected = selected - doneIds
                    renamedTotal += ok
                    renaming = false
                    message = "Renamed $ok file" + (if (ok == 1) "" else "s") +
                        (if (bad > 0) ", $bad skipped or failed" else "")
                }
            }
        }
    }
}
