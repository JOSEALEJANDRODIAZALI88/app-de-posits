package com.notaflotante.app

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.math.roundToInt

class HomeNotesService : Service() {
    private val app get() = application as NotesApp
    private val store get() = app.store
    private lateinit var windows: WindowManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val overlays = mutableListOf<View>()
    private var rendered = emptyList<Note>()
    private var monitoring: Job? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        windows = getSystemService(WindowManager::class.java)
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Pósits en el inicio", NotificationManager.IMPORTANCE_LOW)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) {
            hideNotes()
            stopSelf()
            return START_NOT_STICKY
        }
        if (!Settings.canDrawOverlays(this) || !hasUsageAccess() || store.notes.none { it.pinned }) {
            hideNotes()
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION, notification())
        running = true
        if (intent?.action == SYNC) hideNotes()
        if (monitoring?.isActive != true) monitoring = scope.launch { monitor() }
        return START_NOT_STICKY
    }

    private fun notification(): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stop = PendingIntent.getService(this, 1, Intent(this, HomeNotesService::class.java).setAction(STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_note)
            .setContentTitle("Pósits solo en tu inicio")
            .setContentText("Se ocultan al abrir otras apps. Toca para administrar.")
            .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
            .addAction(Notification.Action.Builder(null, "Detener", stop).build()).build()
    }

    private suspend fun monitor() {
        val detector = HomeDetection(this)
        val power = getSystemService(PowerManager::class.java)
        val keyguard = getSystemService(KeyguardManager::class.java)
        while (currentCoroutineContext().isActive) {
            if (!Settings.canDrawOverlays(this) || !hasUsageAccess() || store.notes.none { it.pinned }) {
                hideNotes()
                stopSelf()
                return
            }
            val unlocked = power.isInteractive && !keyguard.isKeyguardLocked
            if (!unlocked || app.activityVisible) hideNotes()
            val homeVisible = try {
                withContext(Dispatchers.IO) { detector.isHomeVisible() }
            } catch (_: SecurityException) { false }
            val notes = store.notes.filter { it.pinned }
            if (homeVisible && power.isInteractive && !keyguard.isKeyguardLocked && !app.activityVisible) {
                if (rendered != notes) {
                    try { render(notes) } catch (_: SecurityException) {
                        hideNotes(); stopSelf(); return
                    }
                }
            } else hideNotes()
            delay(if (unlocked) 250 else 1_000)
        }
    }

    private fun hideNotes() {
        overlays.forEach { runCatching { windows.removeView(it) } }
        overlays.clear()
        rendered = emptyList()
    }

    private fun render(notes: List<Note>) {
        hideNotes()
        notes.asReversed().forEach { addNote(it) }
        rendered = notes
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()

    private fun availableSize(): Pair<Int, Int> {
        if (Build.VERSION.SDK_INT >= 30) {
            val metrics = windows.currentWindowMetrics
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
            )
            return (metrics.bounds.width() - insets.left - insets.right) to
                (metrics.bounds.height() - insets.top - insets.bottom)
        }
        val metrics = resources.displayMetrics
        return metrics.widthPixels to (metrics.heightPixels - dp(48)).coerceAtLeast(dp(100))
    }

    private fun addNote(note: Note) {
        val (screenWidth, screenHeight) = availableSize()
        val width = dp(if (note.wide) 280 else 210).coerceAtMost(screenWidth)
        val height = dp(if (note.collapsed) 52 else 250).coerceAtMost(screenHeight)
        val travelX = (screenWidth - width).coerceAtLeast(0)
        val travelY = (screenHeight - height).coerceAtLeast(0)
        val params = WindowManager.LayoutParams(width, height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (note.boardX * travelX).roundToInt().coerceIn(0, travelX)
            y = (note.boardY * travelY).roundToInt().coerceIn(0, travelY)
            setTitle("Posit:${note.id}")
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = PaperDrawable(NoteColors[note.color].toInt(), resources.displayMetrics.density,
                folded = !note.collapsed)
        }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val grip = TextView(this).apply {
            text = if (note.collapsed) note.title.ifBlank { "Mi nota" } else "⠿ MOVER"
            setTextColor(0xFF3A3024.toInt()); textSize = if (note.collapsed) 15f else 11f
            setTypeface(null, Typeface.BOLD); maxLines = 1; ellipsize = TextUtils.TruncateAt.END
            gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), 0, 0, 0)
            contentDescription = "Mover ${note.title.ifBlank { "nota" }}; tocar para editar"
            setOnClickListener { edit(note.id) }
        }
        header.addView(grip, LinearLayout.LayoutParams(0, dp(52), 1f))
        fun control(text: String, description: String, action: () -> Unit) = Button(this).apply {
            this.text = text; textSize = 22f; setTextColor(0xFF3A3024.toInt())
            background = null; minWidth = 0; minimumWidth = 0; setPadding(0, 0, 0, 0)
            contentDescription = description; setOnClickListener { action() }
        }
        header.addView(control(if (note.collapsed) "+" else "-", "Plegar o expandir ${note.title}") {
            store.update(note.id) { it.copy(collapsed = !it.collapsed) }; render(store.notes.filter { it.pinned })
        }, LinearLayout.LayoutParams(dp(48), dp(52)))
        header.addView(control("\u00d7", "Retirar ${note.title} del inicio") {
            store.update(note.id) { it.copy(pinned = false) }
            hideNotes() // The monitor will restore the remaining notes or stop when none remain.
        }, LinearLayout.LayoutParams(dp(48), dp(52)))
        root.addView(header)
        if (!note.collapsed) {
            val content = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; setPadding(dp(14), 0, dp(14), dp(12))
                setOnClickListener { edit(note.id) }
            }
            val title = TextView(this).apply {
                text = note.title.ifBlank { "Mi nota" }; textSize = 23f; setTextColor(0xFF3A3024.toInt())
                typeface = Typeface.create("serif", Typeface.BOLD); maxLines = 2; ellipsize = TextUtils.TruncateAt.END
            }
            val body = TextView(this).apply {
                text = note.body.ifBlank { "Toca para escribir..." }; textSize = note.fontSize.toFloat()
                setTextColor(0xFF3A3024.toInt()); ellipsize = TextUtils.TruncateAt.END
                setPadding(0, dp(6), 0, 0)
            }
            content.addView(title, LinearLayout.LayoutParams(-1, -2))
            content.addView(body, LinearLayout.LayoutParams(-1, 0, 1f))
            root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        }
        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0; var dragging = false
        val slop = ViewConfiguration.get(this).scaledTouchSlop
        grip.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY; startX = params.x; startY = params.y; dragging = false; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX; val dy = event.rawY - downY
                    if (abs(dx) > slop || abs(dy) > slop) dragging = true
                    if (dragging && root.isAttachedToWindow) {
                        params.x = (startX + dx.toInt()).coerceIn(0, travelX)
                        params.y = (startY + dy.toInt()).coerceIn(0, travelY)
                        windows.updateViewLayout(root, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (dragging) {
                        store.update(note.id, bringToFront = true) { it.copy(
                            boardX = if (travelX > 0) params.x.toFloat() / travelX else it.boardX,
                            boardY = if (travelY > 0) params.y.toFloat() / travelY else it.boardY) }
                    } else view.performClick()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    params.x = startX; params.y = startY
                    if (root.isAttachedToWindow) windows.updateViewLayout(root, params)
                    true
                }
                else -> false
            }
        }
        windows.addView(root, params)
        overlays.add(root)
    }

    private fun edit(id: String) {
        hideNotes()
        startActivity(Intent(this, MainActivity::class.java).putExtra("edit_note", id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        hideNotes()
    }

    override fun onDestroy() {
        scope.cancel(); hideNotes(); running = false; instance = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val START = "com.notaflotante.START_HOME_NOTES"
        const val SYNC = "com.notaflotante.SYNC_HOME_NOTES"
        private const val STOP = "com.notaflotante.STOP_HOME_NOTES"
        private const val CHANNEL = "home_notes"
        private const val NOTIFICATION = 101
        private var instance: HomeNotesService? = null
        var running by mutableStateOf(false)
            private set
        fun hideForEditor() { instance?.hideNotes() }
    }
}
