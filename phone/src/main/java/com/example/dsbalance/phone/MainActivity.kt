package com.example.dsbalance.phone

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date

private const val PREFS_NAME = "ds"
private const val KEY_API = "key"
private const val KEY_AUTOREFRESH = "autoRefresh"
private const val KEY_INTERVAL = "intervalSec"

// ---- OKX 深色配色 ----
private val BgDark = Color(0xFF0B0E11)
private val CardDark = Color(0xFF171A1F)
private val LineDark = Color(0xFF2A2E36)
private val TextMain = Color(0xFFEAECEF)
private val TextSub = Color(0xFF848E9C)
private val UpGreen = Color(0xFF2EBD85)
private val DownRed = Color(0xFFF6465D)
private val Accent = Color(0xFFFCD535)
private val BtnYellow = Color(0xFFFCD535)
private val BtnText = Color(0xFF0B0E11)

private val REFRESH_OPTIONS = listOf(30, 60, 120, 300)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = BgDark, surface = BgDark)) {
                Surface(modifier = Modifier.fillMaxSize(), color = BgDark) {
                    BalanceScreen(applicationContext)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceScreen(context: Context) {
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    var key by remember { mutableStateOf(prefs.getString(KEY_API, "").orEmpty()) }
    var savedKey by remember { mutableStateOf(prefs.getString(KEY_API, "").orEmpty()) }
    var autoRefresh by remember { mutableStateOf(prefs.getBoolean(KEY_AUTOREFRESH, false)) }
    var intervalSec by remember { mutableStateOf(prefs.getInt(KEY_INTERVAL, 30)) }

    var loading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Balance?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var hint by remember { mutableStateOf<String?>(null) }
    var lastUpdate by remember { mutableStateOf<String?>(null) }
    var showKeyPanel by remember { mutableStateOf(prefs.getString(KEY_API, "").isNullOrBlank()) }

    val scope = rememberCoroutineScope()

    fun saveKey(msg: String) {
        val trimmed = key.trim()
        prefs.edit().putString(KEY_API, trimmed).apply()
        savedKey = trimmed
        hint = msg
        error = null
        if (trimmed.isNotBlank()) showKeyPanel = false
    }

    fun runQuery(k: String) {
        if (k.isBlank()) { error = "\u8bf7\u5148\u4fdd\u5b58 API Key"; return }
        loading = true; error = null
        scope.launch {
            val r = withContext(Dispatchers.IO) { BalanceApi.query(k) }
            loading = false
            r.onSuccess {
                result = it
                lastUpdate = SimpleDateFormat("HH:mm:ss").format(Date())
                error = null
            }.onFailure { error = it.message ?: "\u672a\u77e5\u9519\u8bef" }
        }
    }

    LaunchedEffect(autoRefresh, savedKey, intervalSec) {
        if (!autoRefresh || savedKey.isBlank()) return@LaunchedEffect
        while (true) {
            runQuery(savedKey)
            delay(intervalSec * 1000L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("\u8d44\u4ea7", color = TextMain, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            StatusDot(autoRefresh, savedKey.isNotBlank())
            Spacer(Modifier.width(6.dp))
            Text(
                if (autoRefresh && savedKey.isNotBlank()) "\u5b9e\u65f6\u5237\u65b0\u4e2d"
                else "\u672a\u5b9e\u65f6",
                color = if (autoRefresh && savedKey.isNotBlank()) UpGreen else TextSub,
                fontSize = 12.sp
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardDark)
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("\u603b\u4f59\u989d", color = TextSub, fontSize = 13.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("\u2248 ${result?.currency ?: "CNY"}", color = TextSub, fontSize = 13.sp)
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        result?.total ?: "--",
                        color = TextMain,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    if (result != null) {
                        Text(
                            if (result!!.available) "\u53ef\u7528" else "\u4e0d\u53ef\u7528",
                            color = if (result!!.available) UpGreen else DownRed,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (lastUpdate != null) "\u6700\u540e\u66f4\u65b0 $lastUpdate" else "\u5c1a\u672a\u83b7\u53d6\u6570\u636e",
                    color = TextSub,
                    fontSize = 12.sp
                )
                if (loading) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Accent,
                        trackColor = LineDark
                    )
                }
            }
        }

        if (result != null) {
            Text("\u8d44\u4ea7\u660e\u7ec6", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardDark)
            ) {
                AssetRow("\u5145\u503c\u4f59\u989d", result!!.currency, result!!.toppedUp)
                DividerLine()
                AssetRow("\u8d60\u9001\u4f59\u989d", result!!.currency, result!!.granted)
                DividerLine()
                AssetRow("\u603b\u4f59\u989d", result!!.currency, result!!.total, highlight = true)
            }
        }

        Text("\u64cd\u4f5c", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

        Button(
            onClick = {
                val k = key.trim()
                saveKey("\u5df2\u4fdd\u5b58")
                runQuery(k)
            },
            enabled = !loading && key.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BtnYellow,
                contentColor = BtnText,
                disabledContainerColor = LineDark,
                disabledContentColor = TextSub
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (loading) "\u67e5\u8be2\u4e2d..." else "\u5237\u65b0\u8d44\u4ea7", fontWeight = FontWeight.SemiBold)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardDark)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("\u5b9e\u65f6\u5237\u65b0", color = TextMain, fontSize = 15.sp)
                Text(
                    "\u6bcf ${formatInterval(intervalSec)} \u81ea\u52a8\u66f4\u65b0\u4e00\u6b21",
                    color = TextSub,
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = autoRefresh,
                onCheckedChange = {
                    autoRefresh = it
                    prefs.edit().putBoolean(KEY_AUTOREFRESH, it).apply()
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = BtnText,
                    checkedTrackColor = UpGreen,
                    uncheckedThumbColor = TextSub,
                    uncheckedTrackColor = LineDark
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            REFRESH_OPTIONS.forEach { opt ->
                val selected = opt == intervalSec
                val bg = if (selected) Accent else CardDark
                val fg = if (selected) BtnText else TextSub
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .clickable {
                            intervalSec = opt
                            prefs.edit().putInt(KEY_INTERVAL, opt).apply()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(formatInterval(opt), color = fg, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("API Key", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            if (savedKey.isNotBlank()) {
                Text(mask(savedKey), color = TextSub, fontSize = 12.sp)
            }
        }

        if (showKeyPanel) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardDark)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it; hint = null },
                    label = { Text("DeepSeek API Key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextMain,
                        unfocusedTextColor = TextMain,
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = LineDark,
                        focusedLabelColor = Accent,
                        unfocusedLabelColor = TextSub,
                        cursorColor = Accent
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { saveKey("\u5df2\u4fdd\u5b58\u5230\u672c\u673a") },
                        enabled = key.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent,
                            contentColor = BtnText
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("\u4fdd\u5b58") }
                    OutlinedButton(
                        onClick = {
                            key = ""; savedKey = ""
                            prefs.edit().remove(KEY_API).apply()
                            result = null; error = null
                            hint = "\u5df2\u6e05\u9664"
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSub)
                    ) { Text("\u6e05\u9664") }
                }
            }
        } else {
            OutlinedButton(
                onClick = { showKeyPanel = true },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMain)
            ) { Text("\u4fee\u6539 API Key") }
        }

        hint?.let { InfoCard(it, UpGreen) }
        error?.let { InfoCard(it, DownRed) }

        Spacer(Modifier.height(4.dp))
        Text(
            "Key \u4ec5\u4fdd\u5b58\u5728\u672c\u673a\uff0c\u4e0d\u4e0a\u4f20\u670d\u52a1\u5668\u3002",
            color = TextSub,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun StatusDot(active: Boolean, hasKey: Boolean) {
    val color = if (active && hasKey) UpGreen else TextSub
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun AssetRow(label: String, currency: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextMain, fontSize = 15.sp)
        Spacer(Modifier.weight(1f))
        Text(
            "$value $currency",
            color = if (highlight) Accent else TextMain,
            fontSize = 15.sp,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(LineDark)
    )
}

@Composable
private fun InfoCard(msg: String, accent: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardDark)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(Modifier.width(10.dp))
            Text(msg, color = TextMain, fontSize = 13.sp)
        }
    }
}

private fun mask(k: String): String {
    if (k.length <= 10) return "****"
    return k.take(6) + "****" + k.takeLast(4)
}

private fun formatInterval(sec: Int): String =
    if (sec % 60 == 0) "${sec / 60} \u5206\u949f" else "$sec \u79d2"