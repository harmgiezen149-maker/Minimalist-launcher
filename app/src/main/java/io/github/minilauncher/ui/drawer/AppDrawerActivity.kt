package io.github.minilauncher.ui.drawer

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import io.github.minilauncher.R
import io.github.minilauncher.data.AppRepository
import io.github.minilauncher.data.Prefs
import io.github.minilauncher.data.model.AppEntry
import io.github.minilauncher.data.model.Folder
import io.github.minilauncher.ui.common.AppLauncher
import io.github.minilauncher.ui.common.AppLongPressDialog
import io.github.minilauncher.ui.common.BaseActivity
import io.github.minilauncher.ui.common.TextListAdapter
import io.github.minilauncher.ui.common.WheelLayoutManager
import io.github.minilauncher.util.VerticalSwipe

class AppDrawerActivity : BaseActivity() {

    override val followsPortraitLock = true

    /** Inside a folder, back returns to the folder list before it closes the drawer. */
    private val leaveFolder = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            currentFolderId = null
            rebuild()
        }
    }

    /** One drawer line: its label plus what a tap/long-press does. */
    private data class Row(
        val label: String,
        val onClick: () -> Unit,
        val onLongClick: () -> Unit = {},
    )

    private lateinit var repo: AppRepository
    private lateinit var prefs: Prefs
    private lateinit var adapter: TextListAdapter
    private lateinit var gestureDetector: GestureDetector

    private var allApps: List<AppEntry> = emptyList()
    private var rows: List<Row> = emptyList()
    private var currentFolderId: String? = null
    private var query: String = ""
    private var lastModeKey: String? = null

    private val background = java.util.concurrent.Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private val modeTick = object : Runnable {
        override fun run() {
            // Day/evening can flip while the drawer sits open.
            val state = prefs.currentModeState()
            val key = "${state.enabled}-${state.evening}"
            if (key != lastModeKey) reload()
            handler.postDelayed(this, MODE_TICK_MILLIS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drawer)
        repo = AppRepository(this)
        prefs = Prefs.get(this)

        onBackPressedDispatcher.addCallback(this, leaveFolder)

        adapter = TextListAdapter(
            onClick = { pos -> rows.getOrNull(pos)?.onClick?.invoke() },
            onLongClick = { pos -> rows.getOrNull(pos)?.onLongClick?.invoke() },
        )
        findViewById<RecyclerView>(R.id.appList).apply {
            layoutManager = WheelLayoutManager(this@AppDrawerActivity)
            adapter = this@AppDrawerActivity.adapter
            clipToPadding = false
            // Recomputed whenever the list's height changes: the keyboard
            // shrinks it while searching, and room sized for the full height
            // could then cover every row.
            addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
                if (bottom - top != oldBottom - oldTop) post { updateListRoom() }
            }
        }
        findViewById<EditText>(R.id.searchField).doAfterTextChanged {
            query = it?.toString().orEmpty()
            rebuild()
            updateListRoom()
        }

        // Swipe down while the list is at the top closes the drawer — the
        // mirror of the swipe-up that opened it.
        gestureDetector = GestureDetector(
            this,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                    if (e1 == null) return false
                    val direction = VerticalSwipe.classify(
                        dx = e2.x - e1.x,
                        dy = e2.y - e1.y,
                        velocityY = velocityY,
                        density = resources.displayMetrics.density,
                    )
                    if (direction != VerticalSwipe.Direction.DOWN) return false
                    if (!findViewById<RecyclerView>(R.id.appList).canScrollVertically(-1)) finish()
                    return true
                }
            },
        )
    }

    /**
     * Room above and below the rows, so the first folder and the last app can
     * scroll into the middle, where they render at full size. Search results
     * get none: they should sit right under the search field, and the
     * keyboard leaves little height to spare.
     */
    private fun updateListRoom() {
        val list = findViewById<RecyclerView>(R.id.appList)
        val room = if (query.isBlank()) (list.height * TOP_BOTTOM_ROOM).toInt() else 0
        if (list.paddingTop != room || list.paddingBottom != room) {
            list.setPadding(list.paddingLeft, room, list.paddingRight, room)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    override fun onResume() {
        super.onResume()
        reload()
        handler.post(modeTick)
    }

    override fun onPause() {
        handler.removeCallbacks(modeTick)
        super.onPause()
    }

    override fun onDestroy() {
        background.shutdown()
        super.onDestroy()
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.stay, R.anim.slide_out_down)
    }

    /** Package-manager work stays off the main thread so the slide-up stays smooth. */
    private fun reload() {
        val state = prefs.currentModeState()
        lastModeKey = "${state.enabled}-${state.evening}"
        background.execute {
            val apps = runCatching { repo.allApps() }.getOrDefault(emptyList())
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                allApps = apps
                rebuild()
            }
        }
    }

    private fun rebuild() {
        val q = query.trim()
        rows = when {
            // A search always spans every app, so folders never hide a result.
            q.isNotEmpty() ->
                allApps.filter { it.displayLabel.contains(q, ignoreCase = true) }.map { appRow(it) }

            currentFolderId != null -> {
                val folder = prefs.folders.firstOrNull { it.id == currentFolderId }
                if (folder == null) {
                    currentFolderId = null
                    return rebuild()
                }
                val byPackage = allApps.associateBy { it.packageName }
                buildList {
                    add(
                        Row(getString(R.string.folder_back), onClick = {
                            currentFolderId = null
                            rebuild()
                        }),
                    )
                    folder.packages.mapNotNull { byPackage[it] }
                        .sortedBy { it.displayLabel.lowercase() }
                        .forEach { add(appRow(it)) }
                }
            }

            else -> {
                val folders = prefs.folders
                val inFolder = folders.flatMap { it.packages }.toSet()
                buildList {
                    folders.sortedBy { it.name.lowercase() }.forEach { add(folderRow(it)) }
                    allApps.filter { it.packageName !in inFolder }.forEach { add(appRow(it)) }
                }
            }
        }
        adapter.submit(rows.map { it.label })
        leaveFolder.isEnabled = currentFolderId != null && q.isEmpty()
    }

    private fun folderRow(folder: Folder): Row {
        val installed = allApps.map { it.packageName }.toSet()
        val count = folder.packages.count { it in installed }
        return Row(
            label = getString(R.string.folder_label, folder.name, count),
            onClick = {
                currentFolderId = folder.id
                clearSearch()
                rebuild()
            },
            onLongClick = { showFolderMenu(folder) },
        )
    }

    private fun appRow(app: AppEntry): Row = Row(
        label = app.displayLabel,
        onClick = {
            AppLauncher.launch(this, app.packageName)
            finish()
        },
        onLongClick = { AppLongPressDialog.show(this, app) { reload() } },
    )

    private fun clearSearch() {
        query = ""
        findViewById<EditText>(R.id.searchField).text?.clear()
    }

    private fun showFolderMenu(folder: Folder) {
        val labels = arrayOf(
            getString(R.string.folder_rename),
            getString(R.string.folder_delete),
        )
        AlertDialog.Builder(this)
            .setTitle(folder.name)
            .setItems(labels) { _, which ->
                when (which) {
                    0 -> showRenameFolder(folder)
                    1 -> {
                        prefs.deleteFolder(folder.id)
                        reload()
                    }
                }
            }
            .show()
    }

    private fun showRenameFolder(folder: Folder) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(folder.name)
            setSelection(text.length)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.folder_rename)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) prefs.renameFolder(folder.id, name)
                reload()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    companion object {
        /** Share of the list height kept free above and below the rows. */
        private const val TOP_BOTTOM_ROOM = 0.32f

        /** Day/evening can flip while the drawer sits open; checked this often. */
        private const val MODE_TICK_MILLIS = 30_000L
    }
}
