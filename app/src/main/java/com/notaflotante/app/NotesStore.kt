package com.notaflotante.app

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

val NoteColors = listOf(0xFFFFE082, 0xFFF8BBD0, 0xFFBDF66B, 0xFFB3E5FC, 0xFFD1C4E9)

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val body: String = "",
    val color: Int = 0,
    val fontSize: Int = 20,
    val wide: Boolean = false,
    val pinned: Boolean = true,
    val collapsed: Boolean = false,
    // Fraction of available travel, independent of display density and note size.
    val boardX: Float = 0.15f,
    val boardY: Float = 0.15f,
    // Legacy overlay pixel positions, retained only for backwards-compatible JSON.
    val x: Int = 32,
    val y: Int = 180,
)

class NotesApp : Application() {
    val store by lazy { NotesStore(this) }
    var activityVisible = false
}

class NotesStore(context: Context) {
    private val prefs = context.getSharedPreferences("sticky_notes", Context.MODE_PRIVATE)
    var notes by mutableStateOf(load())
        private set
    init {
        // Previously hidden/pinned widget notes all become available on the new free board.
        if (!prefs.getBoolean("launcher_migrated", false)) {
            notes = notes.map { it.copy(pinned = true) }
            persist()
            prefs.edit().putBoolean("launcher_migrated", true).apply()
        }
    }

    fun save(note: Note, bringToFront: Boolean = false) {
        val exists = notes.any { it.id == note.id }
        val saved = if (exists) note else note.copy(
            boardX = (notes.size % 3) * .32f,
            boardY = (notes.size % 4) * .21f,
        )
        notes = if (exists && !bringToFront) notes.map { if (it.id == saved.id) saved else it }
        else listOf(saved) + notes.filterNot { it.id == saved.id }
        persist()
    }

    fun update(id: String, bringToFront: Boolean = false, change: (Note) -> Note) {
        notes.firstOrNull { it.id == id }?.let { save(change(it), bringToFront) }
    }

    fun delete(id: String) {
        notes = notes.filterNot { it.id == id }
        persist()
    }

    private fun persist() {
        val array = JSONArray()
        notes.forEach { n ->
            array.put(JSONObject().apply {
                put("id", n.id); put("title", n.title); put("body", n.body)
                put("color", n.color); put("fontSize", n.fontSize); put("wide", n.wide)
                put("pinned", n.pinned); put("collapsed", n.collapsed)
                put("x", n.x); put("y", n.y)
                put("boardX", n.boardX); put("boardY", n.boardY)
            })
        }
        prefs.edit().putString("notes", array.toString()).apply()
    }

    private fun load(): List<Note> = runCatching {
        val array = JSONArray(prefs.getString("notes", "[]"))
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Note(
                id = o.getString("id"), title = o.optString("title"), body = o.optString("body"),
                color = o.optInt("color").coerceIn(NoteColors.indices),
                fontSize = o.optInt("fontSize", 20).coerceIn(16, 32), wide = o.optBoolean("wide"),
                pinned = o.optBoolean("pinned"), collapsed = o.optBoolean("collapsed"),
                x = o.optInt("x", 32), y = o.optInt("y", 180),
                boardX = o.optDouble("boardX", (i % 3) * .32).toFloat().let { if (it.isFinite()) it.coerceIn(0f, 1f) else 0f },
                boardY = o.optDouble("boardY", (i % 4) * .21).toFloat().let { if (it.isFinite()) it.coerceIn(0f, 1f) else 0f },
            )
        }
    }.getOrDefault(emptyList())
}
