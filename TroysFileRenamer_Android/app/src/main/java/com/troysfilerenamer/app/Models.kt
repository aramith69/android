package com.troysfilerenamer.app

import android.net.Uri

/** One video file that has a recognisable season/episode number. */
data class Candidate(
    val id: Long,
    val docUri: Uri,
    val dirKey: String,
    val dirLabel: String,
    val show: String,
    val oldName: String,
    val newName: String,
    val season: Int,
    val episode: Int,
    /** Lower-cased names already in the same folder, used to avoid overwriting anything. */
    val dirNames: MutableSet<String>,
) {
    val changed: Boolean get() = oldName != newName
}

data class FolderEntry(val uri: Uri, val label: String)
