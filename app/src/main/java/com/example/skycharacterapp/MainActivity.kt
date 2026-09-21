package com.example.skycharacterapp

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import android.graphics.BitmapFactory
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import java.time.LocalTime

class MainActivity : ComponentActivity() {

    private var selectedImageUri by mutableStateOf<Uri?>(null)

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
            
            selectedImageUri = uri
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences("SkyCharacterApp", Context.MODE_PRIVATE)
        val savedUri = prefs.getString("imageUri", null)
        if (savedUri != null) {
            selectedImageUri = Uri.parse(savedUri)
        }

        setContent {
            MaterialTheme {
                MainScreen(
                    imageUri = selectedImageUri,
                    onSelectImage = {
                        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )
            }
        }
    }
}

@Composable
fun MainScreen(imageUri: Uri?, onSelectImage: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("SkyCharacterApp", Context.MODE_PRIVATE)
    
    var characterSize by remember { 
        mutableFloatStateOf(prefs.getFloat("characterSize", 200f))
    }
    
    var offsetX by remember { mutableFloatStateOf(prefs.getFloat("offsetX", 0f)) }
    var offsetY by remember { mutableFloatStateOf(prefs.getFloat("offsetY", 0f)) }
    
    var skyPalette by remember { mutableStateOf(SkyPalette.loadFromPrefs(prefs)) }
    var showColorSettings by remember { mutableStateOf(false) }

    var isPreviewMode by remember { mutableStateOf(false) }
    var previewTime by remember { mutableStateOf(LocalTime.now()) }
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    var isPreviewAnimationRunning by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(60000)
        }
    }

    LaunchedEffect(isPreviewAnimationRunning) {
        if (isPreviewAnimationRunning) {
            isPreviewMode = true
            val startTime = System.currentTimeMillis()
            val duration = 8000L
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed >= duration) {
                    isPreviewAnimationRunning = false
                    isPreviewMode = false
                    break
                }
                val progress = elapsed.toFloat() / duration.toFloat()
                val totalMinutes = (progress * 24 * 60).toInt()
                val hour = (totalMinutes / 60).coerceIn(0, 23)
                val minute = (totalMinutes % 60).coerceIn(0, 59)
                previewTime = LocalTime.of(hour, minute)
                delay(16)
            }
        }
    }

    val displayTime = if (isPreviewMode) previewTime else currentTime
    val backgroundColors = getSkyGradientForTime(displayTime, skyPalette)
    val backgroundBrush = Brush.verticalGradient(backgroundColors)

    if (showColorSettings) {
        ColorSettingsDialog(
            currentPalette = skyPalette,
            onPaletteChanged = { newPalette ->
                skyPalette = newPalette
            },
            onDismiss = { showColorSettings = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        if (imageUri != null) {
            AsyncImage(
                model = imageUri,
                contentDescription = "Character",
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .requiredSize(characterSize.dp)
                    .align(Alignment.Center)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            characterSize = (characterSize * zoom).coerceIn(10f, 3000f)
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    }
            )
        } else {
            Text(
                text = "캐릭터 이미지를 선택해주세요",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Feedback Button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 100.dp, end = 16.dp)
                .background(Color.White.copy(alpha = 0.25f), shape = RoundedCornerShape(20.dp))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)), RoundedCornerShape(20.dp))
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com/forms/d/e/1FAIpQLSecPww8bdv_ZgnHO5yNWkM1VNnKYjoPPsXpUr1pQgcCuI-piw/viewform?usp=header"))
                    context.startActivity(intent)
                }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "버그 신고/기능요청",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Time Preview Controls
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)), RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.25f), shape = RoundedCornerShape(24.dp))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val previewTimes = listOf(
                "현재" to null,
                "0시" to LocalTime.of(0, 0),
                "6시" to LocalTime.of(6, 0),
                "12시" to LocalTime.of(12, 0),
                "18시" to LocalTime.of(18, 0)
            )

            previewTimes.forEach { (label, time) ->
                val isSelected = (!isPreviewAnimationRunning && isPreviewMode && previewTime == time) || 
                                 (!isPreviewAnimationRunning && !isPreviewMode && time == null)
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) Color.White else Color.Transparent, 
                            RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            isPreviewAnimationRunning = false
                            if (time == null) {
                                isPreviewMode = false
                            } else {
                                isPreviewMode = true
                                previewTime = time
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color(0xFF2B3252) else Color.White,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            
            // 24H Animation Button
            Box(
                modifier = Modifier
                    .background(
                        if (isPreviewAnimationRunning) Color(0xFFFFD700) else Color.Transparent, 
                        RoundedCornerShape(20.dp)
                    )
                    .border(
                        BorderStroke(1.dp, if (isPreviewAnimationRunning) Color.Transparent else Color.White.copy(alpha = 0.3f)), 
                        RoundedCornerShape(20.dp)
                    )
                    .clickable { isPreviewAnimationRunning = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "▷ 24H",
                    color = if (isPreviewAnimationRunning) Color(0xFF2B3252) else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        // Bottom Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val configuration = LocalConfiguration.current
            val screenWidthDp = configuration.screenWidthDp.toFloat()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSelectImage,
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(0.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text("이미지 변경", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                
                Button(
                    onClick = { showColorSettings = true },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(0.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text("배경색 편집", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        if (imageUri != null) {
                            try {
                                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                context.contentResolver.openInputStream(imageUri)?.use {
                                    BitmapFactory.decodeStream(it, null, options)
                                }
                                val width = options.outWidth.toFloat()
                                val height = options.outHeight.toFloat()
                                if (width > 0 && height > 0) {
                                    val aspectRatio = width / height
                                    characterSize = if (aspectRatio > 1) {
                                        screenWidthDp
                                    } else {
                                        screenWidthDp / aspectRatio
                                    }
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(0.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text("너비 꽉채우기", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Button(
                onClick = {
                    prefs.edit()
                        .putString("imageUri", imageUri?.toString())
                        .putFloat("offsetX", offsetX)
                        .putFloat("offsetY", offsetY)
                        .putFloat("characterSize", characterSize)
                        .apply()
                    skyPalette.saveToPrefs(prefs)

                    val wallpaperManager = android.app.WallpaperManager.getInstance(context)
                    val wallpaperInfo = wallpaperManager.wallpaperInfo
                    if (wallpaperInfo != null && wallpaperInfo.packageName == context.packageName) {
                        android.widget.Toast.makeText(context, "배경화면이 성공적으로 업데이트되었습니다!", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        val intent = Intent(android.app.WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                            putExtra(
                                android.app.WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                ComponentName(context, SkyWallpaperService::class.java)
                            )
                        }
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 4.dp)
            ) {
                Text("배경화면 적용하기", color = Color(0xFF2B3252), fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            }
        }
    }
}

data class SkyPalette(
    val nightTop: Color = Color(0xFF2B3252),
    val nightBottom: Color = Color(0xFF4A5578),
    val sunriseTop: Color = Color(0xFF8FB8DE),
    val sunriseBottom: Color = Color(0xFFFFCBA4),
    val dayTop: Color = Color(0xFFA4D8EE),
    val dayBottom: Color = Color(0xFFF2FAFF),
    val sunsetTop: Color = Color(0xFFB39DDB),
    val sunsetBottom: Color = Color(0xFFFFB7B2)
) {
    fun saveToPrefs(prefs: android.content.SharedPreferences) {
        prefs.edit()
            .putInt("nightTop", nightTop.toArgb())
            .putInt("nightBottom", nightBottom.toArgb())
            .putInt("sunriseTop", sunriseTop.toArgb())
            .putInt("sunriseBottom", sunriseBottom.toArgb())
            .putInt("dayTop", dayTop.toArgb())
            .putInt("dayBottom", dayBottom.toArgb())
            .putInt("sunsetTop", sunsetTop.toArgb())
            .putInt("sunsetBottom", sunsetBottom.toArgb())
            .apply()
    }

    companion object {
        fun loadFromPrefs(prefs: android.content.SharedPreferences): SkyPalette {
            val def = SkyPalette()
            return SkyPalette(
                nightTop = Color(prefs.getInt("nightTop", def.nightTop.toArgb())),
                nightBottom = Color(prefs.getInt("nightBottom", def.nightBottom.toArgb())),
                sunriseTop = Color(prefs.getInt("sunriseTop", def.sunriseTop.toArgb())),
                sunriseBottom = Color(prefs.getInt("sunriseBottom", def.sunriseBottom.toArgb())),
                dayTop = Color(prefs.getInt("dayTop", def.dayTop.toArgb())),
                dayBottom = Color(prefs.getInt("dayBottom", def.dayBottom.toArgb())),
                sunsetTop = Color(prefs.getInt("sunsetTop", def.sunsetTop.toArgb())),
                sunsetBottom = Color(prefs.getInt("sunsetBottom", def.sunsetBottom.toArgb()))
            )
        }
    }
}

fun getSkyGradientForTime(time: LocalTime, palette: SkyPalette): List<Color> {
    val hour = time.hour
    val minute = time.minute
    val timeInHours = hour + minute / 60.0

    return when {
        // 새벽 -> 일출 (4:00 ~ 7:00)
        timeInHours in 4.0..7.0 -> {
            val fraction = ((timeInHours - 4.0) / 3.0).toFloat()
            listOf(
                lerpColor(palette.nightTop, palette.sunriseTop, fraction),
                lerpColor(palette.nightBottom, palette.sunriseBottom, fraction)
            )
        }
        // 일출 -> 낮 (7:00 ~ 10:00)
        timeInHours in 7.0..10.0 -> {
            val fraction = ((timeInHours - 7.0) / 3.0).toFloat()
            listOf(
                lerpColor(palette.sunriseTop, palette.dayTop, fraction),
                lerpColor(palette.sunriseBottom, palette.dayBottom, fraction)
            )
        }
        // 낮 (10:00 ~ 16:00)
        timeInHours in 10.0..16.0 -> {
            listOf(palette.dayTop, palette.dayBottom)
        }
        // 낮 -> 일몰 (16:00 ~ 19:00)
        timeInHours in 16.0..19.0 -> {
            val fraction = ((timeInHours - 16.0) / 3.0).toFloat()
            listOf(
                lerpColor(palette.dayTop, palette.sunsetTop, fraction),
                lerpColor(palette.dayBottom, palette.sunsetBottom, fraction)
            )
        }
        // 일몰 -> 밤 (19:00 ~ 22:00)
        timeInHours in 19.0..22.0 -> {
            val fraction = ((timeInHours - 19.0) / 3.0).toFloat()
            listOf(
                lerpColor(palette.sunsetTop, palette.nightTop, fraction),
                lerpColor(palette.sunsetBottom, palette.nightBottom, fraction)
            )
        }
        // 밤 (22:00 ~ 4:00)
        else -> {
            listOf(palette.nightTop, palette.nightBottom)
        }
    }
}

fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    return Color(
        red = start.red + (end.red - start.red) * fraction,
        green = start.green + (end.green - start.green) * fraction,
        blue = start.blue + (end.blue - start.blue) * fraction,
        alpha = start.alpha + (end.alpha - start.alpha) * fraction
    )
}

@Composable
fun ColorSettingsDialog(
    currentPalette: SkyPalette,
    onPaletteChanged: (SkyPalette) -> Unit,
    onDismiss: () -> Unit
) {
    var palette by remember { mutableStateOf(currentPalette) }
    var editingColorKey by remember { mutableStateOf<String?>(null) }
    
    if (editingColorKey != null) {
        val initialColor = when(editingColorKey) {
            "nightTop" -> palette.nightTop
            "nightBottom" -> palette.nightBottom
            "sunriseTop" -> palette.sunriseTop
            "sunriseBottom" -> palette.sunriseBottom
            "dayTop" -> palette.dayTop
            "dayBottom" -> palette.dayBottom
            "sunsetTop" -> palette.sunsetTop
            "sunsetBottom" -> palette.sunsetBottom
            else -> Color.White
        }
        ColorPickerDialog(
            initialColor = initialColor,
            onColorSelected = { newColor ->
                palette = when(editingColorKey) {
                    "nightTop" -> palette.copy(nightTop = newColor)
                    "nightBottom" -> palette.copy(nightBottom = newColor)
                    "sunriseTop" -> palette.copy(sunriseTop = newColor)
                    "sunriseBottom" -> palette.copy(sunriseBottom = newColor)
                    "dayTop" -> palette.copy(dayTop = newColor)
                    "dayBottom" -> palette.copy(dayBottom = newColor)
                    "sunsetTop" -> palette.copy(sunsetTop = newColor)
                    "sunsetBottom" -> palette.copy(sunsetBottom = newColor)
                    else -> palette
                }
                editingColorKey = null
            },
            onDismiss = { editingColorKey = null }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("배경색 커스텀") },
            text = {
                Column {
                    PaletteRow("밤 (0시)", palette.nightTop, palette.nightBottom, { editingColorKey = "nightTop" }, { editingColorKey = "nightBottom" })
                    PaletteRow("일출 (6시)", palette.sunriseTop, palette.sunriseBottom, { editingColorKey = "sunriseTop" }, { editingColorKey = "sunriseBottom" })
                    PaletteRow("낮 (12시)", palette.dayTop, palette.dayBottom, { editingColorKey = "dayTop" }, { editingColorKey = "dayBottom" })
                    PaletteRow("일몰 (18시)", palette.sunsetTop, palette.sunsetBottom, { editingColorKey = "sunsetTop" }, { editingColorKey = "sunsetBottom" })
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "기본 색상으로 초기화",
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable {
                                palette = SkyPalette()
                            }
                            .padding(4.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onPaletteChanged(palette)
                    onDismiss()
                }) { Text("저장") }
            },
            dismissButton = {
                Button(onClick = onDismiss) { Text("취소") }
            }
        )
    }
}

@Composable
fun PaletteRow(label: String, topColor: Color, bottomColor: Color, onTopClick: () -> Unit, onBottomClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("상단", fontSize = 10.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(2.dp))
            Box(modifier = Modifier.size(36.dp).background(topColor, RoundedCornerShape(4.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(4.dp)).clickable(onClick = onTopClick))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("하단", fontSize = 10.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(2.dp))
            Box(modifier = Modifier.size(36.dp).background(bottomColor, RoundedCornerShape(4.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(4.dp)).clickable(onClick = onBottomClick))
        }
    }
}

@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val presetColors = listOf(
        Color(0xFF0B1021), Color(0xFF1A237E), Color(0xFF2B3252), Color(0xFF4A5578),
        Color(0xFF8FB8DE), Color(0xFFA4D8EE), Color(0xFFF2FAFF), Color(0xFF4DB6AC),
        Color(0xFFB39DDB), Color(0xFF9575CD), Color(0xFFF06292), Color(0xFFFFB7B2),
        Color(0xFFFFCBA4), Color(0xFFFF9E80), Color(0xFFFF7043), Color(0xFFFFF59D)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("색상 선택") },
        text = {
            Column {
                val rows = presetColors.chunked(4)
                rows.forEach { rowColors ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowColors.forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(c, RoundedCornerShape(8.dp))
                                    .clickable {
                                        onColorSelected(c)
                                    }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("닫기") }
        }
    )
}
