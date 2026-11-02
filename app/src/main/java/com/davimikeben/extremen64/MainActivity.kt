package com.davimikeben.extremen64

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

private val BG_TOP = Color(0xFF0A0C1A)
private val BG_BOTTOM = Color(0xFF141A33)
private val ACCENT = Color(0xFFE91E63)

class MainActivity : ComponentActivity() {
    private var emulatorView: EmulatorView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SettingsManager.init(this)
        setContent { ExtremeApp(onViewReady = { emulatorView = it }) }
    }

    override fun onPause() {
        super.onPause()
        emulatorView?.pauseEmulation()
    }

    override fun onResume() {
        super.onResume()
        emulatorView?.resumeEmulation()
    }
}

@Composable
fun ExtremeApp(onViewReady: (EmulatorView) -> Unit) {
    var screen by remember { mutableStateOf(Screen.MENU) }
    var editMode by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = BG_TOP) {
        when (screen) {
            Screen.MENU -> ExtremeMenu(
                onPlay = { screen = Screen.GAME },
                onExit = { }
            )
            Screen.GAME -> GameScreen(
                onViewReady = onViewReady,
                editMode = editMode,
                onEditModeChange = { editMode = it },
                onBackToMenu = { screen = Screen.MENU }
            )
        }
    }
}

enum class Screen { MENU, GAME }

// ===========================================================================
// MAIN MENU
// ===========================================================================
@Composable
fun ExtremeMenu(onPlay: () -> Unit, onExit: () -> Unit) {
    var dialog by remember { mutableStateOf<MenuDialog?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BG_TOP, BG_BOTTOM)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "EXTREME N64",
                color = ACCENT,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black
            )
            Text("por davimikeben  •  ROM integrada  •  núcleo interno", color = Color.Gray, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "Super Mario 64 (USA).z64  •  8,0 MB  •  sha256 17ce0773…1d91",
                color = Color(0xFF5C6BC0),
                fontSize = 10.sp
            )
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MenuColumn(
                    MenuItem("▶️", "PLAY", "Iniciar o jogo", ACCENT) { onPlay() },
                    MenuItem("🎮", "CONTROLES", "Teclado virtual e perfis") { dialog = MenuDialog.CONTROLS },
                    MenuItem("⚡", "DESEMPENHO", "Modos para Mali-G52") { dialog = MenuDialog.PERFORMANCE }
                )
                MenuColumn(
                    MenuItem("🎨", "GRÁFICOS", "Filtro e resolução") { dialog = MenuDialog.GRAPHICS },
                    MenuItem("🔊", "ÁUDIO", "Ligar/desligar saída") { dialog = MenuDialog.AUDIO },
                    MenuItem("📱", "TELA", "Formato e orientação") { dialog = MenuDialog.SCREEN }
                )
                MenuColumn(
                    MenuItem("📊", "MONITORAMENTO", "FPS, RAM e renderização") { dialog = MenuDialog.MONITORING },
                    MenuItem("💾", "SALVAR", "Gravar configurações") {
                        SettingsManager.saveSnapshot(); dialog = MenuDialog.SAVE
                    },
                    MenuItem("🔄", "RESTAURAR", "Voltar aos padrões") {
                        SettingsManager.restoreDefaults(); dialog = MenuDialog.RESTORE
                    }
                )
            }
        }

        when (dialog) {
            MenuDialog.PERFORMANCE -> PerformanceDialog(onClose = { dialog = null })
            MenuDialog.CONTROLS -> ControlsConfigDialog(onClose = { dialog = null })
            MenuDialog.GRAPHICS -> GraphicsDialog(onClose = { dialog = null })
            MenuDialog.AUDIO -> AudioDialog(onClose = { dialog = null })
            MenuDialog.SCREEN -> ScreenDialog(onClose = { dialog = null })
            MenuDialog.MONITORING -> MonitoringDialog(onClose = { dialog = null })
            MenuDialog.SAVE -> InfoDialog(
                title = "CONFIGURAÇÕES SALVAS",
                body = SettingsManager.snapshotSummary(),
                onClose = { dialog = null }
            )
            MenuDialog.RESTORE -> InfoDialog(
                title = "CONFIGURAÇÕES RESTAURADAS",
                body = "Todos os valores voltaram ao padrão de fábrica do aplicativo.",
                onClose = { dialog = null }
            )
            null -> Unit
        }
    }
}

enum class MenuDialog { PERFORMANCE, CONTROLS, GRAPHICS, AUDIO, SCREEN, MONITORING, SAVE, RESTORE }

@Composable
private fun MenuColumn(vararg items: MenuItem) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        items.forEach { ExtremeMenuButton(it) }
    }
}

data class MenuItem(
    val icon: String,
    val label: String,
    val description: String,
    val highlight: Color = Color(0xFF1E2238),
    val onClick: () -> Unit
)

@Composable
private fun ExtremeMenuButton(item: MenuItem) {
    Button(
        onClick = item.onClick,
        modifier = Modifier
            .width(210.dp)
            .padding(vertical = 5.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = item.highlight),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(item.icon, fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(item.label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(item.description, color = Color(0xFFB0B6D0), fontSize = 10.sp)
            }
        }
    }
}

// ===========================================================================
// IN-GAME SCREEN
// ===========================================================================
@Composable
fun GameScreen(
    onViewReady: (EmulatorView) -> Unit,
    editMode: Boolean,
    onEditModeChange: (Boolean) -> Unit,
    onBackToMenu: () -> Unit
) {
    var view by remember { mutableStateOf<EmulatorView?>(null) }
    var fps by remember { mutableStateOf(0f) }
    var status by remember { mutableStateOf("Preparando...") }
    var error by remember { mutableStateOf<String?>(null) }
    var hardware by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(view) {
        while (true) {
            view?.let {
                fps = it.fps
                status = it.status
                error = it.error
                hardware = it.hardwareRendering
            }
            delay(300)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                EmulatorView(ctx).also {
                    view = it
                    onViewReady(it)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        VirtualControls(editMode = editMode)

        // Discrete FPS counter - the only HUD shown while playing
        if (SettingsManager.showFps) {
            Text(
                text = "FPS ${fps.toInt()}",
                color = Color(0xFF00E676),
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 6.dp)
            )
        }

        // Small floating button to open/close the in-game menu
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 44.dp, end = 12.dp)
                .size(34.dp)
                .background(Color(0x661E2238), RoundedCornerShape(50))
                .clickableNoIndication { showMenu = !showMenu },
            contentAlignment = Alignment.Center
        ) {
            Text(if (showMenu) "✕" else "⚙️", fontSize = 15.sp)
        }

        if (error != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color(0xCC000000), RoundedCornerShape(12.dp))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠️ $error", color = Color(0xFFFF5252), fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(status, color = Color.Gray, fontSize = 11.sp)
                }
            }
        } else if (fps == 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color(0xCC000000), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(status, color = Color.White, fontSize = 13.sp)
            }
        }

        if (showMenu) {
            FloatingMenu(
                editMode = editMode,
                onEditModeChange = onEditModeChange,
                fps = fps,
                hardware = hardware,
                onReset = { view?.resetEmulation() },
                onBackToMenu = {
                    showMenu = false
                    view?.shutdownEmulation()
                    onBackToMenu()
                },
                onClose = { showMenu = false }
            )
        }
    }
}

private fun Modifier.clickableNoIndication(onClick: () -> Unit): Modifier =
    clickable(
        indication = null,
        interactionSource = MutableInteractionSource(),
        onClick = onClick
    )
