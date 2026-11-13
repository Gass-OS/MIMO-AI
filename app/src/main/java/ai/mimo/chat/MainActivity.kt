package ai.mimo.chat

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class MimoTheme(
    val id: String, val title: String, val background: Color,
    val surface: Color, val accent: Color, val secondary: Color
)

private val themes = listOf(
    MimoTheme("blue", "Blue Dark", Color(0xFF08111F), Color(0xFF14243D), Color(0xFF75B7FF), Color(0xFFB8D5F5)),
    MimoTheme("gold", "Golden Lux", Color(0xFF100E0A), Color(0xFF211A10), Color(0xFFE2B85C), Color(0xFFD9C69E)),
    MimoTheme("red", "Blood Red", Color(0xFF12090D), Color(0xFF261118), Color(0xFFFF5268), Color(0xFFE7B0B9)),
    MimoTheme("black", "Pure Black", Color(0xFF050608), Color(0xFF15171B), Color(0xFFE6E8ED), Color(0xFFAEB4C0))
)

private data class ChatMessage(val text: String, val fromUser: Boolean)

class MainActivity : ComponentActivity() {
    @Suppress("DEPRECATION")
    private fun enableImmersiveMode() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveMode()
        val prefs = getSharedPreferences("mimo_settings", Context.MODE_PRIVATE)
        setContent { MimoApp(prefs) }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableImmersiveMode()
    }
}

@Composable
private fun MimoApp(prefs: android.content.SharedPreferences) {
    var themeId by remember { mutableStateOf(prefs.getString("theme", "blue") ?: "blue") }
    var showWelcome by remember { mutableStateOf(!prefs.getBoolean("welcome_seen", false)) }
    var page by remember { mutableStateOf("chat") }
    val theme = themes.firstOrNull { it.id == themeId } ?: themes.first()

    MaterialTheme(colorScheme = darkColorScheme(
        background = theme.background, surface = theme.surface,
        primary = theme.accent, onPrimary = Color(0xFF080B10),
        onBackground = Color(0xFFF4F6FA), onSurface = Color(0xFFF4F6FA),
        secondary = theme.secondary
    )) {
        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(theme.surface.copy(alpha = 0.72f), theme.background, theme.background))
        )) {
            if (showWelcome) {
                WelcomeScreen(theme, onStart = {
                    prefs.edit().putBoolean("welcome_seen", true).apply()
                    showWelcome = false
                })
            } else if (page == "settings") {
                SettingsScreen(theme, themeId, onTheme = {
                    themeId = it
                    prefs.edit().putString("theme", it).apply()
                }, onBack = { page = "chat" })
            } else {
                ChatScreen(theme, onSettings = { page = "settings" })
            }
        }
    }
}

@Composable
private fun BrandMark(theme: MimoTheme, size: Int = 54) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape((size * 0.28f).dp))
            .background(Brush.linearGradient(listOf(theme.accent.copy(alpha = 0.28f), theme.surface)))
            .border(1.dp, theme.accent.copy(alpha = 0.55f), RoundedCornerShape((size * 0.28f).dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("M", color = theme.accent, fontSize = (size * 0.58f).sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun WelcomeScreen(theme: MimoTheme, onStart: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BrandMark(theme, 88)
        Spacer(Modifier.height(20.dp))
        Text("MIMO AI", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = theme.accent, letterSpacing = 2.sp)
        Text("One chat. Two creative minds. Your device.", color = theme.secondary, textAlign = TextAlign.Center, fontSize = 14.sp)
        Spacer(Modifier.height(34.dp))
        InfoFeature(theme, Icons.Outlined.Forum, "Talk naturally", "Ask questions, write, translate, learn and explore with your local AI assistant.")
        InfoFeature(theme, Icons.Outlined.AutoAwesome, "Create images", "Describe an image and MIMO will create it and place the result directly in your chat.")
        InfoFeature(theme, Icons.Outlined.PhoneAndroid, "Private and on-device", "After the one-time model setup, supported AI features are designed to work offline.")
        InfoFeature(theme, Icons.Outlined.Download, "One-time setup", "The AI models are large, so the first launch will download and verify them with a progress bar.")
        Spacer(Modifier.height(28.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)) {
            Text("Enter MIMO AI", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Outlined.ArrowForward, null)
        }
        Spacer(Modifier.height(12.dp))
        Text("Your assistant is getting ready.", color = theme.secondary.copy(alpha = 0.75f), fontSize = 12.sp)
    }
}

@Composable
private fun InfoFeature(theme: MimoTheme, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp)
            .clip(RoundedCornerShape(18.dp)).background(theme.surface.copy(alpha = 0.78f)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = theme.accent, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, color = Color.White)
            Text(body, color = theme.secondary, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun ChatScreen(theme: MimoTheme, onSettings: () -> Unit) {
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var input by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            BrandMark(theme, 42)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text("MIMO AI", fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = theme.accent)
                Text("Your personal AI space", fontSize = 11.sp, color = theme.secondary)
            }
            IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Settings", tint = theme.secondary) }
        }
        if (messages.isEmpty()) {
            Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                BrandMark(theme, 76)
                Spacer(Modifier.height(18.dp))
                Text(MimoIdentity.chatGreeting, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color.White)
                Text("Chat, explore ideas or create something amazing.", color = theme.secondary, fontSize = 13.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(25.dp))
                SuggestionChip("Explain something", Icons.Outlined.Lightbulb, theme) { input = "Explain something interesting to me" }
                SuggestionChip("Help me write", Icons.Outlined.EditNote, theme) { input = "Help me write something" }
                SuggestionChip("Create an image", Icons.Outlined.Image, theme) { input = "Create an image: " }
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(messages) { msg ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (msg.fromUser) Arrangement.End else Arrangement.Start) {
                        Surface(color = if (msg.fromUser) theme.accent.copy(alpha = 0.20f) else theme.surface, shape = RoundedCornerShape(18.dp)) {
                            Text(msg.text, Modifier.padding(14.dp).widthIn(max = 290.dp), color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(theme.surface)
                .border(1.dp, theme.accent.copy(alpha = 0.24f), RoundedCornerShape(24.dp)).padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { messages.add(ChatMessage(MimoIdentity.imageGreeting, false)) }) {
                Icon(Icons.Outlined.AddPhotoAlternate, "Image generation", tint = theme.accent)
            }
            BasicTextField(
                value = input, onValueChange = { input = it },
                modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 14.sp),
                decorationBox = { inner ->
                    Box {
                        if (input.isEmpty()) Text("Message MIMO AI...", color = theme.secondary.copy(alpha = 0.7f), fontSize = 14.sp)
                        inner()
                    }
                }
            )
            IconButton(onClick = {
                if (input.isNotBlank()) {
                    messages.add(ChatMessage(input.trim(), true))
                    messages.add(ChatMessage(MimoIdentity.previewReply, false))
                    input = ""
                }
            }) { Icon(Icons.Outlined.Send, "Send", tint = theme.accent) }
        }
        Text("Preview interface · AI engines not connected yet", Modifier.fillMaxWidth().padding(top = 8.dp), color = theme.secondary.copy(alpha = 0.65f), fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SuggestionChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, theme: MimoTheme, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
        color = theme.surface.copy(alpha = 0.75f), shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.accent.copy(alpha = 0.18f))
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = theme.accent, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(10.dp))
            Text(label, color = Color.White, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Outlined.ArrowForward, null, tint = theme.secondary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SettingsScreen(theme: MimoTheme, selected: String, onTheme: (String) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = theme.secondary) }
            Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
            item {
                SettingsHeader("APPEARANCE", theme)
                Text("Choose your MIMO style", color = theme.secondary, fontSize = 13.sp)
                themes.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(16.dp))
                            .background(item.surface).border(if (selected == item.id) 2.dp else 1.dp, if (selected == item.id) item.accent else Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .clickable { onTheme(item.id) }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(32.dp).clip(CircleShape).background(item.background).border(2.dp, item.accent, CircleShape))
                        Spacer(Modifier.width(12.dp))
                        Text(item.title, Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.SemiBold)
                        if (selected == item.id) Icon(Icons.Outlined.CheckCircle, null, tint = item.accent)
                    }
                }
            }
            item {
                SettingsHeader("AI MODELS", theme)
                SettingsRow(Icons.Outlined.Psychology, "Language model", "Not installed · planned", theme)
                SettingsRow(Icons.Outlined.Image, "Image generation", "Not installed · planned", theme)
                SettingsRow(Icons.Outlined.Storage, "Model storage", "Model manager coming soon", theme)
            }
            item {
                SettingsHeader("PREFERENCES", theme)
                SettingsRow(Icons.Outlined.Language, "Language", "English · more languages planned", theme)
                SettingsRow(Icons.Outlined.Wifi, "Downloads", "Wi-Fi preference coming soon", theme)
                SettingsRow(Icons.Outlined.Security, "Privacy", "Designed for on-device AI", theme)
            }
            item {
                SettingsHeader("ABOUT", theme)
                SettingsRow(Icons.Outlined.Info, "About MIMO AI", "Your AI. Your device. Your conversation.", theme)
                SettingsRow(Icons.Outlined.Code, "Version", "0.1.0 · Early preview", theme)
                Text("MIMO AI is an independent project. Model names and third-party components will be credited with their applicable licenses before release.", color = theme.secondary, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun SettingsHeader(label: String, theme: MimoTheme) {
    Text(label, Modifier.padding(top = 14.dp, bottom = 8.dp), color = theme.accent, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
}

@Composable
private fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, theme: MimoTheme) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = theme.accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(13.dp))
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(subtitle, color = theme.secondary, fontSize = 11.sp)
        }
    }
}
