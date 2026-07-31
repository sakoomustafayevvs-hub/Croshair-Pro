package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.data.CrosshairPreferences
import com.example.model.CrosshairConfig
import com.example.model.CrosshairStyle
import com.example.ui.components.CrosshairCanvas
import kotlin.math.abs

class CrosshairOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var prefs: CrosshairPreferences

    private var crosshairView: ComposeView? = null
    private var squareWidgetView: ComposeView? = null
    private var popupMenuView: ComposeView? = null

    private var crosshairParams: WindowManager.LayoutParams? = null
    private var squareWidgetParams: WindowManager.LayoutParams? = null
    private var popupMenuParams: WindowManager.LayoutParams? = null

    private var currentConfigState = mutableStateOf(CrosshairConfig())
    private var isPopupExpanded = mutableStateOf(false)

    private val lifecycleOwner = OverlayLifecycleOwner()

    companion object {
        const val CHANNEL_ID = "crosshair_overlay_channel"
        const val NOTIF_ID = 1001
        const val ACTION_STOP = "com.example.ACTION_STOP_SERVICE"
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        prefs = CrosshairPreferences(this)
        currentConfigState.value = prefs.getConfig()

        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_RESUME)

        createNotificationChannel()
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(
                    NOTIF_ID,
                    createNotification(),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIF_ID,
                    createNotification(),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE
                )
            } else {
                startForeground(NOTIF_ID, createNotification())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (android.provider.Settings.canDrawOverlays(this)) {
            setupCrosshairOverlay()
            setupFloatingSquareWidget()
            setupPopupMenuOverlay()
        } else {
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        // Refresh config in case it changed from main activity
        currentConfigState.value = prefs.getConfig()
        updateCrosshairLayout()
        return START_STICKY
    }

    private fun setupCrosshairOverlay() {
        crosshairParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        crosshairView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                val config = currentConfigState.value
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CrosshairCanvas(config = config)
                }
            }
        }

        try {
            windowManager.addView(crosshairView, crosshairParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var popupX = 40
    private var popupY = 160

    private fun setupFloatingSquareWidget() {
        val config = currentConfigState.value
        squareWidgetParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = config.squareX
            y = config.squareY
        }

        squareWidgetView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                val cfg = currentConfigState.value
                if (cfg.showFloatingSquare) {
                    var isClick by remember { mutableStateOf(true) }

                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .shadow(8.dp, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF161B22))
                            .border(1.5.dp, Color(cfg.color), RoundedCornerShape(12.dp))
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    var dragAmountTotal = 0f
                                    var slopExceeded = false
                                    val pointerId = down.id

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break

                                        if (!change.pressed) {
                                            if (!slopExceeded) {
                                                isPopupExpanded.value = !isPopupExpanded.value
                                            } else {
                                                squareWidgetParams?.let { params ->
                                                    currentConfigState.value = currentConfigState.value.copy(
                                                        squareX = params.x,
                                                        squareY = params.y
                                                    )
                                                    prefs.saveConfig(currentConfigState.value)
                                                }
                                            }
                                            break
                                        } else {
                                            val dragDelta = change.positionChange()
                                            val dx = dragDelta.x
                                            val dy = dragDelta.y
                                            if (abs(dx) > 0.5f || abs(dy) > 0.5f) {
                                                dragAmountTotal += abs(dx) + abs(dy)
                                                if (dragAmountTotal > 12f) {
                                                    slopExceeded = true
                                                }
                                                if (slopExceeded) {
                                                    change.consume()
                                                    squareWidgetParams?.let { params ->
                                                        params.x = (params.x + dx.toInt()).coerceAtLeast(0)
                                                        params.y = (params.y + dy.toInt()).coerceAtLeast(0)
                                                        try {
                                                            windowManager.updateViewLayout(squareWidgetView, params)
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = "Crosshair Menu",
                                tint = Color(cfg.color),
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "MENU",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        try {
            windowManager.addView(squareWidgetView, squareWidgetParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupPopupMenuOverlay() {
        popupMenuParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = popupX
            y = popupY
        }

        popupMenuView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                val expanded by isPopupExpanded
                val config = currentConfigState.value

                if (expanded) {
                    Box(
                        modifier = Modifier.padding(4.dp)
                    ) {
                        OverlayQuickControlDialog(
                            config = config,
                            onConfigChange = { updated ->
                                currentConfigState.value = updated
                                prefs.saveConfig(updated)
                            },
                            onClose = {
                                isPopupExpanded.value = false
                            },
                            onDrag = { dx, dy ->
                                popupMenuParams?.let { params ->
                                    params.x = (params.x + dx.toInt()).coerceAtLeast(0)
                                    params.y = (params.y + dy.toInt()).coerceAtLeast(0)
                                    popupX = params.x
                                    popupY = params.y
                                    try {
                                        windowManager.updateViewLayout(popupMenuView, params)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        try {
            windowManager.addView(popupMenuView, popupMenuParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateCrosshairLayout() {
        try {
            crosshairView?.invalidate()
            squareWidgetView?.invalidate()
            popupMenuView?.invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Crosshair Overlay Servisi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Crosshair overlay aktiv olduqda bildiriş göstərir"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, CrosshairOverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Crosshair Hub Aktivdir")
            .setContentText("Ekranda xüsusi nişangah overlay göstərilir.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingOpen)
            .addAction(R.drawable.ic_launcher_foreground, "Dayandır", pendingStop)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleOwner.destroy()
        try {
            crosshairView?.let { windowManager.removeView(it) }
            squareWidgetView?.let { windowManager.removeView(it) }
            popupMenuView?.let { windowManager.removeView(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

// =========================================================
// 3 BÖLMƏLİ BALACA SƏHİFƏ (OVERLAY QUICK CONTROL DIALOG)
// =========================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlayQuickControlDialog(
    config: CrosshairConfig,
    onConfigChange: (CrosshairConfig) -> Unit,
    onClose: () -> Unit,
    onDrag: (Float, Float) -> Unit = { _, _ -> }
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Rəng, 1: Pozisiya & Ölçü, 2: Croshairlər

    Card(
        modifier = Modifier
            .widthIn(max = 330.dp)
            .shadow(16.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F1722)
        ),
        border = CardBorder(1.5.dp, Color(0xFF00FF66))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header Bar (Draggable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF182433))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.x, dragAmount.y)
                        }
                    }
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.OpenWith,
                    contentDescription = "Tərpət",
                    tint = Color(0xFF00FF66),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Tərpət",
                    color = Color(0xFF8A99AD),
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3 Bölmə Tab Bar (Compact)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF162232),
                contentColor = Color(0xFF00FF66),
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
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "1. Rəng",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) Color(0xFF00FF66) else Color.LightGray
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "2. Ölçü",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) Color(0xFF00FF66) else Color.LightGray
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            text = "3. Formalar",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 2) Color(0xFF00FF66) else Color.LightGray
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tab Content
            when (selectedTab) {
                0 -> SectionColor(config = config, onConfigChange = onConfigChange)
                1 -> SectionPositionAndSize(config = config, onConfigChange = onConfigChange)
                2 -> SectionCrosshairsGrid(config = config, onConfigChange = onConfigChange)
            }
        }
    }
}

private fun CardBorder(width: androidx.compose.ui.unit.Dp, color: Color) = BorderStroke(width, color)

data class ColorOption(val hex: Long, val name: String)

// ---------------------------------------------------------
// BÖLMƏ 1: CROSHAİR RƏNGİ VƏ PARLAQLIQ
// ---------------------------------------------------------
@Composable
private fun SectionColor(
    config: CrosshairConfig,
    onConfigChange: (CrosshairConfig) -> Unit
) {
    val colorPresets = listOf(
        ColorOption(0xFF00FF66L, "Neon Yaşıl"),
        ColorOption(0xFFFF0033L, "Qırmızı"),
        ColorOption(0xFF00E5FFL, "Neon Göy"),
        ColorOption(0xFFFFCC00L, "Sarı"),
        ColorOption(0xFFFF00FFL, "Çəhrayı"),
        ColorOption(0xFF9D00FFL, "Bənövşəyi"),
        ColorOption(0xFFFF6D00L, "Narinci"),
        ColorOption(0xFFFFFFFFL, "Ağ"),
        ColorOption(0xFF000000L, "Qara"),
        ColorOption(0xFF00FFCCL, "Firuzəyi")
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Rəng Seçimi:",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(100.dp)
        ) {
            items(colorPresets) { item ->
                val hex = item.hex
                val isSelected = config.color == hex
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(hex))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) Color.White else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable {
                            onConfigChange(config.copy(color = hex))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = if (hex == 0xFFFFFFFFL) Color.Black else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Opacity Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Şəffaflıq / Opacity:", color = Color.LightGray, fontSize = 13.sp)
            Text("${(config.opacity * 100).toInt()}%", color = Color(0xFF00FF66), fontWeight = FontWeight.Bold, fontSize = 13.sp)
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

        // Outline Border Switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1A2636))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Qara Haşiyə / Outline", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("Hər fonda aydın görünmə üçün", color = Color.Gray, fontSize = 11.sp)
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

// ---------------------------------------------------------
// BÖLMƏ 2: CROSHAİR-I SAĞA SOLA YUXARI AŞAĞI ETMƏK VƏ BÖYÜDÜB KİÇİTMƏK
// ---------------------------------------------------------
@Composable
private fun SectionPositionAndSize(
    config: CrosshairConfig,
    onConfigChange: (CrosshairConfig) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Pozisiya (Sağa / Sola / Yuxarı / Aşağı):",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        // D-Pad Directional Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Directional cross controller
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Up
                IconButton(
                    onClick = { onConfigChange(config.copy(offsetY = config.offsetY - 2)) },
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Yuxarı", tint = Color(0xFF00FF66))
                }

                Row {
                    // Left
                    IconButton(
                        onClick = { onConfigChange(config.copy(offsetX = config.offsetX - 2)) },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Sola", tint = Color(0xFF00FF66))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Reset center
                    IconButton(
                        onClick = { onConfigChange(config.copy(offsetX = 0, offsetY = 0)) },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF00FF66), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "Mərkəz", tint = Color.Black)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Right
                    IconButton(
                        onClick = { onConfigChange(config.copy(offsetX = config.offsetX + 2)) },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Sağa", tint = Color(0xFF00FF66))
                    }
                }

                // Down
                IconButton(
                    onClick = { onConfigChange(config.copy(offsetY = config.offsetY + 2)) },
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Aşağı", tint = Color(0xFF00FF66))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text("X Offset: ${config.offsetX} px", color = Color.LightGray, fontSize = 11.sp)
                Text("Y Offset: ${config.offsetY} px", color = Color.LightGray, fontSize = 11.sp)
                OutlinedButton(
                    onClick = { onConfigChange(config.copy(offsetX = 0, offsetY = 0)) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00FF66)),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text("Mərkəzlə", fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Böyüdüb Kiçiltmək (Size Slider)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ölçü (Böyüdüb Kiçiltmək):", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text("${config.sizeDp.toInt()} dp", color = Color(0xFF00FF66), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Slider(
            value = config.sizeDp,
            onValueChange = { onConfigChange(config.copy(sizeDp = it)) },
            valueRange = 12f..100f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF00FF66),
                activeTrackColor = Color(0xFF00FF66)
            )
        )

        // Xətt Qalınlığı (Stroke Width Slider)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Xətt Qalınlığı:", color = Color.White, fontSize = 12.sp)
            Text("${config.strokeWidthDp.toInt()} dp", color = Color(0xFF00FF66), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Slider(
            value = config.strokeWidthDp,
            onValueChange = { onConfigChange(config.copy(strokeWidthDp = it)) },
            valueRange = 1f..12f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF00FF66),
                activeTrackColor = Color(0xFF00FF66)
            )
        )
    }
}

// ---------------------------------------------------------
// BÖLMƏ 3: CROSHAİRLƏR (FORMALAR VƏ DİZAYNLAR)
// ---------------------------------------------------------
@Composable
private fun SectionCrosshairsGrid(
    config: CrosshairConfig,
    onConfigChange: (CrosshairConfig) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Nişangah Formasını Seçin:",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(210.dp)
        ) {
            items(CrosshairStyle.entries) { style ->
                val isSelected = config.style == style
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clickable {
                            onConfigChange(config.copy(style = style))
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF004D25) else Color(0xFF162232)
                    ),
                    border = CardBorder(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Color(0xFF00FF66) else Color(0xFF23354D)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CrosshairCanvas(
                            config = config.copy(
                                style = style,
                                sizeDp = 24f,
                                strokeWidthDp = 2f,
                                gapDp = 4f,
                                offsetX = 0,
                                offsetY = 0
                            ),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = style.displayName,
                            color = if (isSelected) Color(0xFF00FF66) else Color.White,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
