package com.amrjudy.kids

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** An Arabic letter with its name, an example word and a picture. */
private class AL(val ch: String, val name: String, val word: String, val emoji: String)

private val ALPHA = listOf(
    AL("أ", "ألف", "أرنب", "🐰"), AL("ب", "باء", "بطة", "🦆"), AL("ت", "تاء", "تفاحة", "🍎"),
    AL("ث", "ثاء", "ثعلب", "🦊"), AL("ج", "جيم", "جمل", "🐪"), AL("ح", "حاء", "حصان", "🐴"),
    AL("خ", "خاء", "خروف", "🐑"), AL("د", "دال", "دب", "🐻"), AL("ذ", "ذال", "ذرة", "🌽"),
    AL("ر", "راء", "راديو", "📻"), AL("ز", "زاي", "زرافة", "🦒"), AL("س", "سين", "سمكة", "🐟"),
    AL("ش", "شين", "شمس", "☀️"), AL("ص", "صاد", "صقر", "🦅"), AL("ض", "ضاد", "ضفدع", "🐸"),
    AL("ط", "طاء", "طيارة", "✈️"), AL("ظ", "ظاء", "ظرف", "✉️"), AL("ع", "عين", "عصفور", "🐦"),
    AL("غ", "غين", "غزال", "🦌"), AL("ف", "فاء", "فيل", "🐘"), AL("ق", "قاف", "قرد", "🐒"),
    AL("ك", "كاف", "كلب", "🐶"), AL("ل", "لام", "ليمون", "🍋"), AL("م", "ميم", "موزة", "🍌"),
    AL("ن", "نون", "نحلة", "🐝"), AL("ه", "هاء", "هلال", "🌙"), AL("و", "واو", "وردة", "🌹"),
    AL("ي", "ياء", "يد", "✋")
)

private val BUILD_WORDS = listOf(
    "🌙" to "قمر", "🏠" to "بيت", "🐪" to "جمل", "👦" to "ولد", "👧" to "بنت", "🐶" to "كلب",
    "🐱" to "قطة", "🐘" to "فيل", "🐟" to "سمك", "🐝" to "نحل", "☀️" to "شمس", "📖" to "كتاب",
    "✏️" to "قلم", "🚪" to "باب"
)

private fun letterChoices(answer: AL): List<String> =
    (listOf(answer.ch) + ALPHA.filter { it !== answer }.shuffled().take(3).map { it.ch }).shuffled()

fun arabicLetterNames(): List<String> = ALPHA.map { it.name }

/** Words with full diacritics (tashkeel) for first-grade reading. */
private val TASHKEEL = mapOf(
    "أرنب" to "أَرْنَب", "بطة" to "بَطَّة", "تفاحة" to "تُفَّاحَة", "ثعلب" to "ثَعْلَب", "جمل" to "جَمَل",
    "حصان" to "حِصَان", "خروف" to "خَرُوف", "دب" to "دُبّ", "ذرة" to "ذُرَة", "راديو" to "رَادْيُو",
    "زرافة" to "زَرَافَة", "سمكة" to "سَمَكَة", "شمس" to "شَمْس", "صقر" to "صَقْر", "ضفدع" to "ضِفْدَع",
    "طيارة" to "طَيَّارَة", "ظرف" to "ظَرْف", "عصفور" to "عُصْفُور", "غزال" to "غَزَال", "فيل" to "فِيل",
    "قرد" to "قِرْد", "كلب" to "كَلْب", "ليمون" to "لَيْمُون", "موزة" to "مَوْزَة", "نحلة" to "نَحْلَة",
    "هلال" to "هِلَال", "وردة" to "وَرْدَة", "يد" to "يَد"
)

private fun tk(l: AL): String = TASHKEEL[l.word] ?: l.word

private val SENTENCES = listOf(
    "🏫" to "ذَهَبْتُ إِلَى الْمَدْرَسَةِ", "📖" to "أَقْرَأُ الْكِتَابَ", "🐱" to "الْقِطَّةُ تَشْرَبُ اللَّبَنَ",
    "🌞" to "الشَّمْسُ مُشْرِقَةٌ", "👩" to "أُحِبُّ أُمِّي", "👨" to "أَبِي يَذْهَبُ إِلَى الْعَمَلِ",
    "🍎" to "أَكَلْتُ تُفَّاحَةً", "🌙" to "الْقَمَرُ جَمِيلٌ", "🐟" to "السَّمَكُ يَسْبَحُ فِي الْمَاءِ",
    "🌹" to "الْوَرْدَةُ جَمِيلَةٌ"
)

// Diacritic marks: fatha, damma, kasra, sukun, shadda, tanween fath
private val MARKS = listOf(
    "\u064E" to "فتحة", "\u064F" to "ضمة", "\u0650" to "كسرة",
    "\u0652" to "سكون", "\u0651" to "شدة", "\u064B" to "تنوين فتح"
)

private fun markNameQ(): MQ {
    val l = ALPHA.random(); val m = MARKS.random()
    val choices = (listOf(m.second) + MARKS.filter { it !== m }.shuffled().take(3).map { it.second }).shuffled()
    return MQ(l.ch + m.first, "إيه اسم العلامة اللي على الحرف؟", "إيه اسم العلامة اللي على الحرف؟", choices, m.second)
}

private fun readingQ(): MQ {
    val l = ALPHA.random()
    val i = (0..2).random()
    val sounds = listOf(l.ch + "ا", l.ch + "و", l.ch + "ي")
    return MQ(l.ch + MARKS[i].first, "بتتقرأ إزاي؟", "الحرف ده بيتقرأ إزاي؟", sounds.shuffled(), sounds[i])
}

private fun wordPictureQ(): MQ {
    val l = ALPHA.random()
    val choices = (listOf(tk(l)) + ALPHA.filter { it !== l }.shuffled().take(3).map { tk(it) }).shuffled()
    return MQ(l.emoji, "اقرأ واختار الكلمة الصح", "إيه اسم الصورة دي؟ اقرأ الكلمات واختار", choices, tk(l))
}

private fun listenWordQ(): MQ {
    val l = ALPHA.random()
    val choices = (listOf(tk(l)) + ALPHA.filter { it !== l }.shuffled().take(3).map { tk(it) }).shuffled()
    return MQ("🔊", "اسمع واختار الكلمة", l.word, choices, tk(l))
}

/** Letters for Amr (3 years): hear the letter, find it, say it, and say letter + vowel sounds (با، بو، بي). */
@Composable
fun AmrLettersHub(vm: AppViewModel) {
    Hub(
        vm, "الحروف",
        listOf(
            HubCard("🔤", "اتعرف على الحروف", Sky) { LetterExplorer(vm) },
            HubCard("👂", "فين الحرف؟", Mint) {
                QuizRunner(vm, glyphSize = 80) {
                    val l = ALPHA.random()
                    val ch = (listOf(l.ch) + ALPHA.filter { it !== l }.shuffled().take(2).map { it.ch }).shuffled()
                    MQ(l.emoji, "فين حرف ${l.name}؟", "فين حرف ${l.name}؟", ch, l.ch)
                }
            },
            HubCard("🗣️", "قول الحرف", Pink) {
                SayItScreen(vm, ALPHA.map { SayItItem(it.emoji, it.name, "${it.ch}  ${it.name}") }, manual = true)
            },
            HubCard("🔊", "با - بو - بي", Peach) {
                SayItScreen(
                    vm,
                    ALPHA.flatMap { l ->
                        listOf(
                            SayItItem("👄", l.ch + "ا", l.ch + MARKS[0].first),
                            SayItItem("👄", l.ch + "و", l.ch + MARKS[1].first),
                            SayItItem("👄", l.ch + "ي", l.ch + MARKS[2].first)
                        )
                    },
                    manual = true
                )
            },
            HubCard("🖼️", "حرف وكلمة", Sun) {
                SayItScreen(vm, ALPHA.map { SayItItem(it.emoji, it.word, "${it.ch}  ${it.word}") }, manual = true)
            }
        )
    )
}

/** Judy's Arabic language lessons (first grade). */
@Composable
fun ArabicHub(vm: AppViewModel) {
    Hub(
        vm, "لغتي",
        listOf(
            HubCard("🔤", "الحروف", Sky) { LetterExplorer(vm) },
            HubCard("👂", "فين الحرف؟", Mint) {
                QuizRunner(vm, glyphSize = 60) {
                    val l = ALPHA.random()
                    MQ(l.emoji, "فين حرف ${l.name}؟", "فين حرف ${l.name}؟", letterChoices(l), l.ch)
                }
            },
            HubCard("🅰️", "أول حرف", Peach) {
                QuizRunner(vm, glyphSize = 110) {
                    val l = ALPHA.random()
                    MQ(l.emoji, "إيه أول حرف في ${l.word}؟", "إيه أول حرف في ${l.word}؟", letterChoices(l), l.ch)
                }
            },
            HubCard("✏️", "كمّل الكلمة", Sun) {
                QuizRunner(vm, glyphSize = 100) {
                    val l = ALPHA.random()
                    MQ(l.emoji, "؟" + l.word.drop(1), "كمّل الكلمة: ${l.word}", letterChoices(l), l.ch)
                }
            },
            HubCard("🧩", "ركّب الكلمة", Lilac) { WordBuild(vm) },
            HubCard("🗣️", "انطق", Pink) {
                SayItScreen(vm, ALPHA.map { SayItItem(it.emoji, it.word, "${it.ch}  ${it.word}") })
            },
            HubCard("✨", "كلمات بالتشكيل", Sun) {
                SayItScreen(vm, ALPHA.map { SayItItem(it.emoji, it.word, tk(it)) })
            },
            HubCard("📝", "جمل بالتشكيل", Sky) {
                SayItScreen(vm, SENTENCES.map { SayItItem(it.first, it.second) })
            },
            HubCard("🔣", "اسم الحركة", Peach) { QuizRunner(vm, glyphSize = 110) { markNameQ() } },
            HubCard("🔊", "بتتقرأ إزاي؟", Lilac) { QuizRunner(vm, glyphSize = 110) { readingQ() } },
            HubCard("🖼️", "صل الكلمة بالصورة", Mint) { QuizRunner(vm, glyphSize = 100) { wordPictureQ() } },
            HubCard("👂", "اسمع واختار", Pink) { QuizRunner(vm, glyphSize = 80) { listenWordQ() } }
        )
    )
}

@Composable
private fun LetterExplorer(vm: AppViewModel) {
    var sel by remember { mutableStateOf(0) }
    val l = ALPHA[sel]
    fun hear() = vm.speech.speak("حرف ${l.name}، زي ${l.word}", pitch = 1.1f, rate = 0.7f)
    LaunchedEffect(sel) { hear() }
    Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(28.dp)).background(Sun).clickable { hear() },
            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
        ) {
            Text(l.ch, fontSize = 110.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Bouncy { Text(l.emoji, fontSize = 76.sp) }
                Text(l.word, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text(l.name, fontSize = 18.sp, color = Ink)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            BigCard("⬅️", "", Peach, Modifier.size(72.dp), 28.sp) { sel = (sel + ALPHA.size - 1) % ALPHA.size }
            MicButton(vm, l.word) { s ->
                if (s > 0) { vm.addStars(s); vm.speech.speak("برافو يا جودي!", pitch = 1.3f) }
                else vm.speech.speak("جرّبي تاني", pitch = 1.3f)
            }
            BigCard("➡️", "", Peach, Modifier.size(72.dp), 28.sp) { sel = (sel + 1) % ALPHA.size }
        }
        Spacer(Modifier.height(8.dp))
        for (row in ALPHA.chunked(7)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (x in row) {
                    Box(
                        Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(12.dp))
                            .background(if (x === l) Pink else Color.White)
                            .clickable { sel = ALPHA.indexOf(x) },
                        contentAlignment = Alignment.Center
                    ) { Text(x.ch, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink) }
                }
                repeat(7 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Tap the scrambled letters in the right order to build the word. */
@Composable
private fun WordBuild(vm: AppViewModel) {
    var target by remember { mutableStateOf(BUILD_WORDS.random()) }
    var built by remember(target) { mutableStateOf("") }
    var used by remember(target) { mutableStateOf(setOf<Int>()) }
    var win by remember { mutableStateOf(0) }
    val tiles = remember(target) { target.second.toList().shuffled() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(target) { vm.speech.speak("ركّبي كلمة ${target.second}", pitch = 1.1f) }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            Text(target.first, fontSize = 100.sp, modifier = Modifier.clickable { vm.speech.speak(target.second) })
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(24.dp)).background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (built.isEmpty()) "؟" else built, fontSize = 56.sp,
                    fontWeight = FontWeight.ExtraBold, color = Ink, textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                tiles.forEachIndexed { idx, ch ->
                    Box(
                        Modifier.size(78.dp).clip(RoundedCornerShape(20.dp))
                            .background(if (idx in used) Color(0xFFE9ECEF) else Sky)
                            .clickable {
                                if (idx !in used && built.length < target.second.length) {
                                    if (ch == target.second[built.length]) {
                                        built += ch
                                        used = used + idx
                                        if (built == target.second) {
                                            vm.addStars(2); win = 2
                                            vm.speech.speak("برافو! ${target.second}", pitch = 1.3f)
                                            scope.launch {
                                                delay(1900); win = 0
                                                var n = target
                                                while (n == target) n = BUILD_WORDS.random()
                                                target = n
                                            }
                                        }
                                    } else vm.speech.speak("حاولي تاني", pitch = 1.3f)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (idx !in used) Text(ch.toString(), fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Ink)
                    }
                }
            }
        }
        Celebrate(win) { win = 0 }
    }
}
