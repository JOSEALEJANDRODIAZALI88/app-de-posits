package com.notaflotante.app

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private val app get() = application as NotesApp
    private val store get() = app.store
    private var editingId by mutableStateOf<String?>(null)
    private var editorOpen by mutableStateOf(false)
    private var overlayAllowed by mutableStateOf(false)
    private var usageAllowed by mutableStateOf(false)
    private var homeAvailable by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(0xFFF5EFE3.toInt(), 0xFFF5EFE3.toInt()),
            navigationBarStyle = SystemBarStyle.light(0xFFF5EFE3.toInt(), 0xFFF5EFE3.toInt()),
        )
        editingId = savedInstanceState?.getString("editingId")
        editorOpen = savedInstanceState?.getBoolean("editorOpen") ?: false
        acceptEdit(intent)
        setContent { PostitTheme { NotesScreen() } }
    }

    override fun onStart() {
        super.onStart()
        app.activityVisible = true
        HomeNotesService.hideForEditor()
    }

    override fun onStop() {
        app.activityVisible = false
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        overlayAllowed = Settings.canDrawOverlays(this)
        usageAllowed = hasUsageAccess()
        homeAvailable = originalHomePackage() != null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("editingId", editingId)
        outState.putBoolean("editorOpen", editorOpen)
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acceptEdit(intent)
    }

    private fun acceptEdit(intent: Intent) {
        intent.getStringExtra("edit_note")?.let { id ->
            editingId = id
            editorOpen = store.notes.any { it.id == id }
            intent.removeExtra("edit_note")
        }
    }

    private fun openSetting(intent: Intent) {
        try { startActivity(intent) } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "Abre este permiso desde Ajustes > Aplicaciones > Acceso especial.", Toast.LENGTH_LONG).show()
        }
    }

    private fun sync() {
        if (HomeNotesService.running) startService(Intent(this, HomeNotesService::class.java).setAction(HomeNotesService.SYNC))
    }

    @Composable
    private fun NotesScreen() {
        var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
        val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
        val enabled = overlayAllowed && usageAllowed && homeAvailable && store.notes.any { it.pinned }
        Scaffold(containerColor = Paper, floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { editingId = null; editorOpen = true }, containerColor = Accent) {
                Text("+ Nuevo pósit", color = androidx.compose.ui.graphics.Color.White)
            }
        }) { padding ->
            LazyColumn(Modifier.fillMaxSize().boardTexture().padding(padding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    Text("Mis Pósits", fontFamily = FontFamily.Serif, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    Text("En el inicio de siempre, sin reemplazarlo.", color = Muted)
                }
                item {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(if (HomeNotesService.running) "Detección del inicio activa" else "Pósits en tu pantalla de inicio", fontWeight = FontWeight.Bold)
                            Text("Conserva tus iconos y tu fondo. Las notas aparecen sobre tu inicio y se ocultan al abrir otras apps. La detección puede tardar un momento.", fontSize = 13.sp, color = Muted)
                            PermissionRow("1. Mostrar sobre otras apps", overlayAllowed) {
                                openSetting(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                            }
                            PermissionRow("2. Acceso al uso de apps", usageAllowed) {
                                openSetting(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:$packageName")))
                            }
                            Text("El acceso de uso solo se consulta en este dispositivo para reconocer el inicio. No se envían datos.", color = Muted, fontSize = 12.sp)
                            if (!homeAvailable) {
                                Text("Selecciona la pantalla de inicio original de tu teléfono para continuar.", color = Accent, fontSize = 13.sp)
                                TextButton(onClick = { openSetting(Intent(Settings.ACTION_HOME_SETTINGS)) }) { Text("Elegir inicio del teléfono") }
                            }
                            if (HomeNotesService.running) {
                                Button(onClick = { stopService(Intent(this@MainActivity, HomeNotesService::class.java)) }) { Text("Detener pósits") }
                            } else Button(enabled = enabled, onClick = {
                                if (Build.VERSION.SDK_INT >= 33) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                                startForegroundService(Intent(this@MainActivity, HomeNotesService::class.java).setAction(HomeNotesService.START))
                            }) { Text("Activar en mi inicio") }
                            if (store.notes.none { it.pinned }) Text("Crea o pega al inicio al menos una nota para activar.", color = Muted, fontSize = 12.sp)
                            if (HomeNotesService.running) TextButton(onClick = {
                                startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }) { Text("Ir a mi pantalla de inicio") }
                        }
                    }
                }
                item { Text("Tus notas", fontFamily = FontFamily.Serif, fontSize = 25.sp, fontWeight = FontWeight.Bold) }
                if (store.notes.isEmpty()) item { Text("Pulsa + Nuevo pósit para escribir tu primer recordatorio.", color = Muted) }
                items(store.notes, key = { it.id }) { note ->
                    PaperSurface(note.color) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Column(Modifier.fillMaxWidth().clickable { editingId = note.id; editorOpen = true }) {
                                Text(if (note.pinned) "PEGADO AL INICIO" else "GUARDADO", color = Muted, fontSize = 10.sp)
                                Text(note.title.ifBlank { "Mi nota" }, fontFamily = FontFamily.Serif, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                                Text(note.body, maxLines = 4, overflow = TextOverflow.Ellipsis)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                TextButton(onClick = {
                                    store.update(note.id, bringToFront = !note.pinned) { it.copy(pinned = !it.pinned) }; sync()
                                }) { Text(if (note.pinned) "Retirar del inicio" else "Pegar al inicio", color = Ink) }
                                TextButton(onClick = { deleteId = note.id }) { Text("Eliminar", color = Ink) }
                            }
                        }
                    }
                }
            }
        }
        if (editorOpen) NoteEditor(store.notes.firstOrNull { it.id == editingId }, onDismiss = { editorOpen = false }) {
            store.save(it, bringToFront = it.pinned); editorOpen = false; sync()
        }
        deleteId?.let { id -> AlertDialog(onDismissRequest = { deleteId = null },
            title = { Text("¿Eliminar este pósit?") }, text = { Text("Se borrará de tus notas y del inicio.") },
            confirmButton = { TextButton(onClick = { store.delete(id); deleteId = null; sync() }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Cancelar") } }) }
    }
}

@Composable
private fun PermissionRow(label: String, allowed: Boolean, onRequest: () -> Unit) {
    TextButton(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
        Text(if (allowed) "\u2713 $label" else "$label -> Permitir")
    }
}
