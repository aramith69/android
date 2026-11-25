package com.troysfilerenamer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TroyTheme {
                AppRoot()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(vm: MainViewModel = viewModel()) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showPreview by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.addFolder(uri)
    }

    LaunchedEffect(vm.message) {
        val m = vm.message
        if (m != null) {
            snackbar.showSnackbar(m)
            vm.message = null
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Troy's File Renamer", fontWeight = FontWeight.ExtraBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(
                selectedTabIndex = tab,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("RENAMER") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("ABOUT") })
            }
            if (tab == 0) {
                RenamerScreen(
                    vm = vm,
                    onAddFolder = { picker.launch(null) },
                    onPreview = { showPreview = true },
                )
            } else {
                AboutScreen()
            }
        }
    }

    if (showPreview) {
        val rows = vm.candidates.filter { it.id in vm.selected && it.changed && !vm.isDone(it.id) }
        if (rows.isNotEmpty()) {
            PreviewDialog(
                rows = rows,
                onConfirm = {
                    showPreview = false
                    vm.renameSelected()
                },
                onDismiss = { showPreview = false },
            )
        }
    }
}

@Composable
fun RenamerScreen(vm: MainViewModel, onAddFolder: () -> Unit, onPreview: () -> Unit) {
    val visible = remember(vm.candidates, vm.showOnlyChanges) {
        vm.candidates.filter { !vm.showOnlyChanges || it.changed }
    }
    val groups = remember(visible) { visible.groupBy { it.dirKey } }
    val toRename = vm.candidates.count { it.changed && !vm.isDone(it.id) }
    val selectedCount = vm.candidates.count { it.id in vm.selected && it.changed && !vm.isDone(it.id) }

    Column(Modifier.fillMaxSize()) {

        // ---- folders ----
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "FOLDERS TO SCAN",
                    style = MaterialTheme.typography.labelMedium,
                    color = DimText,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = onAddFolder) { Text("+ Add folder") }
            }
            if (vm.folders.isEmpty()) {
                Text(
                    "Tap “Add folder”, then choose a folder or drive that holds your TV shows " +
                        "(internal storage, SD card or USB drive).",
                    style = MaterialTheme.typography.bodySmall,
                    color = DimText,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            vm.folders.forEach { f ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "📁  ${f.label}",
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    TextButton(onClick = { vm.removeFolder(f) }) { Text("✕") }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (vm.scanning) {
                    OutlinedButton(onClick = { vm.cancelScan() }) { Text("CANCEL SCAN") }
                } else {
                    Button(onClick = { vm.startScan() }, enabled = !vm.renaming) { Text("SCAN") }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = vm.showOnlyChanges, onCheckedChange = { vm.showOnlyChanges = it })
                    Text("  Only changes", style = MaterialTheme.typography.bodySmall, color = DimText)
                }
            }
            if (vm.scanning) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(
                    vm.progress,
                    style = MaterialTheme.typography.bodySmall,
                    color = DimText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // ---- stats ----
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatBox("TOTAL", vm.candidates.size, Color(0xFF8880FF), Modifier.weight(1f))
            StatBox("TO RENAME", toRename, PurpleAccent, Modifier.weight(1f))
            StatBox("RENAMED", vm.renamedTotal, Color(0xFF00E876), Modifier.weight(1f))
        }

        // ---- results ----
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (visible.isEmpty() && !vm.scanning) {
                item(key = "empty") {
                    Text(
                        if (vm.candidates.isEmpty()) "Nothing scanned yet." else "Every file already has the right name.",
                        color = DimText,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }
            groups.forEach { (key, list) ->
                item(key = "h:$key") {
                    Column(Modifier.padding(top = 12.dp, bottom = 4.dp)) {
                        Text(list.first().show, color = Color(0xFFA090FF), fontWeight = FontWeight.Bold)
                        Text(
                            list.first().dirLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = DimText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                items(list, key = { it.id }) { c ->
                    CandidateRow(c, vm)
                }
            }
        }

        // ---- bottom bar ----
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { vm.selectAll() }) { Text("All") }
            TextButton(onClick = { vm.selectNone() }) { Text("None") }
            TextButton(onClick = { vm.clearResults() }) { Text("Clear") }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onPreview,
                enabled = selectedCount > 0 && !vm.renaming && !vm.scanning,
            ) {
                Text(if (vm.renaming) "RENAMING…" else "PREVIEW & RENAME ($selectedCount)")
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value.toString(), color = color, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelSmall, color = DimText)
        }
    }
}

@Composable
fun CandidateRow(c: Candidate, vm: MainViewModel) {
    val done = vm.isDone(c.id)
    val canSelect = c.changed && !done
    val status = vm.statuses[c.id]
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = canSelect) { vm.toggle(c.id) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = c.id in vm.selected,
            onCheckedChange = { vm.toggle(c.id) },
            enabled = canSelect,
        )
        Column(Modifier.weight(1f)) {
            Text(
                c.oldName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (c.changed) {
                Text(
                    "→ ${c.newName}",
                    color = GreenNew,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text("Already correct", style = MaterialTheme.typography.bodySmall, color = DimText)
            }
            if (status != null) {
                Text(
                    status,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (status.startsWith("✓")) GreenNew else WarnText,
                )
            }
        }
    }
}

@Composable
fun PreviewDialog(rows: List<Candidate>, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Preview: ${rows.size} file" + if (rows.size == 1) "" else "s") },
        text = {
            Column {
                Text(
                    "Review the new names. Nothing changes until you confirm, and renaming can't be undone easily.",
                    style = MaterialTheme.typography.bodySmall,
                    color = DimText,
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(rows, key = { it.id }) { c ->
                        Column(Modifier.padding(vertical = 6.dp)) {
                            Text(
                                c.oldName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text("→ ${c.newName}", color = GreenNew, fontWeight = FontWeight.SemiBold)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Rename ${rows.size}") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun AboutScreen() {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("How Troy's File Renamer works", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        AboutCard(
            "STEP 1 — SHOW NAME",
            "The name of the folder holding your video files becomes the show name. Dots, underscores, " +
                "years like (2021) and tags like 1080p, BluRay or x264 are removed.\n\n" +
                "My.Show.1080p.BluRay  →  My Show\n\n" +
                "If the files are inside a folder called “Season 01” or “Specials”, the parent folder's name is used instead.",
        )
        AboutCard(
            "STEP 2 — SEASON & EPISODE",
            "The app looks for these patterns in each file name:\n\n" +
                "S01E04   (standard)\n1x04   (season × episode)\nSeason 1 Episode 4   (written out)\n" +
                "104   (compact, ignoring things like 1080, 2021 and x264)\n\n" +
                "Files with no recognisable pattern are skipped and left untouched.",
        )
        AboutCard(
            "STEP 3 — NEW FILE NAME",
            "Every file gets the same format:\n\nShow Name - S01E04.mkv\n\n" +
                "my.show.s01e04.1080p.mkv  →  My Show - S01E04.mkv\n\nThe file type is always kept.",
        )
        AboutCard(
            "VIDEO TYPES",
            "mkv, mp4, avi, mov, wmv, m4v, ts, mpg, mpeg, flv and webm. Subtitles, images and every other " +
                "kind of file are never touched.",
        )
        AboutCard(
            "SAFETY",
            "Nothing is renamed until you tick the files you want and confirm the preview. " +
                "A file is never overwritten: if the new name already exists, that file is skipped.",
        )
        AboutCard(
            "FOLDER ACCESS",
            "Android only lets an app change files in folders you give it access to. Tap “Add folder” and " +
                "choose the folder or drive to scan. The app has no other storage permission, no internet " +
                "access and collects no data. Some cloud storage providers do not allow renaming; those files will show an error.",
        )
        Text(
            "Troy's File Renamer v1.0.0",
            style = MaterialTheme.typography.labelSmall,
            color = DimText,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
    }
}

@Composable
fun AboutCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box(Modifier.padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = Color(0xFF8880FF), fontWeight = FontWeight.ExtraBold)
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
