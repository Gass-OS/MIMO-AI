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
import android.speech.tts.TextToSpeech
import java.util.Locale
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Text as MaterialText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import android.app.Activity
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

private val LocalMimoArabic = androidx.compose.runtime.staticCompositionLocalOf { false }

@Composable
private fun Text(
    text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified, fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null, lineHeight: TextUnit = TextUnit.Unspecified,
    letterSpacing: TextUnit = TextUnit.Unspecified, maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip, style: TextStyle = LocalTextStyle.current
) {
    val arabic = LocalMimoArabic.current
    MaterialText(
        text = if (arabic) MimoArabic.translate(text) else text,
        modifier = modifier, color = color, fontSize = fontSize, fontWeight = fontWeight,
        textAlign = textAlign, lineHeight = lineHeight, letterSpacing = letterSpacing,
        maxLines = maxLines, overflow = overflow, style = style
    )
}

private object MimoArabic {
    private val phrases = mapOf(
        "One chat. Two creative minds. Your device." to "محادثة واحدة، وقدرات إبداعية، وعلى جهازك.",
        "Enter MIMO AI" to "ادخل إلى ميمو AI", "Your assistant is getting ready." to "مساعدك يستعدّ للعمل.",
        "Talk naturally" to "تحدّث بطبيعتك", "Ask questions, write, translate, learn and explore with your local AI assistant." to "اسأل، واكتب، وترجم، وتعلّم واكتشف مع مساعدك الذكي على الجهاز.",
        "Create images" to "أنشئ الصور", "Describe an image and MIMO will create it and place the result directly in your chat." to "صف الصورة التي تريدها وسيُنشئها ميمو ويعرضها مباشرة داخل المحادثة.",
        "Private and on-device" to "خصوصية ومعالجة على الجهاز", "After the one-time model setup, supported AI features are designed to work offline." to "بعد إعداد الملفات لأول مرة، ستعمل الميزات المدعومة دون إنترنت.",
        "One-time setup" to "إعداد لمرة واحدة", "The AI models are large, so the first launch will download and verify them with a progress bar." to "ملفات الذكاء الاصطناعي كبيرة؛ لذلك سيجري تنزيلها والتحقق منها عند التشغيل الأول مع عرض التقدّم.",
        "Your personal AI space" to "مساحتك الذكية الشخصية", "Chat, explore ideas or create something amazing." to "تحدّث، واكتشف أفكاراً، أو أنشئ شيئاً رائعاً.",
        "Explain something" to "فسّر لي موضوعاً", "Help me write" to "ساعدني في الكتابة", "Create an image" to "أنشئ صورة",
        "Message MIMO AI..." to "اكتب رسالة إلى ميمو...", "Preview interface · AI engines not connected yet" to "واجهة تجريبية · محركات الذكاء الاصطناعي لم تُربط بعد",
        "Settings" to "الإعدادات", "Choose your MIMO style" to "اختر مظهر ميمو", "Wi-Fi only downloads" to "التنزيل عبر Wi‑Fi فقط",
        "Avoid large model downloads on mobile data" to "تجنّب تنزيل الملفات الكبيرة عبر بيانات الهاتف", "Language" to "اللغة",
        "English · language selector planned" to "العربية (تونس) / English", "Arabic (Tunisia)" to "العربية (تونس)",
        "English" to "الإنجليزية", "Language engine" to "محرك اللغة", "Image engine" to "محرك الصور",
        "Model Manager" to "إدارة الملفات", "Manage setup and offline model files" to "إدارة الإعداد والملفات اللازمة للعمل دون إنترنت",
        "Device permissions" to "أذونات الجهاز", "About MIMO AI" to "حول ميمو AI", "My Projects" to "مشاريعي",
        "Prepare MIMO for offline use" to "جهّز ميمو للعمل دون إنترنت", "Not installed" to "غير مثبّت",
        "Ready to download inside MIMO" to "في انتظار مصدر تنزيل موثوق", "MIMO Brain" to "عقل ميمو",
        "MIMO Studio" to "استوديو ميمو", "READY" to "جاهز", "QUEUED" to "في الانتظار",
        "SETUP PROGRESS" to "تقدّم الإعداد", "FIRST SETUP · ABOUT 10 GB TOTAL" to "الإعداد الأول · حوالي 10 جيجابايت",
        "Continue to preview" to "المتابعة إلى المعاينة", "INITIALIZING YOUR PRIVATE AI" to "جارٍ تجهيز مساعدك الذكي الخاص",
        "Preparing MIMO Brain" to "جارٍ تجهيز عقل ميمو", "Your chat assistant is ready" to "مساعد المحادثة جاهز",
        "MIMO's main intelligence — the brain behind conversations, answers, writing, translation and everyday assistance." to "الذكاء الأساسي لميمو — العقل الذي يدعم المحادثات والإجابات والكتابة والترجمة والمساعدة اليومية.",
        "MIMO's creative studio — turns your text descriptions and ideas into original pictures." to "استوديو ميمو الإبداعي — يحوّل وصفك وأفكارك المكتوبة إلى صور أصلية.",
        "Chat engine verified" to "تم التحقق من محرك المحادثة", "Starts after MIMO Brain is ready" to "يبدأ بعد جاهزية عقل ميمو",
        "Actual verified progress" to "التقدّم الفعلي بعد التحقق", "Chat available · Studio continues" to "المحادثة متاحة · ويستمر الاستوديو",
        "MIMO downloads directly inside the app. Model files are verified before setup is marked complete." to "هذه الشاشة جاهزة لعرض تقدّم التنزيل الفعلي. خدمة التنزيل والتحقق من المحركات غير مفعّلة في هذا الإصدار التجريبي بعد.",
        "Your assistant is getting ready." to "مساعدك يستعدّ للعمل.",
        "Android speech engine is not ready. Check Text-to-speech settings." to "محرك النطق في أندرويد غير جاهز. تحقّق من إعدادات تحويل النص إلى كلام.",
        "Reference image selected" to "تم اختيار الصورة المرجعية",
        "Hi! I'm MIMO AI. Your offline assistant is ready to speak." to "مرحباً! أنا ميمو AI، مساعدك الذي يستعدّ للتحدّث دون إنترنت.",
        "Explain something interesting to me" to "اشرح لي موضوعاً مثيراً للاهتمام",
        "Help me write something" to "ساعدني في كتابة شيء",
        "Create an image: " to "أنشئ صورة: ",
        "Read aloud" to "اقرأ بصوت مرتفع", "Image generation" to "إنشاء الصور", "Send" to "إرسال", "Back" to "رجوع",
        "APPEARANCE" to "المظهر", "AI & MODELS" to "الذكاء الاصطناعي والملفات", "DEVICE & PERMISSIONS" to "الجهاز والأذونات",
        "Location and GPS" to "الموقع وGPS", "Permission controls and nearby-place support" to "إدارة الأذونات ودعم الأماكن القريبة",
        "Notifications" to "الإشعارات", "Choose whether MIMO may notify you" to "اختر ما إذا كان بإمكان ميمو إرسال إشعارات",
        "Reference pictures" to "الصور المرجعية", "Select a picture with Android Photo Picker" to "اختر صورة باستخدام منتقي الصور في أندرويد",
        "App permissions" to "أذونات التطبيق", "Review Android permission settings" to "راجع إعدادات أذونات أندرويد",
        "PREFERENCES" to "التفضيلات", "PERFORMANCE PROFILE" to "ملف الأداء", "TEXT TO SPEECH" to "تحويل النص إلى كلام",
        "MIMO speaks replies" to "ميمو ينطق الردود", "Use the Android installed speech engine" to "استخدم محرك النطق المثبّت في أندرويد",
        "Auto-read assistant replies" to "قراءة ردود المساعد تلقائياً", "Speak new assistant messages automatically" to "انطق رسائل المساعد الجديدة تلقائياً",
        "Test MIMO voice" to "اختبر صوت ميمو", "ABOUT & PROJECTS" to "حول ميمو والمشاريع", "Purpose, privacy and creator" to "الهدف والخصوصية وصاحب المشروع",
        "About this APK" to "حول هذا التطبيق", "Explore apps and open-source work" to "اكتشف التطبيقات والمشاريع مفتوحة المصدر",
        "SETUP CHECKLIST" to "قائمة التحقق من الإعداد", "Source and license" to "المصدر والترخيص",
        "Must be checked before distributing model files" to "يجب التحقق منه قبل توزيع ملفات النماذج",
        "File integrity" to "سلامة الملفات", "SHA-256 verification will be required" to "سيكون التحقق باستخدام SHA-256 ضرورياً",
        "Resumable downloads" to "تنزيلات قابلة للاستئناف", "Not enabled until secure sources are configured" to "لن تتفعّل حتى إعداد مصادر آمنة",
        "Device storage" to "مساحة تخزين الجهاز", "Storage check will run before each download" to "سيتم فحص المساحة قبل كل تنزيل",
        "Location while using MIMO" to "الموقع أثناء استخدام ميمو", "Allowed" to "مسموح", "Not allowed" to "غير مسموح",
        "Permission granted" to "تم منح الإذن", "Allow location" to "السماح بالوصول إلى الموقع",
        "Allow notifications" to "السماح بالإشعارات", "Reference picture" to "صورة مرجعية",
        "Picture selected" to "تم اختيار صورة", "No picture selected" to "لم يتم اختيار صورة",
        "Choose a picture" to "اختر صورة", "Android app settings" to "إعدادات تطبيق أندرويد",
        "Manage all permissions and notifications" to "إدارة جميع الأذونات والإشعارات",
        "Your assistant. Your device. Your control." to "مساعدك. جهازك. وتحكّمك.",
        "ABOUT THE ASSISTANT" to "حول المساعد", "One conversation" to "محادثة واحدة",
        "Chat and image creation are designed to live in the same conversation." to "المحادثة وإنشاء الصور يجتمعان في محادثة واحدة.",
        "Offline-first" to "مصمم للعمل دون إنترنت", "Privacy" to "الخصوصية", "ABOUT THIS APK" to "حول التطبيق",
        "Application" to "التطبيق", "Version" to "الإصدار", "Build" to "البناء",
        "CREATOR" to "صاحب المشروع", "Created by" to "أنشئ بواسطة", "Project origin" to "منشأ المشروع",
        "More work from the Gass-OS project space." to "المزيد من أعمال ومشاريع Gass-OS.",
        "Tip · You can ask MIMO to explain, translate, summarize and help you write." to "نصيحة · يمكنك طلب الشرح والترجمة والتلخيص والمساعدة في الكتابة من ميمو.",
        "Privacy · After setup, supported AI processing is designed to happen on your device." to "الخصوصية · بعد الإعداد، ستتم معالجة ميزات الذكاء الاصطناعي المدعومة على جهازك.",
        "Good to know · Keep MIMO open and connected to Wi-Fi during the first download." to "معلومة · اترك ميمو مفتوحاً ومتّصلاً بالـ Wi‑Fi أثناء التنزيل الأول.",
        "Smart setup · MIMO prepares chat first, then continues the creative tools in the background." to "إعداد ذكي · يجهّز ميمو المحادثة أولاً ثم يواصل تجهيز أدوات الإبداع في الخلفية.",
        "You can start chatting while Studio finishes in the background." to "يمكنك بدء المحادثة بينما يكتمل تجهيز الاستوديو في الخلفية.",
        "Setting up the language engine first. Image tools will follow automatically." to "يتم إعداد محرك اللغة أولاً، ثم تُجهّز أدوات الصور تلقائياً.",
        "Waiting for the verified download service" to "في انتظار خدمة التنزيل والتحقق",
        "Image creation is preparing in the background." to "يجري تجهيز إنشاء الصور في الخلفية.",
        "Creative tools verified" to "تم التحقق من الأدوات الإبداعية",
        "Low / Eco" to "منخفض / اقتصادي", "Save battery, heat and memory" to "توفير البطارية وتقليل الحرارة واستهلاك الذاكرة",
        "Medium / Balanced" to "متوسط / متوازن", "Recommended for most phones" to "موصى به لمعظم الهواتف",
        "High / Performance" to "مرتفع / أداء", "Use more available CPU/GPU resources" to "استخدام المزيد من موارد المعالج والرسوميات",
        "Blue Dark" to "أزرق داكن", "Golden Lux" to "ذهبي فاخر", "Blood Red" to "أحمر داكن", "Pure Black" to "أسود خالص",
        "Hi 👋 I'm MIMO AI, your offline AI assistant. How can I help you today?" to "أهلاً 👋 أنا ميمو AI، مساعدك الذكي. كيف يمكنني مساعدتك اليوم؟",
        "Hi 👋 I'm MIMO AI. Tell me what you'd like me to create. Your image will appear here in our chat." to "أهلاً 👋 أنا ميمو AI. أخبرني بالصورة التي تريد إنشاءها وستظهر هنا في محادثتنا.",
        "Thanks for your message! I'm still being connected to my on-device AI engine. This preview can show the interface, but real AI replies aren't available yet." to "شكراً على رسالتك! ما زلنا نربط محرك الذكاء الاصطناعي على الجهاز. هذه معاينة للواجهة، والردود الذكية الفعلية غير متاحة بعد.",
        "I'm getting my creative tools ready. Image creation isn't available in this preview yet." to "أجهّز أدواتي الإبداعية. إنشاء الصور غير متاح في هذه المعاينة بعد.",
        "Large downloads require your approval. Wi-Fi only is enabled by default." to "التنزيلات كبيرة وتتطلب موافقتك. التنزيل عبر Wi‑Fi فقط مفعّل افتراضياً.",
        "At least 3 GB of free internal app storage is required before starting MIMO Brain." to "يلزم توفير 15 جيجابايت على الأقل قبل بدء التنزيل.",
        "At least 3 GB of free internal app storage is required. Free up space and try again." to "يلزم توفير 15 جيجابايت على الأقل. أفرغ بعض المساحة ثم أعد المحاولة.",
        "Downloading MIMO Brain…" to "جارٍ تنزيل عقل ميمو…",
        "MIMO Brain · " to "عقل ميمو · ",
        "MIMO Brain download failed. Check your connection and try again." to "فشل تنزيل عقل ميمو. تحقّق من الاتصال ثم أعد المحاولة.",
        "MIMO Brain file verified. Downloading MIMO Studio…" to "تم التحقق من ملف عقل ميمو. جارٍ تنزيل استوديو ميمو…",
        "MIMO Studio · " to "استوديو ميمو · ",
        "MIMO Studio download failed. MIMO Brain remains safely verified; retry to continue." to "فشل تنزيل استوديو ميمو. ملف عقل ميمو محفوظ وموثّق؛ أعد المحاولة للمتابعة.",
        "Both model files passed SHA-256 verification. AI runtime integration is the next step." to "اجتاز الملفان التحقق من السلامة. الخطوة التالية هي ربط محركات الذكاء الاصطناعي.",
        "Download setup error: " to "حدث خطأ في إعداد التنزيل: ",
        "MIMO Brain file verified" to "تم التحقق من ملف عقل ميمو",
        "The Brain file is verified. The on-device chat runtime still needs integration." to "تم التحقق من ملف العقل. ما زلنا بحاجة إلى ربط محرك المحادثة على الجهاز.",
        "Approve the one-time download to install the verified MIMO components." to "وافق على التنزيل لمرة واحدة لتثبيت ملفات ميمو والتحقق منها.",
        "Model file verified · chat runtime integration pending" to "تم التحقق من ملف النموذج · ربط محرك المحادثة ما زال مطلوباً",
        "Ready when you approve the download" to "جاهز عند موافقتك على التنزيل",
        "VERIFIED" to "تم التحقق", "MIMO Studio download is next" to "تنزيل استوديو ميمو هو التالي",
        "Model file verified · image runtime integration pending" to "تم التحقق من ملف النموذج · ربط محرك الصور ما زال مطلوباً",
        "Starts after MIMO Brain is verified" to "يبدأ بعد التحقق من عقل ميمو",
        "Resume / retry downloads" to "استئناف / إعادة التنزيل", "Start downloads · about 9.5 GB" to "بدء التنزيل · حوالي 9.5 جيجابايت",
        "Downloads run inside MIMO into private internal app storage, resume partial transfers when you retry, and are SHA-256 verified before being marked complete. This installs model files only; AI engines are a separate integration step." to "يستخدم التنزيل Wi‑Fi افتراضياً، ويمكن استئنافه عبر مدير تنزيل أندرويد. يتم التحقق من الملفات قبل اعتمادها. هذه الخطوة تثبّت الملفات فقط؛ أما محركات الذكاء الاصطناعي فتحتاج إلى ربط منفصل.",
        "Welcome to MIMO AI" to "مرحباً بك في ميمو AI",
        "One assistant. Two creative tools." to "مساعد واحد وأداتان للإبداع",
        "Your privacy matters" to "خصوصيتك تهمّنا",
        "Permissions and setup" to "الأذونات والإعداد",
        "Your personal AI space for conversations, learning, writing and creative ideas." to "مساحتك الذكية للمحادثة والتعلّم والكتابة والأفكار الإبداعية.",
        "MIMO Brain is planned for chat, answers and writing. MIMO Studio is planned for image creation. Both model files are downloaded only after your approval." to "يُخصّص عقل ميمو للمحادثة والإجابات والكتابة، واستوديو ميمو لإنشاء الصور. لن يتم تنزيل أي ملف قبل موافقتك.",
        "The model files come from their official public model repositories. Downloading them does not send your chat messages to those repositories. Offline AI features still require their runtime engines to be integrated." to "تأتي ملفات النماذج من مستودعاتها الرسمية والعامة. تنزيلها لا يرسل محادثاتك إلى تلك المستودعات. كما أن تشغيل الذكاء الاصطناعي دون إنترنت يتطلب دمج محركات التشغيل.",
        "Internet access is needed for the one-time model download. Notifications are optional. Photos, microphone, camera and location are requested only if you choose a feature that needs them." to "يلزم اتصال الإنترنت لتنزيل النماذج لمرة واحدة. الإشعارات اختيارية، ولن نطلب الصور أو الميكروفون أو الكاميرا أو الموقع إلا عند استخدام ميزة تحتاج إليها.",
        "I understand and agree to continue with setup." to "أفهم المعلومات وأوافق على متابعة الإعداد.",
        "Continue" to "متابعة",
        "Accept & continue" to "أوافق وأتابع",
        "Review & download · about 9.5 GB" to "مراجعة وتنزيل · حوالي 9.5 جيجابايت",
        "Download MIMO models?" to "تنزيل نماذج ميمو؟",
        "The first setup downloads about 9.5 GB in total:" to "يتطلب الإعداد الأول تنزيل حوالي 9.5 جيجابايت:",
        "• MIMO Brain · Qwen3 4B Q4_K_M · about 2.5 GB" to "• عقل ميمو · Qwen3 4B Q4_K_M · حوالي 2.5 جيجابايت",
        "• MIMO Studio · SDXL 1.0 · about 6.94 GB" to "• استوديو ميمو · SDXL 1.0 · حوالي 6.94 جيجابايت",
        "Files are downloaded one after another and checked with SHA-256 before being marked verified. Keep at least 3 GB free for Brain; MIMO Studio checks its larger storage requirement before downloading." to "سيتم تنزيل الملفات بالتتابع والتحقق منها باستخدام SHA-256 قبل اعتمادها. يرجى توفير 15 جيجابايت على الأقل.",
        "Wi-Fi only is ON. Downloads will wait for Wi-Fi." to "التنزيل عبر Wi‑Fi فقط مفعّل. سينتظر التنزيل توفر شبكة Wi‑Fi.",
        "Mobile data is allowed. Your carrier may charge data fees." to "بيانات الهاتف مسموحة. قد تُحتسب رسوم من شركة الاتصالات.",
        "I approve downloading these large model files to this device." to "أوافق على تنزيل ملفات النماذج الكبيرة إلى هذا الجهاز.",
        "Accept & start download" to "أوافق وابدأ التنزيل",
        "Not now" to "ليس الآن"

    )
    fun translate(value: String): String = phrases[value] ?: when {
        value.startsWith("MIMO Brain · ") -> "عقل ميمو · " + value.substringAfter("MIMO Brain · ")
        value.startsWith("MIMO Studio · ") -> "استوديو ميمو · " + value.substringAfter("MIMO Studio · ")
        value.startsWith("Download setup error: ") -> "حدث خطأ في إعداد التنزيل: " + value.substringAfter("Download setup error: ")
        else -> value
    }
}

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

private data class MimoPerformanceProfile(
    val id: String, val title: String, val description: String,
    val cpuThreads: Int, val gpuOffloadPercent: Int, val imageSize: Int
)

private object MimoRuntimeConfig {
    val profiles = listOf(
        MimoPerformanceProfile("low", "Low / Eco", "Save battery, heat and memory", 2, 0, 512),
        MimoPerformanceProfile("medium", "Medium / Balanced", "Recommended for most phones", 4, 35, 640),
        MimoPerformanceProfile("high", "High / Performance", "Use more available CPU/GPU resources", 8, 70, 768)
    )
    @Volatile var selected: MimoPerformanceProfile = profiles[1]
}

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
    var showLanguagePicker by remember { mutableStateOf(!prefs.getBoolean("language_selected", false)) }
    var language by remember { mutableStateOf(prefs.getString("language", "ar-TN") ?: "ar-TN") }
    var page by remember { mutableStateOf("setup") }
    var locationGranted by remember {
        mutableStateOf(context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    var notificationsGranted by remember {
        mutableStateOf(Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
    }
    var selectedPhoto by remember { mutableStateOf(prefs.getString("reference_photo_uri", null)) }
    var wifiOnly by remember { mutableStateOf(prefs.getBoolean("wifi_only", true)) }
    var performanceId by remember { mutableStateOf(prefs.getString("performance_profile", "medium") ?: "medium") }
    var speechEnabled by remember { mutableStateOf(prefs.getBoolean("speech_enabled", true)) }
    var autoSpeak by remember { mutableStateOf(prefs.getBoolean("auto_speak", false)) }
    var speechRate by remember { mutableFloatStateOf(prefs.getFloat("speech_rate", 1.0f)) }
    val installer = remember(context) { ModelInstaller(context) }
    var downloadStarted by remember { mutableStateOf(prefs.getBoolean("model_download_started", false)) }
    var downloadAttempt by remember { mutableIntStateOf(0) }
    var showExitDialog by remember { mutableStateOf(false) }
    var brainProgress by remember { mutableFloatStateOf(0f) }
    var studioProgress by remember { mutableFloatStateOf(0f) }
    var brainDownloadedBytes by remember { mutableLongStateOf(0L) }
    var brainTotalBytes by remember { mutableLongStateOf(-1L) }
    var studioDownloadedBytes by remember { mutableLongStateOf(0L) }
    var studioTotalBytes by remember { mutableLongStateOf(-1L) }
    var brainVerified by remember { mutableStateOf(installer.isVerified(ModelInstaller.Model.BRAIN)) }
    var studioVerified by remember { mutableStateOf(installer.isVerified(ModelInstaller.Model.STUDIO)) }
    var installerStatus by remember { mutableStateOf("Large downloads require your approval. Wi-Fi only is enabled by default.") }
    var speechReady by remember { mutableStateOf(false) }
    val tts = remember(context) {
        TextToSpeech(context) { status -> speechReady = status == TextToSpeech.SUCCESS }
    }
    DisposableEffect(tts) {
        onDispose { tts.stop(); tts.shutdown() }
    }
    val speakText: (String) -> Unit = { spoken ->
        if (speechEnabled && speechReady) {
            tts.language = Locale.getDefault()
            tts.setSpeechRate(speechRate)
            tts.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, "mimo-${System.currentTimeMillis()}")
        } else if (!speechReady) {
            Toast.makeText(context, "Android speech engine is not ready. Check Text-to-speech settings.", Toast.LENGTH_SHORT).show()
        }
    }
    MimoRuntimeConfig.selected = MimoRuntimeConfig.profiles.firstOrNull { it.id == performanceId } ?: MimoRuntimeConfig.profiles[1]
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

    LaunchedEffect(downloadStarted, downloadAttempt) {
        if (downloadStarted) {
            try {
                if (!installer.hasRequiredSpace()) {
                    installerStatus = "At least 3 GB of free internal app storage is required before starting MIMO Brain."
                    return@LaunchedEffect
                }
                if (!brainVerified) {
                    installerStatus = "Downloading MIMO Brain…"
                    brainVerified = installer.downloadAndVerify(ModelInstaller.Model.BRAIN, wifiOnly) { transfer ->
                        if (transfer != null) {
                            brainDownloadedBytes = transfer.downloadedBytes
                            brainTotalBytes = transfer.totalBytes
                            if (transfer.totalBytes > 0L) {
                                brainProgress = transfer.progress
                                installerStatus = "MIMO Brain · ${String.format(Locale.US, "%.2f", transfer.progress * 100.0)}% · ${formatModelBytes(transfer.downloadedBytes)} / ${formatModelBytes(transfer.totalBytes)}"
                            } else {
                                installerStatus = when (transfer.status) {
                                    android.app.DownloadManager.STATUS_PENDING -> "MIMO Brain · " + (installer.describeReason(transfer).ifBlank { "Queued; waiting for Android to start…" })
                                    android.app.DownloadManager.STATUS_PAUSED -> "MIMO Brain · " + (installer.describeReason(transfer).ifBlank { "Paused; waiting to resume…" })
                                    android.app.DownloadManager.STATUS_RUNNING -> "MIMO Brain · Connected; preparing download…"
                                    else -> "MIMO Brain · Checking download…"
                                }
                            }
                        }
                    }
                    if (!brainVerified) {
                        val failed = installer.query(installer.savedDownloadId(ModelInstaller.Model.BRAIN))
                        installerStatus = if (failed?.status == android.app.DownloadManager.STATUS_FAILED) {
                            "MIMO Brain download failed: ${installer.describeReason(failed)} (Android code ${failed.reason})."
                        } else "MIMO Brain download failed. Check the connection and retry."
                        return@LaunchedEffect
                    }
                }
                brainProgress = 1f
                if (!studioVerified) {
                    installerStatus = "MIMO Brain file verified. Downloading MIMO Studio…"
                    studioVerified = installer.downloadAndVerify(ModelInstaller.Model.STUDIO, wifiOnly) { transfer ->
                        if (transfer != null) {
                            studioDownloadedBytes = transfer.downloadedBytes
                            studioTotalBytes = transfer.totalBytes
                            if (transfer.totalBytes > 0L) {
                                studioProgress = transfer.progress
                                installerStatus = "MIMO Studio · ${String.format(Locale.US, "%.2f", transfer.progress * 100.0)}% · ${formatModelBytes(transfer.downloadedBytes)} / ${formatModelBytes(transfer.totalBytes)}"
                            } else {
                                installerStatus = when (transfer.status) {
                                    android.app.DownloadManager.STATUS_PENDING -> "MIMO Studio · " + (installer.describeReason(transfer).ifBlank { "Queued; waiting for Android to start…" })
                                    android.app.DownloadManager.STATUS_PAUSED -> "MIMO Studio · " + (installer.describeReason(transfer).ifBlank { "Paused; waiting to resume…" })
                                    android.app.DownloadManager.STATUS_RUNNING -> "MIMO Studio · Connected; preparing download…"
                                    else -> "MIMO Studio · Checking download…"
                                }
                            }
                        }
                    }
                    if (!studioVerified) {
                        val failed = installer.query(installer.savedDownloadId(ModelInstaller.Model.STUDIO))
                        installerStatus = if (failed?.status == android.app.DownloadManager.STATUS_FAILED) {
                            "MIMO Studio download failed: ${installer.describeReason(failed)} (Android code ${failed.reason}). Brain remains verified."
                        } else "MIMO Studio download failed. MIMO Brain remains verified; retry to continue."
                        return@LaunchedEffect
                    }
                }
                studioProgress = 1f
                installerStatus = "Both model files passed SHA-256 verification. AI runtime integration is the next step."
            } catch (error: Exception) {
                installerStatus = "Download setup error: ${error.message ?: "unknown error"}"
            }
        }
    }

    BackHandler(enabled = true) {
        when {
            showExitDialog -> showExitDialog = false
            page == "chat" -> page = "setup"
            page != "setup" -> page = "chat"
            showWelcome || showLanguagePicker -> showExitDialog = true
            else -> showExitDialog = true
        }
    }
    CompositionLocalProvider(
        LocalMimoArabic provides language.startsWith("ar"),
        LocalLayoutDirection provides if (language.startsWith("ar")) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
    MaterialTheme(colorScheme = darkColorScheme(
        background = theme.background, surface = theme.surface,
        primary = theme.accent, onPrimary = Color(0xFF080B10),
        onBackground = Color(0xFFF4F6FA), onSurface = Color(0xFFF4F6FA),
        secondary = theme.secondary
    )) {
        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(theme.surface.copy(alpha = 0.72f), theme.background, theme.background))
        )) {
            if (showExitDialog) {
                AlertDialog(
                    onDismissRequest = { showExitDialog = false },
                    title = { Text("Exit MIMO AI", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = { Text("Are you sure you want to close MIMO AI?", color = theme.secondary) },
                    confirmButton = {
                        TextButton(onClick = { (context as? Activity)?.finishAndRemoveTask() }) {
                            Text("Exit", color = theme.accent, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExitDialog = false }) { Text("Stay", color = theme.secondary) }
                    },
                    containerColor = theme.surface
                )
            }
            if (showLanguagePicker) {
                FirstRunLanguageScreen(theme = theme, selected = language, onSelect = { selectedLanguage ->
                    language = selectedLanguage
                    prefs.edit().putString("language", selectedLanguage).putBoolean("language_selected", true).apply()
                    tts.language = when (selectedLanguage) {
                        "ar-TN" -> Locale("ar", "TN")
                        "fr" -> Locale.FRENCH
                        else -> Locale.ENGLISH
                    }
                    showLanguagePicker = false
                })
            } else if (showWelcome) {
                WelcomeScreen(theme, onStart = {
                    prefs.edit().putBoolean("welcome_seen", true).apply()
                    showWelcome = false
                    page = "setup"
                })
            } else when (page) {
                "settings" -> SettingsScreen(theme, themeId, wifiOnly, performanceId, speechEnabled, autoSpeak, speechRate,
                    onWifiOnly = {
                    wifiOnly = it
                    prefs.edit().putBoolean("wifi_only", it).apply()
                }, onPerformance = {
                    performanceId = it
                    prefs.edit().putString("performance_profile", it).apply()
                }, onSpeechEnabled = {
                    speechEnabled = it
                    prefs.edit().putBoolean("speech_enabled", it).apply()
                }, onAutoSpeak = {
                    autoSpeak = it
                    prefs.edit().putBoolean("auto_speak", it).apply()
                }, onSpeechRate = {
                    speechRate = it
                    prefs.edit().putFloat("speech_rate", it).apply()
                }, onTestSpeech = { speakText("Hi! I'm MIMO AI. Your offline assistant is ready to speak.") },
                    onTheme = {
                    themeId = it
                    prefs.edit().putString("theme", it).apply()
                }, language = language, onLanguage = {
                    language = it
                    prefs.edit().putString("language", it).apply()
                    val locale = if (it.startsWith("ar")) Locale("ar", "TN") else Locale.ENGLISH
                    tts.language = locale
                }, onBack = { page = "chat" }, onManageModels = { page = "models" },
                    onPermissions = { page = "permissions" }, onAbout = { page = "about" }, onProjects = { page = "projects" })
                "setup" -> SetupBootScreen(
                    theme = theme,
                    brainProgress = brainProgress,
                    studioProgress = studioProgress,
                    brainDownloadedBytes = brainDownloadedBytes,
                    brainTotalBytes = brainTotalBytes,
                    studioDownloadedBytes = studioDownloadedBytes,
                    studioTotalBytes = studioTotalBytes,
                    brainReady = brainVerified,
                    studioReady = studioVerified,
                    downloadStarted = downloadStarted,
                    statusMessage = installerStatus,
                    language = language,
                    wifiOnly = wifiOnly,
                    onWifiOnly = { value ->
                        if (wifiOnly != value) {
                            wifiOnly = value
                            prefs.edit().putBoolean("wifi_only", value).apply()
                            if (downloadStarted) {
                                installer.resetTransferForNetworkChange(ModelInstaller.Model.BRAIN)
                                installer.resetTransferForNetworkChange(ModelInstaller.Model.STUDIO)
                                downloadAttempt++
                                installerStatus = if (value) "Switching to Wi-Fi-only download…" else "Allowing mobile data; restarting the pending transfer…"
                            }
                        }
                    },
                    onLanguage = {
                        language = it
                        prefs.edit().putString("language", it).apply()
                        tts.language = when (it) {
                            "ar-TN" -> Locale("ar", "TN")
                            "fr" -> Locale.FRENCH
                            else -> Locale.ENGLISH
                        }
                    },
                    onStartDownloads = {
                        if (!installer.hasRequiredSpace()) {
                            installerStatus = "At least 3 GB of free internal app storage is required. Free up space and try again."
                        } else {
                            prefs.edit().putBoolean("model_download_started", true).apply()
                            downloadStarted = true
                            downloadAttempt++
                        }
                    },
                    onContinuePreview = { page = "chat" }
                )
                "models" -> ModelManagerScreen(theme, onBack = { page = "settings" })
                "permissions" -> PermissionsScreen(theme, locationGranted, notificationsGranted, selectedPhoto != null,
                    onBack = { page = "settings" },
                    onLocation = { locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)) },
                    onNotifications = {
                        if (Build.VERSION.SDK_INT >= 33) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else notificationsGranted = true
                    },
                    onPhoto = { photoLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onAppSettings = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) })
                "about" -> AboutScreen(theme, onBack = { page = "settings" })
                "projects" -> ProjectsScreen(theme, onBack = { page = "settings" }, onOpen = { url ->
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                    catch (_: Exception) { Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show() }
                })
                else -> ChatScreen(theme, prefs = prefs, onSettings = { page = "settings" }, onSpeak = speakText, speechEnabled = speechEnabled, autoSpeak = autoSpeak, downloadStarted = downloadStarted, brainProgress = brainProgress, studioProgress = studioProgress, brainDownloadedBytes = brainDownloadedBytes, brainTotalBytes = brainTotalBytes, studioDownloadedBytes = studioDownloadedBytes, studioTotalBytes = studioTotalBytes, brainVerified = brainVerified, studioVerified = studioVerified, downloadStatus = installerStatus)
            }
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
        Canvas(Modifier.size((size * 0.72f).dp)) {
            val c = theme.accent
            val w = this.size.width
            val h = this.size.height
            // Antenna and signal tip
            drawLine(c, androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.17f),
                androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.04f), strokeWidth = w * 0.055f)
            drawCircle(c, radius = w * 0.065f, center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.035f))
            // Side ear modules
            drawRoundRect(c, topLeft = androidx.compose.ui.geometry.Offset(w * 0.04f, h * 0.43f),
                size = androidx.compose.ui.geometry.Size(w * 0.17f, h * 0.28f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.07f))
            drawRoundRect(c, topLeft = androidx.compose.ui.geometry.Offset(w * 0.79f, h * 0.43f),
                size = androidx.compose.ui.geometry.Size(w * 0.17f, h * 0.28f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.07f))
            // Friendly robot head
            drawRoundRect(c, topLeft = androidx.compose.ui.geometry.Offset(w * 0.16f, h * 0.18f),
                size = androidx.compose.ui.geometry.Size(w * 0.68f, h * 0.66f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.19f))
            // Dark face panel
            drawRoundRect(Color(0xFF071426), topLeft = androidx.compose.ui.geometry.Offset(w * 0.23f, h * 0.27f),
                size = androidx.compose.ui.geometry.Size(w * 0.54f, h * 0.43f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.13f))
            // Bright eyes and a friendly smile
            drawCircle(Color.White, radius = w * 0.055f, center = androidx.compose.ui.geometry.Offset(w * 0.39f, h * 0.43f))
            drawCircle(Color.White, radius = w * 0.055f, center = androidx.compose.ui.geometry.Offset(w * 0.61f, h * 0.43f))
            drawLine(c, androidx.compose.ui.geometry.Offset(w * 0.40f, h * 0.57f),
                androidx.compose.ui.geometry.Offset(w * 0.60f, h * 0.57f), strokeWidth = w * 0.045f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
    }
}

@Composable
private fun FirstRunLanguageScreen(theme: MimoTheme, selected: String, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        BrandMark(theme, 82)
        Spacer(Modifier.height(20.dp))
        Text("MIMO AI", color = theme.accent, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
        Spacer(Modifier.height(10.dp))
        Text("Choose your language", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("اختر لغتك  ·  Choisissez votre langue", color = theme.secondary, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(26.dp))
        data class LanguageChoice(val name: String, val code: String, val tag: String, val subtitle: String)
        listOf(
            LanguageChoice("العربية · تونس", "ar-TN", "RTL", "واجهة عربية كاملة واتجاه من اليمين إلى اليسار"),
            LanguageChoice("Français", "fr", "FR", "Interface en français · navigation et conseils"),
            LanguageChoice("English", "en", "EN", "Full English interface · navigation and setup tips")
        ).forEach { item ->
            val active = selected == item.code
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(16.dp))
                .background(theme.surface).border(if (active) 2.dp else 1.dp,
                    if (active) theme.accent else Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                .clickable { onSelect(item.code) }.padding(horizontal = 18.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(item.subtitle, color = theme.secondary, fontSize = 10.sp, lineHeight = 14.sp)
                }
                Text(item.tag, color = theme.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                if (active) { Spacer(Modifier.width(8.dp)); Icon(Icons.Outlined.CheckCircle, null, tint = theme.accent) }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("You can change this later in Settings.", color = theme.secondary, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

private fun formatModelBytes(bytes: Long): String {
    if (bytes < 0L) return "Unknown"
    val gb = bytes / 1_000_000_000.0
    return if (gb >= 1.0) String.format(Locale.US, "%.2f GB", gb)
    else String.format(Locale.US, "%.0f MB", bytes / 1_000_000.0)
}

@Composable
private fun WelcomeScreen(theme: MimoTheme, onStart: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var accepted by remember { mutableStateOf(false) }
    val titles = listOf(
        "Welcome to MIMO AI",
        "One assistant. Two creative tools.",
        "Your privacy matters",
        "Permissions and setup"
    )
    val descriptions = listOf(
        "Your personal AI space for conversations, learning, writing and creative ideas.",
        "MIMO Brain is planned for chat, answers and writing. MIMO Studio is planned for image creation. Both model files are downloaded only after your approval.",
        "The model files come from their official public model repositories. Downloading them does not send your chat messages to those repositories. Offline AI features still require their runtime engines to be integrated.",
        "Internet access is needed for the one-time model download. Notifications are optional. Photos, microphone, camera and location are requested only if you choose a feature that needs them."
    )
    val icons = listOf(
        Icons.Outlined.Forum,
        Icons.Outlined.AutoAwesome,
        Icons.Outlined.Lock,
        Icons.Outlined.Settings
    )
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BrandMark(theme, 82)
        Spacer(Modifier.height(16.dp))
        Text("MIMO AI", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = theme.accent, letterSpacing = 2.sp)
        Text("WELCOME · ${step + 1} / 4", color = theme.secondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(4) { index ->
                Box(
                    Modifier.padding(horizontal = 4.dp).size(if (index == step) 22.dp else 7.dp, 7.dp)
                        .clip(CircleShape).background(if (index <= step) theme.accent else theme.secondary.copy(alpha = 0.35f))
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = theme.surface.copy(alpha = 0.9f),
            shape = RoundedCornerShape(22.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, theme.accent.copy(alpha = 0.22f))
        ) {
            Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icons[step], null, tint = theme.accent, modifier = Modifier.size(42.dp))
                Spacer(Modifier.height(16.dp))
                Text(titles[step], color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(descriptions[step], color = theme.secondary, fontSize = 13.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
                if (step == 3) {
                    Spacer(Modifier.height(18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = accepted, onCheckedChange = { accepted = it })
                        Text("I understand and agree to continue with setup.", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { if (step < 3) step++ else onStart() },
            enabled = step < 3 || accepted,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(if (step < 3) "Continue" else "Accept & continue", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Outlined.ArrowForward, null)
        }
        if (step > 0) {
            TextButton(onClick = { step--; accepted = false }) {
                Text("Back", color = theme.secondary)
            }
        }
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
private fun ChatScreen(
    theme: MimoTheme,
    prefs: android.content.SharedPreferences,
    onSettings: () -> Unit,
    onSpeak: (String) -> Unit,
    speechEnabled: Boolean,
    autoSpeak: Boolean,
    downloadStarted: Boolean,
    brainProgress: Float,
    studioProgress: Float,
    brainDownloadedBytes: Long,
    brainTotalBytes: Long,
    studioDownloadedBytes: Long,
    studioTotalBytes: Long,
    brainVerified: Boolean,
    studioVerified: Boolean,
    downloadStatus: String
) {
    val messages = remember {
        mutableStateListOf<ChatMessage>().apply {
            val saved = prefs.getString("active_chat_messages", null)
            if (!saved.isNullOrBlank()) {
                try {
                    val array = org.json.JSONArray(saved)
                    for (index in 0 until array.length()) {
                        val item = array.getJSONObject(index)
                        add(ChatMessage(item.optString("text"), item.optBoolean("fromUser")))
                    }
                } catch (_: Exception) { clear() }
            }
        }
    }
    SideEffect {
        val array = org.json.JSONArray()
        messages.forEach { message ->
            array.put(org.json.JSONObject().put("text", message.text).put("fromUser", message.fromUser))
        }
        prefs.edit().putString("active_chat_messages", array.toString()).apply()
    }
    var input by remember { mutableStateOf("") }
    var historyOpen by remember { mutableStateOf(false) }
    var savedChats by remember {
        mutableStateOf(
            try { org.json.JSONArray(prefs.getString("saved_chat_library", "[]")) }
            catch (_: Exception) { org.json.JSONArray() }
        )
    }
    fun encodeMessages(source: List<ChatMessage>): org.json.JSONArray {
        val array = org.json.JSONArray()
        source.forEach { array.put(org.json.JSONObject().put("text", it.text).put("fromUser", it.fromUser)) }
        return array
    }
    fun archiveCurrentChat() {
        if (messages.isEmpty()) return
        val firstUserText = messages.firstOrNull { it.fromUser }?.text ?: "New conversation"
        val title = firstUserText.take(42).ifBlank { "New conversation" }
        val archive = org.json.JSONObject()
            .put("id", System.currentTimeMillis().toString())
            .put("title", title)
            .put("savedAt", System.currentTimeMillis())
            .put("messages", encodeMessages(messages))
        val updated = org.json.JSONArray().put(archive)
        for (i in 0 until savedChats.length()) updated.put(savedChats.getJSONObject(i))
        while (updated.length() > 50) updated.remove(updated.length() - 1)
        savedChats = updated
        prefs.edit().putString("saved_chat_library", updated.toString()).putString("active_chat_messages", "[]").apply()
    }
    fun loadSavedChat(index: Int) {
        val chosen = savedChats.optJSONObject(index) ?: return
        val chosenId = chosen.optString("id")
        val chosenMessages = chosen.optJSONArray("messages") ?: org.json.JSONArray()
        archiveCurrentChat()
        messages.clear()
        for (i in 0 until chosenMessages.length()) {
            val item = chosenMessages.optJSONObject(i) ?: continue
            messages.add(ChatMessage(item.optString("text"), item.optBoolean("fromUser")))
        }
        val updated = org.json.JSONArray()
        for (i in 0 until savedChats.length()) {
            val item = savedChats.optJSONObject(i) ?: continue
            if (item.optString("id") != chosenId) updated.put(item)
        }
        savedChats = updated
        prefs.edit().putString("saved_chat_library", updated.toString()).apply()
        historyOpen = false
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            BrandMark(theme, 42)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text("MIMO AI", fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = theme.accent)
                Text("Your personal AI space", fontSize = 11.sp, color = theme.secondary)
            }
            IconButton(onClick = { historyOpen = true }) { Icon(Icons.Outlined.History, "Chat history", tint = theme.secondary) }
            IconButton(onClick = {
                archiveCurrentChat()
                messages.clear()
                prefs.edit().putString("active_chat_messages", "[]").apply()
            }) { Icon(Icons.Outlined.Add, "New chat", tint = theme.accent) }
            IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Settings", tint = theme.secondary) }
        }
        if (downloadStarted && (!brainVerified || !studioVerified)) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                color = theme.surface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, theme.accent.copy(alpha = 0.28f))
            ) {
                Column(Modifier.padding(horizontal = 13.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CloudDownload, null, tint = theme.accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (!brainVerified) "MIMO Brain · chat model" else "MIMO Studio · image model", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        val activeProgress = if (!brainVerified) brainProgress else studioProgress
                        Text("${String.format(Locale.US, "%.2f", activeProgress.coerceIn(0f, 1f) * 100f)}%", color = theme.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(7.dp))
                    val activeProgress = if (!brainVerified) brainProgress else studioProgress
                    LinearProgressIndicator(
                        progress = { activeProgress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                        color = theme.accent, trackColor = theme.background
                    )
                    val activeBytes = if (!brainVerified) brainDownloadedBytes else studioDownloadedBytes
                    val totalBytes = if (!brainVerified) brainTotalBytes else studioTotalBytes
                    Text(
                        when {
                            totalBytes > 0L -> "${formatModelBytes(activeBytes)} / ${formatModelBytes(totalBytes)} · $downloadStatus"
                            else -> downloadStatus
                        },
                        color = theme.secondary, fontSize = 10.sp, lineHeight = 14.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Text(if (!brainVerified) "MIMO Studio will start automatically after Brain is verified." else "Image model download continues while you chat.", color = theme.secondary, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
        if (historyOpen) {
            AlertDialog(
                onDismissRequest = { historyOpen = false },
                title = { Text("Saved conversations", color = Color.White) },
                text = {
                    if (savedChats.length() == 0) Text("Your saved conversations will appear here.", color = theme.secondary)
                    else Column(Modifier.heightIn(max = 360.dp)) {
                        for (i in 0 until savedChats.length()) {
                            val item = savedChats.optJSONObject(i) ?: continue
                            Text(
                                item.optString("title", "Conversation"),
                                Modifier.fillMaxWidth().clickable { loadSavedChat(i) }.padding(vertical = 12.dp),
                                color = Color.White, fontSize = 14.sp
                            )
                            if (i < savedChats.length() - 1) HorizontalDivider(color = theme.secondary.copy(alpha = 0.18f))
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { historyOpen = false }) { Text("Close") } },
                containerColor = theme.surface
            )
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
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (msg.fromUser) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = if (msg.fromUser) theme.accent.copy(alpha = 0.20f) else theme.surface, shape = RoundedCornerShape(18.dp)) {
                            Text(msg.text, Modifier.padding(14.dp).widthIn(max = 290.dp), color = Color.White, fontSize = 14.sp)
                        }
                        if (!msg.fromUser && speechEnabled) {
                            IconButton(onClick = { onSpeak(msg.text) }) {
                                Icon(Icons.Outlined.VolumeUp, "Read aloud", tint = theme.accent)
                            }
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
                    if (autoSpeak) onSpeak(MimoIdentity.previewReply)
                    input = ""
                }
            }) { Icon(Icons.Outlined.Send, "Send", tint = theme.accent) }
        }
        Text(if (studioVerified) "MIMO Studio model verified · image runtime integration still required" else "MIMO preview · model downloads continue in the background", Modifier.fillMaxWidth().padding(top = 8.dp), color = theme.secondary.copy(alpha = 0.65f), fontSize = 10.sp, textAlign = TextAlign.Center)
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
    performanceId: String, speechEnabled: Boolean, autoSpeak: Boolean, speechRate: Float,
    onWifiOnly: (Boolean) -> Unit, onPerformance: (String) -> Unit,
    onSpeechEnabled: (Boolean) -> Unit, onAutoSpeak: (Boolean) -> Unit,
    onSpeechRate: (Float) -> Unit, onTestSpeech: () -> Unit,
    onTheme: (String) -> Unit, language: String, onLanguage: (String) -> Unit, onBack: () -> Unit,
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
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(theme.surface).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Language, null, tint = theme.accent)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Language", color = Color.White, fontWeight = FontWeight.Medium)
                        Text(if (language.startsWith("ar")) "Arabic (Tunisia)" else "English", color = theme.secondary, fontSize = 11.sp)
                    }
                    TextButton(onClick = { onLanguage(if (language.startsWith("ar")) "en" else "ar-TN") }) {
                        Text(if (language.startsWith("ar")) "English" else "العربية")
                    }
                }
                SettingsHeader("PERFORMANCE PROFILE", theme)
                Text("Choose how much processing power future local AI engines may use. The selected profile is saved for the chat and image engines.", color = theme.secondary, fontSize = 12.sp, lineHeight = 17.sp)
                MimoRuntimeConfig.profiles.forEach { profile ->
                    Row(Modifier.fillMaxWidth().padding(top = 7.dp).clip(RoundedCornerShape(14.dp))
                        .background(theme.surface).border(if (performanceId == profile.id) 2.dp else 1.dp,
                            if (performanceId == profile.id) theme.accent else Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .clickable { onPerformance(profile.id) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(when (profile.id) {
                            "low" -> Icons.Outlined.BatterySaver
                            "high" -> Icons.Outlined.Speed
                            else -> Icons.Outlined.Tune
                        }, null, tint = theme.accent, modifier = Modifier.size(23.dp))
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(profile.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(profile.description + " · " + profile.cpuThreads + " CPU threads · " + profile.imageSize + "px image target", color = theme.secondary, fontSize = 10.sp)
                        }
                        if (performanceId == profile.id) Icon(Icons.Outlined.CheckCircle, null, tint = theme.accent)
                    }
                }
                Text("GPU offload is a target preference (" + MimoRuntimeConfig.selected.gpuOffloadPercent + "%). Actual acceleration depends on device drivers and AI backend; CPU fallback is required.", color = theme.secondary, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 5.dp))
                SettingsHeader("TEXT TO SPEECH", theme)
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(theme.surface).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.VolumeUp, null, tint = theme.accent)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("MIMO speaks replies", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("Use the Android installed speech engine", color = theme.secondary, fontSize = 10.sp)
                    }
                    Switch(checked = speechEnabled, onCheckedChange = onSpeechEnabled)
                }
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(theme.surface).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Auto-read assistant replies", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("Speak new assistant messages automatically", color = theme.secondary, fontSize = 10.sp)
                    }
                    Switch(checked = autoSpeak, onCheckedChange = onAutoSpeak, enabled = speechEnabled)
                }
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(theme.surface).padding(12.dp)) {
                    Text("Speech speed · " + String.format(Locale.getDefault(), "%.1f", speechRate) + "×", color = Color.White, fontSize = 13.sp)
                    Slider(value = speechRate, onValueChange = onSpeechRate, valueRange = 0.7f..1.3f, enabled = speechEnabled)
                    OutlinedButton(onClick = onTestSpeech, enabled = speechEnabled, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.PlayArrow, null)
                        Spacer(Modifier.width(7.dp))
                        Text("Test MIMO voice")
                    }
                }
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
                        Text("Ready to download inside MIMO", Modifier.padding(top = 8.dp), color = theme.secondary, fontSize = 11.sp)
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
                        Text("Ready to download inside MIMO", Modifier.padding(top = 8.dp), color = theme.secondary, fontSize = 11.sp)
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


@Composable
private fun SetupBootScreen(
    theme: MimoTheme,
    brainProgress: Float,
    studioProgress: Float,
    brainDownloadedBytes: Long,
    brainTotalBytes: Long,
    studioDownloadedBytes: Long,
    studioTotalBytes: Long,
    brainReady: Boolean,
    studioReady: Boolean,
    downloadStarted: Boolean,
    statusMessage: String,
    language: String,
    wifiOnly: Boolean,
    onWifiOnly: (Boolean) -> Unit,
    onLanguage: (String) -> Unit,
    onStartDownloads: () -> Unit,
    onContinuePreview: () -> Unit
) {
    val tips = when (language) {
        "ar-TN" -> listOf(
            "نصيحة · يمكنك طلب الشرح والترجمة والتلخيص والمساعدة في الكتابة من ميمو.",
            "الخصوصية · بعد الإعداد، تتم معالجة مهام الذكاء الاصطناعي المدعومة على جهازك.",
            "معلومة · أبقِ ميمو مفتوحاً ومتصلًا بالواي فاي أثناء التنزيل الأول.",
            "إعداد ذكي · يتم تجهيز ميمو Brain أولاً، ثم تنزيل أدوات الإبداع."
        )
        "fr" -> listOf(
            "Conseil · Demandez à MIMO d’expliquer, traduire, résumer ou vous aider à écrire.",
            "Confidentialité · Après la configuration, les fonctions prises en charge sont conçues pour fonctionner sur votre appareil.",
            "À savoir · Gardez MIMO ouvert et connecté au Wi-Fi pendant le premier téléchargement.",
            "Configuration intelligente · MIMO prépare d’abord Brain, puis télécharge les outils créatifs."
        )
        else -> listOf(
            "Tip · Ask MIMO to explain, translate, summarize and help you write.",
            "Privacy · After setup, supported AI features are designed to run on your device.",
            "Good to know · Keep MIMO open and connected to Wi-Fi during the first download.",
            "Smart setup · MIMO prepares Brain first, then downloads the creative tools."
        )
    }
    var tipIndex by remember { mutableIntStateOf(0) }
    var languageMenuExpanded by remember { mutableStateOf(false) }
    var showDownloadConsent by remember { mutableStateOf(false) }
    var downloadConsentAccepted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(4200)
            tipIndex = (tipIndex + 1) % tips.size
        }
    }
    val safeBrain = brainProgress.coerceIn(0f, 1f)
    val safeStudio = studioProgress.coerceIn(0f, 1f)
    val waitingForBrain = downloadStarted && !brainReady && safeBrain <= 0f &&
        !statusMessage.contains("failed", ignoreCase = true) && !statusMessage.contains("error", ignoreCase = true)
    val waitingForStudio = downloadStarted && brainReady && !studioReady && safeStudio <= 0f &&
        !statusMessage.contains("failed", ignoreCase = true) && !statusMessage.contains("error", ignoreCase = true)
    val blue = Color(0xFF66B7FF)
    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(Color(0xFF123B70), Color(0xFF07172D), Color(0xFF02060D)),
                radius = 1150f
            )
        )
    ) {
        Box(Modifier.align(Alignment.TopEnd).size(260.dp).offset(x = 100.dp, y = (-70).dp)
            .clip(CircleShape).background(blue.copy(alpha = 0.07f)))
        Box(Modifier.align(Alignment.BottomStart).size(300.dp).offset(x = (-130).dp, y = 100.dp)
            .clip(CircleShape).background(Color(0xFF1766B5).copy(alpha = 0.09f)))
        Column(
            Modifier.fillMaxSize().padding(horizontal = 25.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box {
                    IconButton(onClick = { languageMenuExpanded = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Language menu", tint = blue)
                    }
                    DropdownMenu(expanded = languageMenuExpanded, onDismissRequest = { languageMenuExpanded = false }) {
                        DropdownMenuItem(text = { Text("العربية") }, onClick = { onLanguage("ar-TN"); languageMenuExpanded = false })
                        DropdownMenuItem(text = { Text("English") }, onClick = { onLanguage("en"); languageMenuExpanded = false })
                        DropdownMenuItem(text = { Text("Français") }, onClick = { onLanguage("fr"); languageMenuExpanded = false })
                    }
                }
            }
            BrandMark(theme.copy(accent = blue, surface = Color(0xFF102A49)), 82)
            Spacer(Modifier.height(18.dp))
            Text("MIMO AI", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
            Text("INITIALIZING YOUR PRIVATE AI", color = blue, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(34.dp))
            Text(
                if (brainReady) "MIMO Brain file verified" else "Preparing MIMO Brain",
                color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
            )
            Text(
                if (brainReady) "The Brain file is verified. The on-device chat runtime still needs integration."
                else "Approve the one-time download to install the verified MIMO components.",
                color = Color(0xFFB9CDE4), fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 18.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(Modifier.height(22.dp))
            Surface(
                color = Color(0xFF071426).copy(alpha = 0.86f),
                shape = RoundedCornerShape(22.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, blue.copy(alpha = 0.24f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        "MIMO's main intelligence — the brain behind conversations, answers, writing, translation and everyday assistance.",
                        color = Color(0xFFB9CDE4), fontSize = 11.sp, lineHeight = 16.sp,
                        modifier = Modifier.padding(bottom = 11.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Psychology, null, tint = blue, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(9.dp))
                        Text("MIMO Brain", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(if (brainReady) "VERIFIED" else if (waitingForBrain) "CONNECTING" else "${String.format(Locale.US, "%.2f", safeBrain * 100.0)}%", color = blue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    if (waitingForBrain) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape), color = blue, trackColor = Color(0xFF1A2B40))
                    } else {
                        LinearProgressIndicator(
                            progress = { if (brainReady) 1f else safeBrain },
                            modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                            color = blue, trackColor = Color(0xFF1A2B40)
                        )
                    }
                    Text(
                        if (brainReady) "Model file verified · chat runtime integration pending"
                        else if (downloadStarted && brainTotalBytes <= 0L) "Received ${formatModelBytes(brainDownloadedBytes)} · waiting for the server response or first data"
                        else if (downloadStarted) "${formatModelBytes(brainDownloadedBytes)} / ${formatModelBytes(brainTotalBytes)} received"
                        else "Ready when you approve the download",
                        color = Color(0xFF9FB5CE), fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(Modifier.height(19.dp))
                    Text(
                        "MIMO's creative studio — turns your text descriptions and ideas into original pictures.",
                        color = Color(0xFFB9CDE4), fontSize = 11.sp, lineHeight = 16.sp,
                        modifier = Modifier.padding(bottom = 11.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AutoAwesome, null, tint = Color(0xFF8CAEFF), modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(9.dp))
                        Text("MIMO Studio", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(if (studioReady) "VERIFIED" else if (waitingForStudio) "CONNECTING" else if (brainReady) "${String.format(Locale.US, "%.2f", safeStudio * 100.0)}%" else "QUEUED", color = Color(0xFF9BB7FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    if (waitingForStudio) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape), color = Color(0xFF8CAEFF), trackColor = Color(0xFF1A2B40))
                    } else {
                        LinearProgressIndicator(
                            progress = { if (safeStudio >= 1f) 1f else safeStudio },
                            modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                            color = Color(0xFF8CAEFF), trackColor = Color(0xFF1A2B40)
                        )
                    }
                    Text(
                        if (studioReady) "Model file verified · image runtime integration pending"
                        else if (brainReady && downloadStarted && studioTotalBytes <= 0L) "Received ${formatModelBytes(studioDownloadedBytes)} · waiting for the server response or first data"
                        else if (brainReady && downloadStarted) "${formatModelBytes(studioDownloadedBytes)} / ${formatModelBytes(studioTotalBytes)} received"
                        else if (brainReady) "MIMO Studio download is next"
                        else "Starts after MIMO Brain is verified",
                        color = Color(0xFF9FB5CE), fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(Modifier.height(18.dp))
                    Text("SETUP PROGRESS", color = Color(0xFF8CAEFF), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
                    val total = if (brainReady) (0.65f + safeStudio * 0.35f) else safeBrain * 0.65f
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (waitingForBrain) "CONNECTING…"
                            else "${String.format(Locale.US, "%.2f", total * 100.0)}%",
                            color = Color.White, fontSize = if (waitingForBrain) 15.sp else 25.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            if (waitingForBrain) "Waiting for first bytes"
                            else if (brainReady && safeStudio < 1f) "Chat file verified · Studio continues"
                            else "Actual verified progress",
                            color = Color(0xFF9FB5CE), fontSize = 10.sp
                        )
                    }
                    if (waitingForBrain) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(9.dp).clip(CircleShape), color = blue, trackColor = Color(0xFF1A2B40))
                    } else {
                        LinearProgressIndicator(progress = { total.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(9.dp).clip(CircleShape), color = blue, trackColor = Color(0xFF1A2B40))
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Surface(
                color = Color(0xFF0B1D32).copy(alpha = 0.88f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, blue.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth().heightIn(min = 62.dp)
            ) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lightbulb, null, tint = Color(0xFFFFD27A), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(tips[tipIndex], color = Color(0xFFD5E5F7), fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Surface(
                color = Color(0xFF0B1D32).copy(alpha = 0.88f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, blue.copy(alpha = 0.18f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Wi-Fi only", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(if (wifiOnly) "Downloads wait for Wi-Fi" else "Mobile data is allowed", color = Color(0xFF9FB5CE), fontSize = 10.sp)
                    }
                    Switch(checked = wifiOnly, onCheckedChange = onWifiOnly)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("FIRST SETUP · ABOUT 10 GB TOTAL", color = Color(0xFF7895B7), fontSize = 9.sp, letterSpacing = 1.3.sp)
            Spacer(Modifier.height(8.dp))
            Text(statusMessage, color = Color(0xFFB9CDE4), fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 15.sp)
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { downloadConsentAccepted = false; showDownloadConsent = true },
                enabled = !downloadStarted || (!brainReady || !studioReady),
                colors = ButtonDefaults.buttonColors(containerColor = blue, contentColor = Color(0xFF061426))
            ) {
                Text(if (downloadStarted) "Resume / retry downloads" else "Review & download · about 9.5 GB", fontWeight = FontWeight.Bold)
            }
            Text("Downloads run inside MIMO into private internal app storage, resume partial transfers when you retry, and are SHA-256 verified before being marked complete. This installs model files only; AI engines are a separate integration step.",
                color = Color(0xFF7895B7), fontSize = 9.sp, lineHeight = 13.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 7.dp))
            OutlinedButton(onClick = onContinuePreview, border = androidx.compose.foundation.BorderStroke(1.dp, blue.copy(alpha = 0.45f))) {
                Text("Continue to preview", color = Color(0xFFB9D8FA))
            }
        }
        if (showDownloadConsent) {
            AlertDialog(
                onDismissRequest = { showDownloadConsent = false },
                title = { Text("Download MIMO models?") },
                text = {
                    Column {
                        Text("The first setup downloads about 9.5 GB in total:", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(12.dp))
                        Text("• MIMO Brain · Qwen3 4B Q4_K_M · about 2.5 GB", color = Color(0xFFB9CDE4), fontSize = 12.sp)
                        Text("• MIMO Studio · SDXL 1.0 · about 6.94 GB", color = Color(0xFFB9CDE4), fontSize = 12.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("Files are downloaded one after another and checked with SHA-256 before being marked verified. Keep at least 3 GB free for Brain; MIMO Studio checks its larger storage requirement before downloading.", color = Color(0xFFB9CDE4), fontSize = 12.sp, lineHeight = 18.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(if (wifiOnly) "Wi-Fi only is ON. Downloads will wait for Wi-Fi." else "Mobile data is allowed. Your carrier may charge data fees.", color = Color(0xFFB9CDE4), fontSize = 11.sp)
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = downloadConsentAccepted, onCheckedChange = { downloadConsentAccepted = it })
                            Text("I approve downloading these large model files to this device.", color = Color.White, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        enabled = downloadConsentAccepted,
                        onClick = { showDownloadConsent = false; onStartDownloads() }
                    ) { Text("Accept & start download") }
                },
                dismissButton = {
                    TextButton(onClick = { showDownloadConsent = false }) { Text("Not now", color = Color(0xFFB9CDE4)) }
                },
                containerColor = Color(0xFF10243B)
            )
        }
    }
}
