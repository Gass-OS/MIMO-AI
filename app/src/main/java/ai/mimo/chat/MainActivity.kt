package ai.mimo.chat

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    var themeId by remember { mutableStateOf(prefs.getString("theme", "blue") ?: "blue") }
    var showWelcome by remember { mutableStateOf(!prefs.getBoolean("welcome_seen", false)) }
    var page by remember { mutableStateOf("chat") }
    var locationGranted by remember {
        mutableStateOf(context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    var notificationsGranted by remember {
        mutableStateOf(Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
    }
    var selectedPhoto by remember { mutableStateOf(prefs.getString("reference_photo_uri", null)) }
    var wifiOnly by remember { mutableStateOf(prefs.getBoolean("wifi_only", true)) }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        locationGranted = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsGranted = it
    }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { }
            selectedPhoto = uri.toString()
            prefs.edit().putString("reference_photo_uri", uri.toString()).apply()
            Toast.makeText(context, "Reference image selected", Toast.LENGTH_SHORT).show()
        }
    }
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
            } else when (page) {
                "settings" -> SettingsScreen(theme, themeId, wifiOnly, onWifiOnly = {
                    wifiOnly = it
                    prefs.edit().putBoolean("wifi_only", it).apply()
                }, onTheme = {
                    themeId = it
                    prefs.edit().putString("theme", it).apply()
                }, onBack = { page = "chat" }, onManageModels = { page = "models" },
                    onPermissions = { page = "permissions" }, onAbout = { page = "about" }, onProjects = { page = "projects" })
                "models" -> ModelManagerScreen(theme, onBack = { page = "settings" })
                "permissions" -> PermissionsScreen(theme, locationGranted, notificationsGranted, selectedPhoto != null,
                    onBack = { page = "settings" },
                    onLocation = { locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)) },
                    onNotifications = {
                        if (Build.VERSION.SDK_INT >= 33) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else notificationsGranted = true
                    },
                    onPhoto = { photoLauncher.launch(ActivityResultContracts.PickVisualMedia.ImageOnly) },
                    onAppSettings = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) })
                "about" -> AboutScreen(theme, onBack = { page = "settings" })
                "projects" -> ProjectsScreen(theme, onBack = { page = "settings" }, onOpen = { url ->
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                    catch (_: Exception) { Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show() }
                })
                else -> ChatScreen(theme, onSettings = { page = "settings" })
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
private fun SettingsScreen(theme: MimoTheme, selected: String, wifiOnly: Boolean,
    onWifiOnly: (Boolean) -> Unit, onTheme: (String) -> Unit, onBack: () -> Unit,
    onManageModels: () -> Unit, onPermissions: () -> Unit, onAbout: () -> Unit, onProjects: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = theme.secondary) }
            Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
            item {
                SettingsHeader("APPEARANCE", theme)
                Text("Choose your MIMO style", color = theme.secondary, fontSize = 13.sp)
                themes.forEach { item ->
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(16.dp))
                        .background(item.surface).border(if (selected == item.id) 2.dp else 1.dp,
                            if (selected == item.id) item.accent else Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .clickable { onTheme(item.id) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(32.dp).clip(CircleShape).background(item.background).border(2.dp, item.accent, CircleShape))
                        Spacer(Modifier.width(12.dp))
                        Text(item.title, Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.SemiBold)
                        if (selected == item.id) Icon(Icons.Outlined.CheckCircle, null, tint = item.accent)
                    }
                }
            }
            item {
                SettingsHeader("AI & MODELS", theme)
                SettingsRow(Icons.Outlined.Psychology, "Language engine", "Not installed · source verification pending", theme)
                SettingsRow(Icons.Outlined.AutoAwesome, "Image engine", "Not installed · source verification pending", theme)
                SettingsRow(Icons.Outlined.Storage, "Model Manager", "Manage setup and offline model files", theme, onManageModels)
            }
            item {
                SettingsHeader("DEVICE & PERMISSIONS", theme)
                SettingsRow(Icons.Outlined.MyLocation, "Location and GPS", "Permission controls and nearby-place support", theme, onPermissions)
                SettingsRow(Icons.Outlined.NotificationsActive, "Notifications", "Choose whether MIMO may notify you", theme, onPermissions)
                SettingsRow(Icons.Outlined.PhotoLibrary, "Reference pictures", "Select a picture with Android Photo Picker", theme, onPermissions)
                SettingsRow(Icons.Outlined.Security, "App permissions", "Review Android permission settings", theme, onPermissions)
            }
            item {
                SettingsHeader("PREFERENCES", theme)
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(theme.surface).padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Wifi, null, tint = theme.accent, modifier = Modifier.size(23.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Wi-Fi only downloads", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Avoid large model downloads on mobile data", color = theme.secondary, fontSize = 11.sp)
                    }
                    Switch(checked = wifiOnly, onCheckedChange = onWifiOnly)
                }
                SettingsRow(Icons.Outlined.Language, "Language", "English · language selector planned", theme)
                SettingsRow(Icons.Outlined.BatteryFull, "Battery and performance", "AI engines will manage memory independently", theme)
            }
            item {
                SettingsHeader("ABOUT & PROJECTS", theme)
                SettingsRow(Icons.Outlined.Info, "About MIMO AI", "Purpose, privacy and creator", theme, onAbout)
                SettingsRow(Icons.Outlined.PhoneAndroid, "About this APK", "Version 0.1.0 · early preview build", theme, onAbout)
                SettingsRow(Icons.Outlined.Code, "My Projects", "Explore apps and open-source work", theme, onProjects)
            }
        }
    }
}

@Composable
private fun ModelManagerScreen(theme: MimoTheme, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = theme.secondary) }
            Column {
                Text("Model Manager", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("Prepare MIMO for offline use", color = theme.secondary, fontSize = 12.sp)
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
            item {
                Surface(color = theme.surface, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Psychology, null, tint = theme.accent, modifier = Modifier.size(26.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("MIMO Chat", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Language and conversation engine", color = theme.secondary, fontSize = 11.sp)
                            }
                            Surface(color = Color(0xFF6B552A), shape = RoundedCornerShape(20.dp)) {
                                Text("Not installed", Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = Color(0xFFFFD98A), fontSize = 10.sp)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        LinearProgressIndicator(progress = { 0f }, modifier = Modifier.fillMaxWidth(), color = theme.accent, trackColor = theme.background)
                        Text("Waiting for a verified download source", Modifier.padding(top = 8.dp), color = theme.secondary, fontSize = 11.sp)
                    }
                }
            }
            item {
                Surface(color = theme.surface, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.AutoAwesome, null, tint = theme.accent, modifier = Modifier.size(26.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("MIMO Image Studio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Local image creation engine", color = theme.secondary, fontSize = 11.sp)
                            }
                            Surface(color = Color(0xFF6B552A), shape = RoundedCornerShape(20.dp)) {
                                Text("Not installed", Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = Color(0xFFFFD98A), fontSize = 10.sp)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        LinearProgressIndicator(progress = { 0f }, modifier = Modifier.fillMaxWidth(), color = theme.accent, trackColor = theme.background)
                        Text("Waiting for a verified download source", Modifier.padding(top = 8.dp), color = theme.secondary, fontSize = 11.sp)
                    }
                }
            }
            item {
                SettingsHeader("SETUP CHECKLIST", theme)
                SettingsRow(Icons.Outlined.VerifiedUser, "Source and license", "Must be checked before distributing model files", theme)
                SettingsRow(Icons.Outlined.Fingerprint, "File integrity", "SHA-256 verification will be required", theme)
                SettingsRow(Icons.Outlined.CloudDownload, "Resumable downloads", "Not enabled until secure sources are configured", theme)
                SettingsRow(Icons.Outlined.Storage, "Device storage", "Storage check will run before each download", theme)
            }
            item {
                Text("No download has started. Progress is shown as 0% because no verified model files or download URLs are configured yet. MIMO will not report an engine as ready until its files are downloaded and verified.", color = theme.secondary, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}


@Composable
private fun PermissionsScreen(theme: MimoTheme, locationGranted: Boolean, notificationsGranted: Boolean,
    photoSelected: Boolean, onBack: () -> Unit, onLocation: () -> Unit, onNotifications: () -> Unit,
    onPhoto: () -> Unit, onAppSettings: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = theme.secondary) }
            Text("Device permissions", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Text("MIMO asks only when a feature needs access. You can change these choices in Android settings at any time.",
                    color = theme.secondary, fontSize = 12.sp, lineHeight = 18.sp)
            }
            item {
                PermissionCard(theme, Icons.Outlined.MyLocation, "Location while using MIMO",
                    if (locationGranted) "Allowed" else "Not allowed",
                    "Can provide your approximate or precise position for nearby hospitals and local context. GPS itself can work offline; place information needs downloaded data or internet.",
                    if (locationGranted) "Permission granted" else "Allow location", onLocation, locationGranted)
            }
            item {
                PermissionCard(theme, Icons.Outlined.NotificationsActive, "Notifications",
                    if (notificationsGranted) "Allowed" else "Not allowed",
                    "Used for download completion, model setup and important app alerts. MIMO does not need notification access to answer questions.",
                    if (notificationsGranted) "Permission granted" else "Allow notifications", onNotifications, notificationsGranted)
            }
            item {
                PermissionCard(theme, Icons.Outlined.PhotoLibrary, "Reference picture",
                    if (photoSelected) "Picture selected" else "No picture selected",
                    "Choose only the picture you want to share with MIMO. Android Photo Picker grants access to the selected item; broad storage permission is not required.",
                    "Choose a picture", onPhoto, photoSelected)
            }
            item {
                SettingsRow(Icons.Outlined.Settings, "Android app settings", "Manage all permissions and notifications", theme, onAppSettings)
            }
            item {
                Text("Offline note: location can be read without internet after permission is granted. Nearby hospitals require an offline map/place database to be bundled or downloaded. Current weather requires a network connection or a previously cached forecast.",
                    color = theme.secondary, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun PermissionCard(theme: MimoTheme, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String,
    status: String, description: String, action: String, onClick: () -> Unit, granted: Boolean) {
    Surface(color = theme.surface, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = theme.accent, modifier = Modifier.size(25.dp))
                Spacer(Modifier.width(10.dp))
                Text(title, Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Bold)
                Text(status, color = if (granted) Color(0xFF80D8A0) else theme.secondary, fontSize = 10.sp)
            }
            Text(description, color = theme.secondary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 10.dp))
            OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Icon(if (granted) Icons.Outlined.Check else Icons.Outlined.Tune, null)
                Spacer(Modifier.width(8.dp))
                Text(action)
            }
        }
    }
}

@Composable
private fun AboutScreen(theme: MimoTheme, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = theme.secondary) }
            Text("About MIMO AI", fontSize = 23.sp, fontWeight = FontWeight.Bold)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Surface(color = theme.surface, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        BrandMark(theme, 76)
                        Text("MIMO AI", color = theme.accent, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 12.dp))
                        Text("Your assistant. Your device. Your control.", color = theme.secondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }
            item { SettingsHeader("ABOUT THE ASSISTANT", theme) }
            item { SettingsRow(Icons.Outlined.Forum, "One conversation", "Chat and image creation are designed to live in the same conversation.", theme) }
            item { SettingsRow(Icons.Outlined.OfflineBolt, "Offline-first", "The goal is local AI inference after models are installed. This preview does not yet contain connected AI engines.", theme) }
            item { SettingsRow(Icons.Outlined.PrivacyTip, "Privacy", "Your prompts and selected pictures are intended to be processed on-device by local engines. Optional live services such as weather need internet.", theme) }
            item { SettingsHeader("ABOUT THIS APK", theme) }
            item { SettingsRow(Icons.Outlined.Android, "Application", "MIMO AI · package ai.mimo.chat", theme) }
            item { SettingsRow(Icons.Outlined.Tag, "Version", "0.1.0 · development preview", theme) }
            item { SettingsRow(Icons.Outlined.Build, "Build", "Android native app · Kotlin and Jetpack Compose", theme) }
            item { SettingsHeader("CREATOR", theme) }
            item { SettingsRow(Icons.Outlined.Person, "Created by", "Aymen Gharsalli", theme) }
            item { SettingsRow(Icons.Outlined.Place, "Project origin", "Kasserine, Tunisia", theme) }
            item { Text("Independent project. Third-party model and component credits will be published with their applicable licenses before distribution.", color = theme.secondary, fontSize = 11.sp, lineHeight = 16.sp) }
        }
    }
}

@Composable
private fun ProjectsScreen(theme: MimoTheme, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val projects = listOf(
        Triple("GASS-JOBS / TUN-JOBS", "Job matching and recruitment for Tunisia", "https://github.com/Gass-OS/GASS-JOBS"),
        Triple("Kasserine City", "A 3D mobile world inspired by Kasserine", "https://github.com/Gass-OS/Kasserine-City"),
        Triple("Phantom Ops", "A mobile action game project", "https://github.com/Gass-OS/Phantom-Ops"),
        Triple("GassNative", "Linux-on-Android environment without root", "https://github.com/Gass-OS/GassNative-"),
        Triple("WINE-HUB", "Android project for Windows app compatibility workflows", "https://github.com/Gass-OS/WINE-HUB"),
        Triple("Long Night", "A story-driven Godot game project", "https://github.com/Gass-OS/long-night")
    )
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = theme.secondary) }
            Text("My Projects", fontSize = 23.sp, fontWeight = FontWeight.Bold)
        }
        Text("More work from the Gass-OS project space.", color = theme.secondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(projects) { project ->
                SettingsRow(Icons.Outlined.Code, project.first, project.second, theme) { onOpen(project.third) }
            }
        }
    }
}

@Composable
private fun SettingsHeader(label: String, theme: MimoTheme) {
    Text(label, Modifier.padding(top = 14.dp, bottom = 8.dp), color = theme.accent, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
}

@Composable
private fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, theme: MimoTheme, onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).background(theme.surface.copy(alpha = 0.72f)).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = theme.accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(subtitle, color = theme.secondary, fontSize = 11.sp)
        }
        if (onClick != null) Icon(Icons.Outlined.ChevronRight, null, tint = theme.secondary, modifier = Modifier.size(18.dp))
    }
}
