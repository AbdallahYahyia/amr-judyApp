package com.amrjudy.kids

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean

class SayItItem(val emoji: String, val say: String, val label: String = say)

/**
 * "Look, listen, say it" screen used by many speech games.
 * - mic: record with speech recognition and score with the fuzzy matcher
 * - manual: a 👏 button (for a parent/child to confirm "I did it") that always gives a star
 * - prompt: a question spoken first (e.g. "ده إيه؟") before the word is revealed aloud
 */
@Composable
fun SayItScreen(
    vm: AppViewModel,
    items: List<SayItItem>,
    lang: String = "ar-EG",
    english: Boolean = false,
    manual: Boolean = false,
    mic: Boolean = true,
    prompt: String? = null
) {
    var idx by remember { mutableStateOf(0) }
    var win by remember { mutableStateOf(0) }
    var msg by remember { mutableStateOf("") }
    val item = items[idx]

    fun say() { if (english) vm.speech.speakEn(item.say) else vm.sayWord(item.say) }

    LaunchedEffect(idx) {
        msg = ""
        if (prompt != null && !english) { vm.speech.speak(prompt); delay(1700) }
        say()
    }

    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            BigCard(item.emoji, item.label, Sun, Modifier.fillMaxWidth().weight(1f), 120.sp) { say() }
            Text(
                msg, fontSize = 22.sp, color = Ink, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.padding(8.dp)
            )
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
            ) {
                BigCard("⬅️", "", Peach, Modifier.size(72.dp), 28.sp) { idx = (idx + items.size - 1) % items.size }
                if (mic) {
                    MicButton(vm, item.say, lang = lang) { s ->
                        if (s < 0) {
                            msg = if (english) "Try again 🎤" else "ما سمعتش، جرّب تاني 🎤"
                        } else if (english) {
                            if (s > 0) { vm.addStars(s); win = s; msg = "Great job!"; vm.speech.speakEn("Great job!") }
                            else { msg = "Try again"; vm.speech.speakEn("Try again") }
                        } else {
                            win = reward(vm, s)
                            msg = if (s > 0) "برافو!" else "كمان مرة"
                        }
                    }
                }
                if (manual) {
                    BigCard("👏", "", Mint, Modifier.size(88.dp), 36.sp) {
                        vm.addStars(1); win = 1; msg = "برافو!"
                        vm.speech.speak("برافو! شاطر", pitch = 1.4f)
                    }
                }
                BigCard("➡️", "", Peach, Modifier.size(72.dp), 28.sp) { idx = (idx + 1) % items.size }
            }
        }
        Celebrate(win) { win = 0 }
    }
}

private val SYLLABLES = listOf(
    "با", "ما", "دا", "تا", "نا", "بو", "مو", "أو", "إي", "آه",
    "با با", "ما ما", "دا دا", "بابا", "ماما"
).map { SayItItem("👄", it) }

private val OBJECTS = listOf(
    "⚽" to "كورة", "🥛" to "كوباية", "🍌" to "موزة", "🏠" to "بيت", "☀️" to "شمس", "🌙" to "قمر",
    "⭐" to "نجمة", "👟" to "جزمة", "🚗" to "عربية", "💧" to "مية", "🍞" to "عيش", "🐱" to "قطة",
    "🐶" to "كلب", "🐔" to "فرخة", "🐟" to "سمكة", "🚪" to "باب", "🪑" to "كرسي", "📱" to "تليفون"
).map { SayItItem(it.first, it.second) }

private val PHRASES = listOf(
    "💧" to "عايز مية", "🍞" to "عايز عيش", "👩" to "ماما تعالي", "🚪" to "افتح الباب",
    "🚗" to "عربية تجري", "😴" to "عايز أنام", "🥛" to "لبن حلو", "👋" to "باي باي",
    "🙏" to "لو سمحت", "😊" to "شكراً", "⚽" to "العب كورة", "🍎" to "عايز تفاحة"
).map { SayItItem(it.first, it.second) }

private val ACTIONS = listOf(
    "🍽️" to "بيأكل", "🥤" to "بيشرب", "😴" to "بينام", "🏃" to "بيجري", "🪑" to "بيقعد",
    "🚶" to "بيمشي", "👏" to "بيصقف", "🛁" to "بيستحمى", "🧼" to "بيغسل إيده", "📖" to "بيقرأ"
).map { SayItItem(it.first, it.second) }

private val MOUTH = listOf(
    "👅" to "طلّع لسانك", "💨" to "انفخ زي الشمعة", "😁" to "ابتسم ابتسامة كبيرة", "😗" to "ابعت بوسة",
    "😮" to "افتح بقك كبير", "🎈" to "انفخ البالونة", "😛" to "حرّك لسانك يمين وشمال", "🦁" to "زأر زي الأسد"
).map { SayItItem(it.first, it.second) }

private val FRUITS = listOf(
    "🍎" to "تفاحة", "🍌" to "موزة", "🍊" to "برتقانة", "🍓" to "فراولة",
    "🍇" to "عنب", "🍉" to "بطيخ", "🍑" to "خوخ"
).map { SayItItem(it.first, it.second) }

private val VEHICLES = listOf(
    "🚗" to "عربية", "✈️" to "طيارة", "🚂" to "قطر", "🚢" to "مركب",
    "🚲" to "عجلة", "🚌" to "أتوبيس", "🛵" to "موتوسيكل", "🚁" to "هليكوبتر"
).map { SayItItem(it.first, it.second) }

private val CLOTHES = listOf(
    "👟" to "جزمة", "👕" to "تيشيرت", "👖" to "بنطلون", "🧦" to "شراب",
    "🧢" to "كاب", "👗" to "فستان", "🧥" to "جاكت", "🧤" to "جوانتي"
).map { SayItItem(it.first, it.second) }

private val FAMILY = listOf(
    "👩" to "ماما", "👨" to "بابا", "👴" to "جدو", "👵" to "تيتة",
    "👦" to "أخويا", "👧" to "أختي", "👶" to "بيبي"
).map { SayItItem(it.first, it.second) }

private val HOME = listOf(
    "🚪" to "باب", "🛏️" to "سرير", "🪑" to "كرسي", "🛋️" to "كنبة",
    "💡" to "نور", "📺" to "تليفزيون", "🚽" to "حمام", "🥄" to "معلقة"
).map { SayItItem(it.first, it.second) }

private val FOOD = listOf(
    "🍞" to "عيش", "🥛" to "لبن", "🧀" to "جبنة", "🥚" to "بيضة", "🍚" to "رز", "🍗" to "فراخ"
).map { SayItItem(it.first, it.second) }

private val RECEPTIVE = listOf(
    "⚽" to "الكورة", "🥛" to "الكوباية", "🍌" to "الموزة", "🏠" to "البيت", "☀️" to "الشمس",
    "🌙" to "القمر", "⭐" to "النجمة", "👟" to "الجزمة", "🚗" to "العربية", "🍞" to "العيش",
    "🐱" to "القطة", "🐶" to "الكلب", "🚪" to "الباب", "🪑" to "الكرسي", "📱" to "التليفون"
).map { QItem(it.first, it.second) }

/** Blow-the-balloon: the louder / longer the child vocalises, the bigger the balloon grows. */
@Composable
fun BalloonBlow(vm: AppViewModel) {
    var size by remember { mutableStateOf(0.25f) }
    var running by remember { mutableStateOf(false) }
    var popped by remember { mutableStateOf(false) }
    var win by remember { mutableStateOf(0) }
    var msg by remember { mutableStateOf("اضغط الميكروفون وصوّت: آآآآ عشان البالونة تكبر!") }
    val handler = remember { Handler(Looper.getMainLooper()) }
    val stopFlag = remember { AtomicBoolean(false) }
    DisposableEffect(Unit) { onDispose { stopFlag.set(true) } }

    fun start() {
        if (running) return
        vm.speech.stopSpeaking()
        size = 0.25f; popped = false; running = true; msg = "صوّت: آآآآ!"
        stopFlag.set(false)
        Thread {
            val ok = try {
                streamVoice(12, { stopFlag.get() }) { voiced ->
                    handler.post {
                        if (running && !popped) {
                            size = (size + (if (voiced) 0.045f else -0.012f)).coerceIn(0.2f, 1f)
                            if (size >= 1f) {
                                popped = true
                                stopFlag.set(true)
                                vm.addStars(3); win = 3
                                msg = "بوم! برافو 🎉"
                                vm.speech.speak("بوم! برافو", pitch = 1.5f)
                            }
                        }
                    }
                }
            } catch (e: Exception) { false }
            handler.post {
                running = false
                if (!ok) msg = "الميكروفون مش شغال، اسمح له من إعدادات التطبيق"
                else if (!popped) msg = "حاول تاني! صوّت أعلى وأطول"
            }
        }.start()
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(if (popped) "💥" else "🎈", fontSize = (50 + 190 * size).sp)
            }
            Text(
                msg, fontSize = 22.sp, color = Ink, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.padding(8.dp)
            )
            BigCard(
                if (running) "👂" else "🎤", if (running) "صوّت!" else "ابدأ",
                if (running) Pink else Mint, Modifier.size(width = 200.dp, height = 120.dp), 40.sp
            ) { start() }
            Spacer(Modifier.height(12.dp))
        }
        Celebrate(win) { win = 0 }
    }
}

/** Extra speech-therapy games for Amr. */
@Composable
fun TalkHub(vm: AppViewModel) {
    Hub(
        vm, "ألعاب الكلام",
        listOf(
            HubCard("🎈", "نفخ البالونة", Pink) { BalloonBlow(vm) },
            HubCard("👄", "أصوات", Sky) { SayItScreen(vm, SYLLABLES, manual = true) },
            HubCard("👂", "فين؟", Mint) { QuizBody(vm, RECEPTIVE, 80) },
            HubCard("🖼️", "ده إيه؟", Peach) { SayItScreen(vm, OBJECTS, manual = true, prompt = "ده إيه؟") },
            HubCard("💬", "كلمتين", Lilac) { SayItScreen(vm, PHRASES, manual = true) },
            HubCard("🏃", "بيعمل إيه؟", Sun) { SayItScreen(vm, ACTIONS, manual = true, prompt = "بيعمل إيه؟") },
            HubCard("😛", "تمارين الفم", Pink) { SayItScreen(vm, MOUTH, mic = false, manual = true) },
            HubCard("🍎", "فواكه", Sun) { SayItScreen(vm, FRUITS, manual = true, prompt = "ده إيه؟") },
            HubCard("🚗", "مواصلات", Sky) { SayItScreen(vm, VEHICLES, manual = true, prompt = "ده إيه؟") },
            HubCard("👕", "هدوم", Mint) { SayItScreen(vm, CLOTHES, manual = true, prompt = "ده إيه؟") },
            HubCard("👪", "العيلة", Peach) { SayItScreen(vm, FAMILY, manual = true, prompt = "دي مين؟") },
            HubCard("🏠", "البيت", Lilac) { SayItScreen(vm, HOME, manual = true, prompt = "ده إيه؟") },
            HubCard("🍞", "الأكل", Pink) { SayItScreen(vm, FOOD, manual = true, prompt = "ده إيه؟") }
        )
    )
}
