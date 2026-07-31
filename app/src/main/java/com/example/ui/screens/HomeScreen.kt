package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CrosshairConfig
import com.example.model.CrosshairPreset
import com.example.model.CrosshairStyle
import com.example.ui.MainViewModel
import com.example.ui.components.CrosshairCanvas

private data class ColorOption(val hex: Long, val name: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val config by viewModel.config.collectAsState()
    val isRunning by viewModel.isServiceRunning.collectAsState()
    val hasPermission by viewModel.hasOverlayPermission.collectAsState()
    val presets by viewModel.presets.collectAsState()

    var selectedMainTab by remember { mutableStateOf(0) } // 0: Formas, 1: Color, 2: Position/Size, 3: Presets

    var showSavePresetDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }
    var newPresetGame by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF00FF66)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Crosshair Hub",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Ekranda Xüsusi Nişangah",
                                color = Color(0xFF00FF66),
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                actions = {
                    // Service toggle button
                    FilledTonalButton(
                        onClick = {
                            if (!hasPermission) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } else {
                                viewModel.toggleOverlayService()
                            }
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isRunning) Color(0xFFFF0033) else Color(0xFF00FF66),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.PowerSettingsNew else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isRunning) "Dayandır" else "Başlat",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF090D14)
                )
            )
        },
        containerColor = Color(0xFF090D14)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Permission Card if needed
            if (!hasPermission) {
                item {
                    PermissionWarningCard(
                        onGrantPermission = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // 2. Status Banner
            item {
                StatusBanner(
                    isRunning = isRunning,
                    onToggle = {
                        if (!hasPermission) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } else {
                            viewModel.toggleOverlayService()
                        }
                    }
                )
            }

            // 4. Floating Square Feature Instruction Banner
            item {
                FloatingSquareGuideBanner(
                    showSquare = config.showFloatingSquare,
                    onToggleSquare = {
                        viewModel.updateConfig(config.copy(showFloatingSquare = it))
                    }
                )
            }

            // 5. Main Control Tab Row
            item {
                MainTabRow(
                    selectedTab = selectedMainTab,
                    onTabSelected = { selectedMainTab = it }
                )
            }

            // 6. Tab Content
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF111823)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF1E2D42))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        when (selectedMainTab) {
                            0 -> ReticleStylesTab(
                                currentConfig = config,
                                onSelectStyle = { style ->
                                    viewModel.updateConfig(config.copy(style = style))
                                }
                            )

                            1 -> ColorAndOpacityTab(
                                config = config,
                                onConfigChange = { updated -> viewModel.updateConfig(updated) }
                            )

                            2 -> PositionAndSizeTab(
                                config = config,
                                onConfigChange = { updated -> viewModel.updateConfig(updated) }
                            )

                            3 -> PresetsTab(
                                presets = presets,
                                currentConfig = config,
                                onApplyPreset = { preset -> viewModel.applyPreset(preset) },
                                onSaveNewPreset = { showSavePresetDialog = true },
                                onDeletePreset = { id -> viewModel.deleteCustomPreset(id) }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Save Preset Dialog
    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = {
                Text("Cari Nişangahı Yadda Saxla", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Öz nişangah dizaynınızı adlandırın:", color = Color.LightGray, fontSize = 13.sp)
                    OutlinedTextField(
                        value = newPresetName,
                        onValueChange = { newPresetName = it },
                        label = { Text("Nişangah Adı (məs: Pro Red Dot)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00FF66),
                            focusedLabelColor = Color(0xFF00FF66)
                        )
                    )
                    OutlinedTextField(
                        value = newPresetGame,
                        onValueChange = { newPresetGame = it },
                        label = { Text("Oyun Adı (məs: PUBG Mobile)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00FF66),
                            focusedLabelColor = Color(0xFF00FF66)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPresetName.isNotBlank()) {
                            viewModel.saveCustomPreset(newPresetName, newPresetGame)
                            newPresetName = ""
                            newPresetGame = ""
                            showSavePresetDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66), contentColor = Color.Black)
                ) {
                    Text("Yadda Saxla", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) {
                    Text("Ləğv et", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF111823)
        )
    }
}

@Composable
private fun PermissionWarningCard(onGrantPermission: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF331400)),
        border = BorderStroke(1.5.dp, Color(0xFFFF6D00))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFF6D00),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ekran İcazəsi Tələb Olunur",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Nişangahı digər oyunların üzərində göstərmək üçün 'Display over other apps' icazəsi verilməlidir.",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onGrantPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6D00)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("İcazə Ver", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun StatusBanner(isRunning: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning) Color(0xFF002B15) else Color(0xFF161F2C)
        ),
        border = BorderStroke(1.5.dp, if (isRunning) Color(0xFF00FF66) else Color(0xFF26374D))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (isRunning) Color(0xFF00FF66) else Color.Red)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isRunning) "CROSSHAİR AKTİVDİR" else "CROSSHAİR DEAKTİVDİR",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (isRunning) "Overlay ekranda görünür" else "Overlayı işə salmaq üçün toxunun",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            Switch(
                checked = isRunning,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF00FF66),
                    checkedTrackColor = Color(0xFF004D25)
                )
            )
        }
    }
}

@Composable
private fun FloatingSquareGuideBanner(
    showSquare: Boolean,
    onToggleSquare: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141F2D)),
        border = BorderStroke(1.dp, Color(0xFF00FF66).copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF00FF66)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CropSquare, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Dörd Bucaq Balaca Düymə",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Oyun üzərində tez sazlama üçün",
                            color = Color(0xFF00FF66),
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = showSquare,
                    onCheckedChange = onToggleSquare,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF00FF66),
                        checkedTrackColor = Color(0xFF004D25)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "💡 Ekranda göstərilən balaca kvadrat düyməyə basdıqda 3 bölməli (Rəng, Pozisiya və Crosshairlər) cəld sazlama pəncərəsi açılır!",
                color = Color.LightGray,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun MainTabRow(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color(0xFF0F1722),
        contentColor = Color(0xFF00FF66),
        edgePadding = 0.dp,
        indicator = { tabPositions ->
            if (selectedTab < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Color(0xFF00FF66)
                )
            }
        }
    ) {
        Tab(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            text = { Text("Formalar", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal, color = if (selectedTab == 0) Color(0xFF00FF66) else Color.Gray) }
        )
        Tab(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            text = { Text("Rəng & Haşiyə", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal, color = if (selectedTab == 1) Color(0xFF00FF66) else Color.Gray) }
        )
        Tab(
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            text = { Text("Pozisiya & Ölçü", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal, color = if (selectedTab == 2) Color(0xFF00FF66) else Color.Gray) }
        )
        Tab(
            selected = selectedTab == 3,
            onClick = { onTabSelected(3) },
            text = { Text("Hazır Modellər", fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal, color = if (selectedTab == 3) Color(0xFF00FF66) else Color.Gray) }
        )
    }
}

// ---------------------------------------------------------
// TAB 1: FORMALAR
// ---------------------------------------------------------
@Composable
private fun ReticleStylesTab(
    currentConfig: CrosshairConfig,
    onSelectStyle: (CrosshairStyle) -> Unit
) {
    Column {
        Text("Nişangah Formasını Seçin:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text("Oyun stilinizə uyğun 12 müxtəlif peşəkar nişangah forması", color = Color.Gray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.height(300.dp)
        ) {
            items(CrosshairStyle.entries) { style ->
                val isSelected = currentConfig.style == style
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(95.dp)
                        .clickable { onSelectStyle(style) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF00381B) else Color(0xFF1A2636)
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Color(0xFF00FF66) else Color(0xFF2B3D54)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CrosshairCanvas(
                            config = currentConfig.copy(
                                style = style,
                                sizeDp = 28f,
                                strokeWidthDp = 2.5f,
                                gapDp = 5f,
                                offsetX = 0,
                                offsetY = 0
                            ),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = style.displayName,
                            color = if (isSelected) Color(0xFF00FF66) else Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------
// TAB 2: RƏNG VƏ HAŞİYƏ
// ---------------------------------------------------------
@Composable
private fun ColorAndOpacityTab(
    config: CrosshairConfig,
    onConfigChange: (CrosshairConfig) -> Unit
) {
    val colorsList = listOf(
        ColorOption(0xFF00FF66L, "Neon Yaşıl"),
        ColorOption(0xFFFF0033L, "Qırmızı"),
        ColorOption(0xFF00E5FFL, "Neon Göy"),
        ColorOption(0xFFFFCC00L, "Neon Sarı"),
        ColorOption(0xFFFF00FFL, "Çəhrayı"),
        ColorOption(0xFF9D00FFL, "Bənövşəyi"),
        ColorOption(0xFFFF6D00L, "Narinci"),
        ColorOption(0xFFFFFFFFL, "Ağ"),
        ColorOption(0xFF000000L, "Qara"),
        ColorOption(0xFF00FFCCL, "Firuzəyi")
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Rəng Palitrası:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.height(110.dp)
        ) {
            items(colorsList) { item ->
                val hex = item.hex
                val isSelected = config.color == hex
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(hex))
                        .border(
                            width = if (isSelected) 3.5.dp else 1.dp,
                            color = if (isSelected) Color.White else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onConfigChange(config.copy(color = hex)) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = if (hex == 0xFFFFFFFFL) Color.Black else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        Divider(color = Color(0xFF26374D))

        // Opacity
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Şəffaflıq / Opacity:", color = Color.White, fontSize = 14.sp)
            Text("${(config.opacity * 100).toInt()}%", color = Color(0xFF00FF66), fontWeight = FontWeight.Bold)
        }
        Slider(
            value = config.opacity,
            onValueChange = { onConfigChange(config.copy(opacity = it)) },
            valueRange = 0.2f..1.0f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF00FF66),
                activeTrackColor = Color(0xFF00FF66)
            )
        )

        // Outline toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2838)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Qara Kontur / Outline", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Parlaq xəritələrdə nişangahın itməməsi üçün qara haşiyə əlavə edir", color = Color.Gray, fontSize = 11.sp)
                }
                Switch(
                    checked = config.hasOutline,
                    onCheckedChange = { onConfigChange(config.copy(hasOutline = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF00FF66),
                        checkedTrackColor = Color(0xFF004D25)
                    )
                )
            }
        }
    }
}

// ---------------------------------------------------------
// TAB 3: POZİSİYA VƏ ÖLÇÜ
// ---------------------------------------------------------
@Composable
private fun PositionAndSizeTab(
    config: CrosshairConfig,
    onConfigChange: (CrosshairConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Pozisiya Düzəlişi (X / Y Offset):", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)

        // Directional controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { onConfigChange(config.copy(offsetY = config.offsetY - 2)) },
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xFF223246), RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Yuxarı", tint = Color(0xFF00FF66))
                }

                Row {
                    IconButton(
                        onClick = { onConfigChange(config.copy(offsetX = config.offsetX - 2)) },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF223246), RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Sola", tint = Color(0xFF00FF66))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { onConfigChange(config.copy(offsetX = 0, offsetY = 0)) },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF00FF66), RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "Mərkəz", tint = Color.Black)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { onConfigChange(config.copy(offsetX = config.offsetX + 2)) },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF223246), RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Sağa", tint = Color(0xFF00FF66))
                    }
                }

                IconButton(
                    onClick = { onConfigChange(config.copy(offsetY = config.offsetY + 2)) },
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xFF223246), RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Aşağı", tint = Color(0xFF00FF66))
                }
            }

            Column {
                Text("X Shift: ${config.offsetX} px", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Y Shift: ${config.offsetY} px", color = Color.White, fontWeight = FontWeight.Bold)
                Button(
                    onClick = { onConfigChange(config.copy(offsetX = 0, offsetY = 0)) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF223246)),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Mərkəzə Sıfırla", color = Color(0xFF00FF66), fontSize = 12.sp)
                }
            }
        }

        Divider(color = Color(0xFF26374D))

        // Size / Böyüdüb Kiçiltmək
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Ölçü (Böyüdüb / Kiçiltmək):", color = Color.White, fontSize = 14.sp)
            Text("${config.sizeDp.toInt()} dp", color = Color(0xFF00FF66), fontWeight = FontWeight.Bold)
        }
        Slider(
            value = config.sizeDp,
            onValueChange = { onConfigChange(config.copy(sizeDp = it)) },
            valueRange = 12f..100f,
            colors = SliderDefaults.colors(thumbColor = Color(0xFF00FF66), activeTrackColor = Color(0xFF00FF66))
        )

        // Stroke width
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Xətt Qalınlığı:", color = Color.White, fontSize = 14.sp)
            Text("${config.strokeWidthDp.toInt()} dp", color = Color(0xFF00FF66), fontWeight = FontWeight.Bold)
        }
        Slider(
            value = config.strokeWidthDp,
            onValueChange = { onConfigChange(config.copy(strokeWidthDp = it)) },
            valueRange = 1f..12f,
            colors = SliderDefaults.colors(thumbColor = Color(0xFF00FF66), activeTrackColor = Color(0xFF00FF66))
        )

        // Gap
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Mərkəzi Boşluq (Gap):", color = Color.White, fontSize = 14.sp)
            Text("${config.gapDp.toInt()} dp", color = Color(0xFF00FF66), fontWeight = FontWeight.Bold)
        }
        Slider(
            value = config.gapDp,
            onValueChange = { onConfigChange(config.copy(gapDp = it)) },
            valueRange = 0f..24f,
            colors = SliderDefaults.colors(thumbColor = Color(0xFF00FF66), activeTrackColor = Color(0xFF00FF66))
        )
    }
}

// ---------------------------------------------------------
// TAB 4: HAZIR MODELLƏR VƏ PRESETS
// ---------------------------------------------------------
@Composable
private fun PresetsTab(
    presets: List<CrosshairPreset>,
    currentConfig: CrosshairConfig,
    onApplyPreset: (CrosshairPreset) -> Unit,
    onSaveNewPreset: () -> Unit,
    onDeletePreset: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Hazır Nişangah Modelləri:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Popular oyunlar üçün xüsusi seçilmiş retikullar", color = Color.Gray, fontSize = 11.sp)
            }

            Button(
                onClick = onSaveNewPreset,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66), contentColor = Color.Black),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Yeni Yadda Saxla", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.height(280.dp)
        ) {
            items(presets) { preset ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clickable { onApplyPreset(preset) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2838)),
                    border = BorderStroke(1.dp, Color(0xFF283B52))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CrosshairCanvas(
                            config = preset.config.copy(sizeDp = 24f, offsetX = 0, offsetY = 0),
                            modifier = Modifier.size(46.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = preset.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                            Text(
                                text = preset.gameName,
                                color = Color(0xFF00FF66),
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tətbiq Et ➔",
                                color = Color.LightGray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (preset.id.startsWith("custom_")) {
                            IconButton(
                                onClick = { onDeletePreset(preset.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color.Red, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
