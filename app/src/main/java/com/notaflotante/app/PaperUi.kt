package com.notaflotante.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

internal val Ink = Color(0xFF3A3024)
internal val Paper = Color(0xFFF5EFE3)
internal val Accent = Color(0xFFA65035)
internal val Muted = Color(0xFF7C7061)

@Composable
internal fun PostitTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Accent, onPrimary = Color.White,
        background = Paper, onBackground = Ink, surface = Paper, onSurface = Ink,
        surfaceVariant = Color(0xFFECE2D0), onSurfaceVariant = Muted), content = content)
}

internal fun Modifier.boardTexture(): Modifier = drawBehind {
    val step = 20.dp.toPx()
    var x = step / 2
    while (x < size.width) {
        var y = step / 2
        while (y < size.height) {
            drawCircle(Color(0x187C7061), .7.dp.toPx(), Offset(x, y))
            y += step
        }
        x += step
    }
}

@Composable
internal fun PaperSurface(color: Int, modifier: Modifier = Modifier, elevation: Dp = 4.dp,
    content: @Composable () -> Unit) {
    val density = LocalDensity.current.density
    val artwork = remember(color, density, elevation) {
        PaperArtwork(NoteColors[color].toInt(), density, shadowDepth = elevation.value)
    }
    Surface(color = Color.Transparent, shape = RectangleShape, shadowElevation = 0.dp,
        modifier = modifier) {
        Box(Modifier.drawBehind {
            drawIntoCanvas { artwork.draw(it.nativeCanvas, size.width, size.height) }
        }) { content() }
    }
}

@Composable
internal fun PaperContent(note: Note, modifier: Modifier = Modifier, onEdit: () -> Unit) {
    Column(modifier.fillMaxWidth().clickable(onClick = onEdit).padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        Text(note.title.ifBlank { "Mi nota" }, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
            fontSize = 23.sp, lineHeight = 26.sp, color = Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(note.body.ifBlank { "Toca para escribir..." }, fontSize = note.fontSize.sp, color = Ink.copy(alpha = .85f),
            modifier = Modifier.weight(1f).padding(top = 8.dp), overflow = TextOverflow.Ellipsis)
        Text("TOCA EL PAPEL PARA EDITAR", fontSize = 9.sp, letterSpacing = .4.sp, color = Ink.copy(alpha = .5f),
            maxLines = 1, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
internal fun NoteEditor(existing: Note?, onDismiss: () -> Unit, onSave: (Note) -> Unit) {
    val key = existing?.id ?: "new"
    var title by rememberSaveable(key) { mutableStateOf(existing?.title ?: "") }
    var body by rememberSaveable(key) { mutableStateOf(existing?.body ?: "") }
    var color by rememberSaveable(key) { mutableIntStateOf(existing?.color ?: 0) }
    var font by rememberSaveable(key) { mutableFloatStateOf(existing?.fontSize?.toFloat() ?: 20f) }
    var wide by rememberSaveable(key) { mutableStateOf(existing?.wide ?: false) }
    val valid = title.isNotBlank() || body.isNotBlank()
    fun save(pinned: Boolean) = onSave((existing ?: Note()).copy(title = title.trim(), body = body.trim(),
        color = color, fontSize = font.toInt(), wide = wide, pinned = pinned))

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(16.dp), color = Paper,
            modifier = Modifier.fillMaxWidth().padding(14.dp).imePadding()) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(if (existing == null) "Una nueva idea" else "Un toque a tu pósit", fontFamily = FontFamily.Serif,
                    fontSize = 28.sp, fontWeight = FontWeight.Bold)
                PaperSurface(color, Modifier.fillMaxWidth(), elevation = 2.dp) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        // The tape is now part of the paper artwork; keep the preview's existing spacing.
                        Spacer(Modifier.height(20.dp))
                        Text(title.ifBlank { "Tu idea aquí" }, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
                            fontSize = 23.sp, color = Ink, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                        Text(body.ifBlank { "Algo que vale la pena recordar..." }, fontSize = font.toInt().sp,
                            color = Ink, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 12.dp))
                    }
                }
                OutlinedTextField(title, onValueChange = { title = it.take(100) }, label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(body, onValueChange = { body = it.take(10000) }, label = { Text("Escribe tu nota") },
                    modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 6)
                Text("ELIGE TU PAPEL", color = Muted, fontSize = 10.sp, letterSpacing = 1.4.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val names = listOf("Amarillo", "Rosa", "Verde", "Azul", "Lila")
                    NoteColors.forEachIndexed { index, value ->
                        Surface(onClick = { color = index }, color = Color(value), shape = CircleShape,
                            modifier = Modifier.size(44.dp).semantics { contentDescription = names[index] }) {
                            Box(contentAlignment = Alignment.Center) {
                                if (color == index) Text("\u2713", color = Ink, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            }
                        }
                    }
                }
                Text("Tamaño del texto . ${font.toInt()} sp", color = Muted, fontSize = 13.sp)
                Slider(font, onValueChange = { font = it }, valueRange = 16f..32f, steps = 15)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Papel ancho", modifier = Modifier.weight(1f))
                    Switch(checked = wide, onCheckedChange = { wide = it })
                }
                Button(onClick = { save(true) }, enabled = valid, modifier = Modifier.fillMaxWidth()) {
                    Text(if (existing?.pinned == true) "Guardar cambios" else "Guardar y pegar al inicio")
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    TextButton(onClick = { save(false) }, enabled = valid) { Text("Guardar en Mis notas") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotesDrawer(notes: List<Note>, onDismiss: () -> Unit, onEdit: (Note) -> Unit,
    onToggle: (Note) -> Unit, onDelete: (Note) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Paper) {
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.85f), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("Mis notas", fontFamily = FontFamily.Serif, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("Guárdalas aquí o pégalas en tu inicio.", color = Muted)
            }
            if (notes.isEmpty()) item { Text("Tu primera idea empieza con + Pósit.") }
            items(notes, key = { it.id }) { note ->
                PaperSurface(note.color) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Column(Modifier.fillMaxWidth().clickable { onEdit(note) }) {
                            Text(note.title.ifBlank { "Mi nota" }, fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold, fontSize = 23.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (note.body.isNotBlank()) Text(note.body, maxLines = 3, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 6.dp))
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = { onToggle(note) }) { Text(if (note.pinned) "Retirar del tablero" else "Pegar al tablero", color = Ink) }
                            TextButton(onClick = { onDelete(note) }) { Text("Eliminar", color = Ink) }
                        }
                    }
                }
            }
        }
    }
}
