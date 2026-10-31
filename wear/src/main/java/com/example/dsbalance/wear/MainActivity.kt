package com.example.dsbalance.wear

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.ColorScheme as WearColorScheme
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.MaterialTheme as WearTheme
import androidx.wear.compose.material3.MotionScheme
import androidx.wear.compose.material3.OutlinedCard
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text as WearText
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.networks.data.Status
import com.google.android.horologist.networks.status.NetworkRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ---- 署名 / 版本 ----
private const val AUTHOR = "XXX"
private const val APP_VERSION = "v3.9"

private const val PREFS = "ds"
private const val K_KEY = "key"

private const val PAGE_BALANCE = 0
private const val PAGE_SETTINGS = 1
private const val PAGE_ABOUT = 2
private const val PAGE_REWARD = 3
private const val PAGE_COUNT = 4

// ---- 左滑收起内容的比例：0.55 → 内容缩到 45% ----
private const val CONTENT_SHRINK = 0.55f

// ---- M3E 大色卡配色（取自 guide_m3e_home.jpg）----
private val CardBlueTop = Color(0xFFBBD6F2)
private val CardBlueBottom = Color(0xFF8CB2DC)
private val OnCard = Color(0xFF14202C)

// ---- 静态回退配色（API < 31 无 Monet）----
private fun fallbackScheme(): ColorScheme = darkColorScheme(
    primary = Color(0xFFFCD535),
    onPrimary = Color(0xFF0B0E11),
    primaryContainer = Color(0xFF3A3320),
    onPrimaryContainer = Color(0xFFFCD535),
    secondaryContainer = Color(0xFF24282F),
    onSecondaryContainer = Color(0xFFEAECEF),
    background = Color(0xFF0B0E11),
    onBackground = Color(0xFFEAECEF),
    surface = Color(0xFF0B0E11),
    onSurface = Color(0xFFEAECEF),
    surfaceVariant = Color(0xFF1C2027),
    onSurfaceVariant = Color(0xFF9AA3AE),
    outline = Color(0xFF2E333B),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    tertiary = Color(0xFF7BD88F)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearAppTheme {
                App(applicationContext)
            }
        }
    }
}

/** Wear Material 3 主题：Monet 取色 → Wear M3 ColorScheme + 弹性动效 */
@Composable
private fun WearAppTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val base = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(ctx)
    } else {
        fallbackScheme()
    }

    val scheme = WearColorScheme().copy(
        primary = base.primary,
        onPrimary = base.onPrimary,
        primaryContainer = base.primaryContainer,
        onPrimaryContainer = base.onPrimaryContainer,
        secondaryContainer = base.secondaryContainer,
        onSecondaryContainer = base.onSecondaryContainer,
        background = base.background,
        onBackground = base.onBackground,
        surfaceContainer = base.surfaceVariant,
        surfaceContainerHigh = base.surfaceContainerHigh,
        onSurface = base.onSurface,
        onSurfaceVariant = base.onSurfaceVariant,
        outline = base.outline,
        error = base.error,
        onError = base.onError
    )

    WearTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}

@OptIn(ExperimentalHorologistApi::class)
@Composable
private fun App(context: Context) {
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var savedKey by remember { mutableStateOf(prefs.getString(K_KEY, "").orEmpty()) }
    var draftKey by remember { mutableStateOf(savedKey) }

    var loading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Balance?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    var sidebarOpen by remember { mutableStateOf(false) }
    val reveal by animateFloatAsState(if (sidebarOpen) 1f else 0f, label = "reveal")

    val connectivity = remember {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    val networkRepository = remember { NetworkRepositoryImpl(connectivity, scope) }
    val networks by networkRepository.networkStatus.collectAsState()
    val online = networks.status is Status.Available
    DisposableEffect(Unit) { onDispose { networkRepository.close() } }

    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val listStates = List(PAGE_COUNT) { rememberTransformingLazyColumnState() }
    val transformationSpec = rememberTransformationSpec()

    fun query(k: String) {
        if (k.isBlank()) {
            error = "请先在设置中填写 API Key"
            return
        }
        loading = true
        error = null
        scope.launch {
            val r = withContext(Dispatchers.IO) { BalanceApi.query(k.trim()) }
            loading = false
            r.onSuccess { result = it }
                .onFailure { error = it.message ?: "未知错误" }
        }
    }

    fun go(page: Int) {
        scope.launch { pagerState.animateScrollToPage(page) }
    }

    LaunchedEffect(Unit) {
        if (savedKey.isBlank()) pagerState.scrollToPage(PAGE_SETTINGS)
    }

    val cs = WearTheme.colorScheme

    val dotColor = when {
        result?.available == true -> cs.tertiary
        result != null -> cs.error
        error != null -> cs.error
        else -> cs.onSurfaceVariant
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppScaffold(
            containerColor = cs.background,
            timeText = { TimeText(backgroundColor = cs.surfaceContainerHigh) }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        // 与系统「右滑返回」抢手势：命中后 consume，避免同时触发退出
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            if (dragAmount < -20f && !sidebarOpen) sidebarOpen = true
                            else if (dragAmount > 20f && sidebarOpen) sidebarOpen = false
                        }
                    }
            ) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = false,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val base = 1f - CONTENT_SHRINK * reveal
                            scaleX = base
                            scaleY = base
                            translationX = -48.dp.toPx() * reveal
                        }
                ) { page ->
                    when (page) {
                        PAGE_BALANCE -> BalancePage(
                            listState = listStates[PAGE_BALANCE],
                            spec = transformationSpec,
                            hasKey = savedKey.isNotBlank(),
                            loading = loading,
                            result = result,
                            error = error,
                            online = online,
                            onQuery = { query(savedKey) }
                        )

                        PAGE_SETTINGS -> SettingsPage(
                            listState = listStates[PAGE_SETTINGS],
                            spec = transformationSpec,
                            key = draftKey,
                            onKeyChange = { draftKey = it },
                            onSave = {
                                val k = draftKey.trim()
                                prefs.edit().putString(K_KEY, k).apply()
                                savedKey = k
                                go(PAGE_BALANCE)
                                if (k.isNotBlank()) query(k) else { result = null; error = null }
                            }
                        )

                        PAGE_ABOUT -> AboutPage(
                            listState = listStates[PAGE_ABOUT],
                            spec = transformationSpec,
                            dotColor = dotColor
                        )

                        else -> RewardPage(
                            listState = listStates[PAGE_REWARD],
                            spec = transformationSpec
                        )
                    }
                }

                if (reveal > 0.001f) {
                    WearArcMenu(
                        expanded = sidebarOpen,
                        current = pagerState.currentPage,
                        onSelect = { go(it); sidebarOpen = false }
                    )
                }
            }
        }
    }
}

/**
 * M3E 极坐标弧形悬浮菜单（照 wear-m3e-radial-menu skill）
 * - 半径 80dp，角度 -45°..45°，48dp 触控目标
 * - 每项独立 Animatable，错峰 50ms 从圆心弹出
 * - spring(StiffnessMediumLow, DampingRatioMediumBouncy) 弹性，收回用 tween(200)
 */
@Composable
private fun WearArcMenu(
    expanded: Boolean,
    current: Int,
    onSelect: (Int) -> Unit,
    startAngle: Float = -45f,
    endAngle: Float = 45f,
    radius: Dp = 80.dp,
) {
    val cs = WearTheme.colorScheme
    val density = LocalDensity.current
    val radiusPx = with(density) { radius.toPx() }

    val icons = listOf(NcmIcons.Chart, NcmIcons.Settings, NcmIcons.Info, NcmIcons.Heart)
    val count = icons.size
    val step = (endAngle - startAngle) / (count - 1)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        icons.forEachIndexed { index, vector ->
            val angleDeg = startAngle + index * step
            val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()

            val progress = remember { Animatable(if (expanded) 1f else 0f) }
            LaunchedEffect(expanded) {
                if (expanded) {
                    delay(index * 50L) // 错峰弹出
                    progress.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMediumLow,
                            dampingRatio = Spring.DampingRatioMediumBouncy
                        )
                    )
                } else {
                    progress.animateTo(targetValue = 0f, animationSpec = tween(200))
                }
            }

            val r = radiusPx * progress.value
            val dx = cos(angleRad) * r
            val dy = sin(angleRad) * r
            val selected = index == current

            Box(
                modifier = Modifier
                    .offset { IntOffset(dx.roundToInt(), dy.roundToInt()) }
                    .scale(progress.value)
                    .alpha(progress.value)
                    .size(48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) cs.primary else cs.surfaceContainerHigh)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = vector,
                    contentDescription = null,
                    tint = if (selected) cs.onPrimary else cs.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/** 统一的列表骨架：AppScaffold 内的 ScreenScaffold + TransformingLazyColumn */
@Composable
private fun WearList(
    listState: TransformingLazyColumnState,
    content: TransformingLazyColumnScope.() -> Unit,
) {
    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(listState),
            rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(listState),
            content = content
        )
    }
}

// ============================ 余额页 ============================
@Composable
private fun BalancePage(
    listState: TransformingLazyColumnState,
    spec: TransformationSpec,
    hasKey: Boolean,
    loading: Boolean,
    result: Balance?,
    error: String?,
    online: Boolean,
    onQuery: () -> Unit,
) {
    WearList(listState) {
        item(key = "title") {
            ListHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding)
                    .transformedHeight(this, spec),
                transformation = SurfaceTransformation(spec)
            ) {
                WearText(
                    text = "DeepSeek 余额",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        item(key = "card") {
            // M3E 大色卡：透明容器 + 内部渐变，保留 guide_m3e_home 的浅蓝观感
            Card(
                onClick = {},
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumVerticalContentPadding(CardDefaults.minimumVerticalListContentPadding)
                    .transformedHeight(this, spec),
                transformation = SurfaceTransformation(spec)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(CardBlueTop, CardBlueBottom)))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(Color.White.copy(alpha = 0.38f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = NcmIcons.Chart,
                                contentDescription = null,
                                tint = OnCard,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        WearText(
                            text = "DeepSeek 余额",
                            color = OnCard,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (result != null) {
                            WearText(
                                text = "${result.currency} ${result.total}",
                                color = OnCard,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            WearText(
                                text = "充值 ${result.toppedUp} · 赠送 ${result.granted}",
                                color = OnCard.copy(alpha = 0.7f),
                                fontSize = 9.sp
                            )
                        } else {
                            WearText(
                                text = if (loading) "查询中…" else if (hasKey) "还没有数据" else "还没有添加 API Key",
                                color = OnCard.copy(alpha = 0.7f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        item(key = "query") {
            Button(
                onClick = onQuery,
                enabled = !loading && hasKey && online,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                    .transformedHeight(this, spec),
                transformation = SurfaceTransformation(spec)
            ) {
                WearText(if (loading) "查询中…" else "查询", fontSize = 13.sp)
            }
        }

        if (!online) {
            item(key = "offline") {
                WearText("手表未联网", color = WearTheme.colorScheme.error, fontSize = 10.sp)
            }
        }

        error?.let { msg ->
            item(key = "error") {
                WearText(
                    text = msg,
                    color = WearTheme.colorScheme.error,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

// ============================ 设置页 ============================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsPage(
    listState: TransformingLazyColumnState,
    spec: TransformationSpec,
    key: String,
    onKeyChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    val cs = WearTheme.colorScheme

    WearList(listState) {
        item(key = "title") {
            ListHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding)
                    .transformedHeight(this, spec),
                transformation = SurfaceTransformation(spec)
            ) {
                WearText("设置", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        item(key = "field") {
            OutlinedCard(
                onClick = {},
                contentPadding = PaddingValues(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumVerticalContentPadding(CardDefaults.minimumVerticalListContentPadding)
                    .transformedHeight(this, spec),
                transformation = SurfaceTransformation(spec)
            ) {
                OutlinedTextField(
                    value = key,
                    onValueChange = onKeyChange,
                    label = { Text("API Key", fontSize = 11.sp, color = cs.onSurfaceVariant) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = cs.onBackground,
                        unfocusedTextColor = cs.onBackground,
                        focusedBorderColor = cs.primary,
                        unfocusedBorderColor = cs.outline,
                        focusedLabelColor = cs.primary,
                        unfocusedLabelColor = cs.onSurfaceVariant,
                        cursorColor = cs.primary
                    )
                )
            }
        }

        item(key = "save") {
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                    .transformedHeight(this, spec),
                transformation = SurfaceTransformation(spec)
            ) {
                WearText("保存", fontSize = 13.sp)
            }
        }

        item(key = "clear") {
            Button(
                onClick = { onKeyChange(""); onSave() },
                colors = ButtonDefaults.filledTonalButtonColors(contentColor = cs.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                    .transformedHeight(this, spec),
                transformation = SurfaceTransformation(spec)
            ) {
                WearText("清除 Key", fontSize = 11.sp)
            }
        }
    }
}

// ============================ 关于页 ============================
@Composable
private fun AboutPage(
    listState: TransformingLazyColumnState,
    spec: TransformationSpec,
    dotColor: Color,
) {
    WearList(listState) {
        item(key = "title") {
            ListHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding)
                    .transformedHeight(this, spec),
                transformation = SurfaceTransformation(spec)
            ) {
                WearText("关于", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        item(key = "card") {
            Card(
                onClick = {},
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumVerticalContentPadding(CardDefaults.minimumVerticalListContentPadding)
                    .transformedHeight(this, spec),
                transformation = SurfaceTransformation(spec)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(dotColor)
                        )
                        WearText(
                            "开发者 · $AUTHOR",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    WearText("DeepSeek 余额 $APP_VERSION", fontSize = 10.sp)
                    WearText("Wear Material 3 · Monet", fontSize = 10.sp)
                }
            }
        }
    }
}

/** 赞赏页 */
@Composable
private fun RewardPage(
    listState: TransformingLazyColumnState,
    spec: TransformationSpec,
) {
    WearList(listState) {
        item(key = "title") {
            ListHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding)
                    .transformedHeight(this, spec),
                transformation = SurfaceTransformation(spec)
            ) {
                WearText("赞赏", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        item(key = "qr") {
            Card(
                onClick = {},
                colors = CardDefaults.cardColors(containerColor = Color.White),
                contentPadding = PaddingValues(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumVerticalContentPadding(CardDefaults.minimumVerticalListContentPadding)
                    .transformedHeight(this, spec),
                transformation = SurfaceTransformation(spec)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.reward_qr),
                    contentDescription = "赞赏码",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        item(key = "hint") {
            WearText(
                "扫码支持开发者",
                color = WearTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}
