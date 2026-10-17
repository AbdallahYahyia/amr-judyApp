package com.amrjudy.kids

import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class QItem(val glyph: String, val name: String, val tint: Color = Ink, val prompt: String? = null)

/** Generic "where is…?" quiz used by colours, shapes, body parts and animal sounds. */
@Composable
fun QuizBody(vm: AppViewModel, items: List<QItem>, glyphSize: Int = 80) {
    var target by remember(items) { mutableStateOf(items.random()) }
    var win by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val choices = remember(target) {
        (listOf(target) + items.filter { it !== target }.shuffled().take(3)).shuffled()
    }
    val question = target.prompt ?: "فين ${target.name}؟"
    LaunchedEffect(target) { vm.speech.speak(question, pitch = 1.3f) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                question, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Ink,
                modifier = Modifier.fillMaxWidth().clickable { vm.speech.speak(question, pitch = 1.3f) },
                textAlign = TextAlign.Center
            )
            for (row in choices.chunked(2)) {
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (c in row) {
                        Box(
                            Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(28.dp))
                                .background(Color.White)
                                .clickable {
                                    if (c === target) {
                                        vm.addStars(1); win = 1
                                        vm.speech.speak("برافو!", pitch = 1.5f)
                                        scope.launch { delay(1400); win = 0; target = items.filter { it !== target }.random() }
                                    } else vm.speech.speak("ده ${c.name}. دوّر تاني", pitch = 1.3f)
                                },
                            contentAlignment = Alignment.Center
                        ) { Text(c.glyph, fontSize = glyphSize.sp, color = c.tint) }
                    }
                }
            }
        }
        Celebrate(win) { win = 0 }
    }
}

/* ---------------- Feature 5: Colours & shapes ---------------- */

private val COLORS = listOf(
    QItem("●", "اللون الأحمر", Color(0xFFFF6B6B)),
    QItem("●", "اللون الأزرق", Color(0xFF4DABF7)),
    QItem("●", "اللون الأصفر", Color(0xFFFFD43B)),
    QItem("●", "اللون الأخضر", Color(0xFF51CF66)),
    QItem("●", "اللون البرتقالي", Color(0xFFFF922B)),
    QItem("●", "اللون الوردي", Color(0xFFF783AC))
)
private val SHAPES = listOf(
    QItem("●", "الدايرة", Color(0xFFFF6B6B)),
    QItem("■", "المربع", Color(0xFF4DABF7)),
    QItem("▲", "المثلث", Color(0xFF51CF66)),
    QItem("★", "النجمة", Color(0xFFFFB300)),
    QItem("♥", "القلب", Color(0xFFF783AC)),
    QItem("▬", "المستطيل", Color(0xFF9775FA))
)

@Composable
fun ColorsShapes(vm: AppViewModel) {
    var tab by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().background(Cream)) {
        TopBar("الألوان والأشكال", vm.stars) { vm.back() }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigCard("🎨", "ألوان", if (tab == 0) Pink else Color(0xFFE9ECEF), Modifier.weight(1f).height(90.dp), 30.sp) { tab = 0 }
            BigCard("🔷", "أشكال", if (tab == 1) Sky else Color(0xFFE9ECEF), Modifier.weight(1f).height(90.dp), 30.sp) { tab = 1 }
        }
        if (tab == 0) QuizBody(vm, COLORS, 110) else QuizBody(vm, SHAPES, 100)
    }
}

/* ---------------- Feature 6: Body parts ---------------- */

private val BODY = listOf(
    QItem("👁️", "العين"), QItem("👃", "الأنف"), QItem("✋", "الإيد"),
    QItem("👂", "الودن"), QItem("👄", "الفم"), QItem("🦶", "الرجل")
)

@Composable
fun BodyParts(vm: AppViewModel) {
    Column(Modifier.fillMaxSize().background(Cream)) {
        TopBar("أجزاء الجسم", vm.stars) { vm.back() }
        Box(Modifier.fillMaxWidth().height(110.dp), contentAlignment = Alignment.Center) {
            Bouncy { Text("🧒", fontSize = 80.sp) }
        }
        QuizBody(vm, BODY, 80)
    }
}

/* ---------------- Feature 7: Counting 1-5 ---------------- */

private val NUM_WORDS = listOf("", "واحد", "اتنين", "تلاتة", "أربعة", "خمسة")
private val FRUITS = listOf("🍎", "🍌", "⚽", "🍓", "🍊")

@Composable
fun Counting(vm: AppViewModel) {
    var n by remember { mutableStateOf((1..5).random()) }
    var fruit by remember { mutableStateOf(FRUITS.random()) }
    var win by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    fun countAloud() = vm.speech.speak((1..n).joinToString("، ") { NUM_WORDS[it] }, pitch = 1.3f, rate = 0.6f)
    LaunchedEffect(n, fruit) { countAloud() }
    Box(Modifier.fillMaxSize().background(Cream)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            TopBar("عدّ معايا", vm.stars) { vm.back() }
            Box(Modifier.weight(1f).fillMaxWidth().clickable { countAloud() }, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val rows = if (n > 3) listOf(n - 3, 3).let { listOf(3, n - 3) } else listOf(n)
                    for (r in rows) Row { repeat(r) { Text(fruit, fontSize = 64.sp) } }
                    Text("كام ${if (fruit == "⚽") "كورة" else "واحدة"}؟", fontSize = 26.sp, color = Ink, fontWeight = FontWeight.Bold)
                }
            }
            Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 1..5) {
                    BigCard("$i", "", listOf(Pink, Sky, Mint, Sun, Lilac)[i - 1], Modifier.weight(1f).height(100.dp), 40.sp) {
                        if (i == n) {
                            vm.addStars(1); win = 1
                            vm.speech.speak("${NUM_WORDS[n]}! برافو", pitch = 1.5f)
                            scope.launch { delay(1600); win = 0; n = (1..5).random(); fruit = FRUITS.random() }
                        } else vm.speech.speak("عدّ تاني معايا", pitch = 1.3f)
                    }
                }
            }
        }
        Celebrate(win) { win = 0 }
    }
}

/* ---------------- Feature 8: Needs & emotions ---------------- */

private class Need(val emoji: String, val label: String, val phrase: String, val color: Color)

private val NEEDS = listOf(
    Need("🥤", "عطشان", "أنا عطشان، عايز مية", Sky),
    Need("🍽️", "جائع", "أنا جعان، عايز آكل", Peach),
    Need("😢", "زعلان", "أنا زعلان", Lilac),
    Need("😊", "مبسوط", "أنا مبسوط", Sun),
    Need("😴", "عاوز أنام", "أنا عاوز أنام", Mint),
    Need("🚽", "الحمام", "عايز أدخل الحمام", Pink)
)

@Composable
fun NeedsCards(vm: AppViewModel) {
    Column(Modifier.fillMaxSize().background(Cream)) {
        TopBar("أنا عايز...", vm.stars) { vm.back() }
        Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (row in NEEDS.chunked(2)) {
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (n in row) BigCard(n.emoji, n.label, n.color, Modifier.weight(1f).fillMaxHeight(), 64.sp) {
                        vm.sayWord(n.label, n.phrase)
                    }
                }
            }
        }
    }
}

/* ---------------- NEW: Animal sounds (for Amr) ---------------- */

private class Animal(val emoji: String, val name: String, val sound: String, val color: Color)

private val ANIMALS = listOf(
    Animal("🐱", "القطة", "مياو مياو", Peach),
    Animal("🐶", "الكلب", "هو هو", Sky),
    Animal("🐮", "البقرة", "موو موو", Mint),
    Animal("🐑", "الخروف", "ماااع", Lilac),
    Animal("🐔", "الفرخة", "كوكو كوكو", Sun),
    Animal("🦆", "البطة", "بط بط", Pink),
    Animal("🐴", "الحصان", "ايهيهي", Peach),
    Animal("🦁", "الأسد", "جررر", Sun)
)

private val ANIMAL_QUIZ = ANIMALS.map { QItem(it.emoji, it.name, Ink, "مين بيقول ${it.sound}؟") }

@Composable
fun AnimalSounds(vm: AppViewModel) {
    var tab by remember { mutableStateOf(0) }
    var current by remember { mutableStateOf<Animal?>(null) }
    var win by remember { mutableStateOf(0) }
    var msg by remember { mutableStateOf("اضغط على الحيوان") }
    Box(Modifier.fillMaxSize().background(Cream)) {
        Column(Modifier.fillMaxSize()) {
            TopBar("أصوات الحيوانات", vm.stars) { vm.back() }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BigCard("🔊", "قلّد", if (tab == 0) Mint else Color(0xFFE9ECEF), Modifier.weight(1f).height(80.dp), 28.sp) { tab = 0 }
                BigCard("❓", "مين بيقول؟", if (tab == 1) Sky else Color(0xFFE9ECEF), Modifier.weight(1f).height(80.dp), 28.sp) { tab = 1 }
            }
            if (tab == 1) {
                QuizBody(vm, ANIMAL_QUIZ, 90)
            } else {
                val a = current
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (a == null) Text(msg, fontSize = 26.sp, color = Ink)
                    else Bouncy {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(a.emoji, fontSize = 130.sp)
                            Text(a.sound, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                            Text(msg, fontSize = 20.sp, color = Ink)
                        }
                    }
                }
                if (a != null) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        BigCard("🔊", "", Sun, Modifier.size(96.dp), 40.sp) { vm.speech.speak(a.sound, pitch = 1.3f, rate = 0.7f) }
                        Spacer(Modifier.width(24.dp))
                        SoundMicButton(vm) { s ->
                            if (s < 0) msg = "الميكروفون مش شغال، اسمح له من إعدادات التطبيق 🎤"
                            else { win = reward(vm, s); msg = if (s > 0) "برافو! سمعتك 👏" else "قلّد تاني بصوت عالي" }
                        }
                    }
                }
                for (row in ANIMALS.chunked(4)) {
                    Row(Modifier.fillMaxWidth().height(100.dp).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (an in row) BigCard(an.emoji, "", an.color, Modifier.weight(1f).fillMaxHeight(), 36.sp) {
                            current = an; msg = "قلّد ${an.name}"
                            vm.speech.speak("${an.name} بتقول ${an.sound}", pitch = 1.3f, rate = 0.7f)
                        }
                    }
                }
            }
        }
        Celebrate(win) { win = 0 }
    }
}

/* ---------------- Feature 9: Buddy mascot ---------------- */

private fun buddyReply(said: String, name: String): String? {
    val s = Fuzzy.normalize(said)
    fun has(vararg w: String) = w.any { it in s }
    return when {
        has("اسمك", "انت مين") -> "أنا دبدوب صاحبك!"
        has("سلام", "مرحبا", "اهلا", "هاي", "صباح", "مساء") -> "أهلاً يا $name! نورتني"
        has("ماما") -> "ماما بتحبك أوي"
        has("بابا") -> "بابا بيحبك أوي"
        has("جعان", "اكل") -> "يلا ناكل حاجة حلوة! تفاحة؟"
        has("عطشان", "مية", "ميه") -> "هات كوباية مية يا $name"
        has("نام", "نعسان") -> "تصبح على خير يا $name"
        has("بحبك") -> "وأنا كمان بحبك يا $name"
        has("قطة", "مياو") -> "مياو مياو! القطة بتقول مياو"
        has("كلب", "هو هو") -> "هو هو! الكلب بيقول هو هو"
        has("شكرا") -> "العفو يا $name"
        else -> null
    }
}

private val BUDDY_QUIZ = listOf(
    "🍎" to "تفاحة", "⚽" to "كورة", "🐱" to "قطة", "🚗" to "عربية",
    "🍌" to "موزة", "🏠" to "بيت", "🐶" to "كلب", "🌙" to "قمر"
)

@Composable
fun Buddy(vm: AppViewModel) {
    val kid = vm.profile?.nameAr ?: "حبيبي"
    var reply by remember { mutableStateOf("أهلاً يا $kid! اختار حاجة أو اضغط الميكروفون وكلمني") }
    var heard by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var quiz by remember { mutableStateOf<Pair<String, String>?>(null) }
    var win by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    fun say(text: String) { reply = text; vm.speech.speak(text, pitch = 1.4f) }

    LaunchedEffect(Unit) { vm.speech.speak("أهلاً يا $kid! أنا دبدوب", pitch = 1.4f) }

    // Starts the recogniser after the bear has stopped talking so it doesn't hear itself.
    fun listenNow(target: String?) {
        if (listening) return
        vm.speech.stopSpeaking()
        listening = true
        vm.refreshOnline()
        Handler(Looper.getMainLooper()).postDelayed({
            vm.speech.listen(
                onResult = { l ->
                    listening = false
                    heard = l.first()
                    val q = quiz
                    if (target != null && q != null) {
                        val sc = Fuzzy.score(target, l)
                        if (sc > 0) {
                            win = reward(vm, sc); reply = "برافو! ${q.second} 👏"
                            scope.launch { delay(1800); win = 0; quiz = null }
                        } else say("قول ${q.second}")
                    } else {
                        vm.addStars(1)
                        say(buddyReply(l.first(), kid) ?: "حلو أوي يا $kid! قول تاني")
                    }
                },
                onFail = { listening = false; say("ما سمعتكش، قول تاني") }
            )
        }, 500)
    }

    Box(Modifier.fillMaxSize().background(Cream)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopBar("دبدوب صاحبك", vm.stars) { vm.back() }
            Text(
                if (vm.online) "🌐 متصل بالإنترنت" else "📴 بدون إنترنت (شغّال عادي)",
                fontSize = 14.sp, color = Ink
            )
            Bouncy(Modifier.padding(8.dp)) { Text(if (listening) "👂" else "🐻", fontSize = 110.sp) }
            val q = quiz
            if (q != null) Text(q.first, fontSize = 80.sp)
            Text(
                reply, fontSize = 22.sp, color = Ink, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (heard.isNotEmpty()) Text("سمعتك بتقول: $heard", fontSize = 13.sp, color = Ink)
            Spacer(Modifier.height(6.dp))
            val topics = listOf(
                "👋" to "سلام", "🍎" to "جعان", "😴" to "نعسان",
                "🐱" to "قطة", "🎵" to "غنّي", "❓" to "سؤال"
            )
            for (row in topics.chunked(3)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for ((e, label) in row) {
                        BigCard(e, label, Peach, Modifier.weight(1f).height(96.dp), 34.sp) {
                            quiz = null
                            when (label) {
                                "سلام" -> say("أهلاً يا $kid! أنا دبدوب. إزيك؟")
                                "جعان" -> say("يلا ناكل تفاحة! نام نام نام")
                                "نعسان" -> say("تصبح على خير يا $kid. نام نام")
                                "قطة" -> say("القطة بتقول مياو مياو")
                                "غنّي" -> say("دبدوب بيقول لا لا لا. $kid بيقول لا لا لا")
                                else -> {
                                    val item = BUDDY_QUIZ.random()
                                    quiz = item
                                    reply = "ده إيه؟"
                                    vm.speech.speak("ده إيه؟", pitch = 1.4f)
                                    scope.launch { delay(1800); vm.speech.speak(item.second, pitch = 1.3f) }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            BigCard(
                if (listening) "👂" else "🎤", if (listening) "اتكلم!" else "كلّمني",
                if (listening) Pink else Mint,
                Modifier.fillMaxWidth(0.8f).height(120.dp), 40.sp
            ) { listenNow(quiz?.second) }
            Spacer(Modifier.height(16.dp))
        }
        Celebrate(win) { win = 0 }
    }
}

/* ---------------- Feature 11: Star tree & stickers ---------------- */

@Composable
fun StarTree(vm: AppViewModel) {
    Column(Modifier.fillMaxSize().background(Cream), horizontalAlignment = Alignment.CenterHorizontally) {
        TopBar("شجرة النجوم", vm.stars) { vm.back() }
        Text("🌳", fontSize = 110.sp)
        Text("⭐ ${vm.stars}", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Text("كل ٥ نجوم = ملصق جديد  (${vm.stars % 5}/5)", fontSize = 18.sp, color = Ink)
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            vm.stickerPool.chunked(4).forEachIndexed { r, row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEachIndexed { c, s ->
                        val unlocked = r * 4 + c < vm.unlockedStickers
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(20.dp))
                                .background(if (unlocked) Sun else Color(0xFFE9ECEF)),
                            contentAlignment = Alignment.Center
                        ) { Text(if (unlocked) s else "🔒", fontSize = 36.sp) }
                    }
                }
            }
        }
    }
}

/* ---------------- Family album (photo uploaded by parent) ---------------- */

@Composable
fun FamilyAlbum(vm: AppViewModel) {
    val path = vm.photoUri
    val bmp = remember(path) {
        path?.let { p -> File(p).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path)?.asImageBitmap() } }
    }
    Column(Modifier.fillMaxSize().background(Cream), horizontalAlignment = Alignment.CenterHorizontally) {
        TopBar("عيلتي", vm.stars) { vm.back() }
        if (bmp != null) {
            Image(bmp, contentDescription = null, contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().weight(1f).padding(12.dp).clip(RoundedCornerShape(24.dp)))
        } else {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Text("👨‍👩‍👧‍👦", fontSize = 100.sp) }
        }
    }
}
